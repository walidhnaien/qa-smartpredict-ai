package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import org.springframework.web.client.RestClientResponseException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class OllamaLifecycleService {
    private final RestClient client;
    private final String keepAlive;
    private final ReentrantLock lock = new ReentrantLock(true);
    public OllamaLifecycleService(@Value("${qa.llm.ollama.base-url:http://ollama:11434}") String url,
                                  @Value("${qa.llm.ollama.keep-alive:5m}") String keepAlive) {
        this.keepAlive = keepAlive;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(240_000);
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    static Map<String,Object> preloadRequest(Map<String,Object> request) {
        Map<String,Object> preload=new HashMap<>();
        preload.put("model",request.get("model"));
        preload.put("options",request.get("options"));
        preload.put("keep_alive",request.get("keep_alive"));
        preload.put("stream",false);
        preload.put("messages",List.of());
        return preload;
    }
    static boolean transientFailure(Throwable error) {
        for(Throwable t=error;t!=null;t=t.getCause()) {
            if(t instanceof SocketTimeoutException || t instanceof ConnectException) return true;
            if(t instanceof RestClientResponseException r) {
                int status=r.getStatusCode().value();
                if(status==408 || status==429 || status==502 || status==503 || status==504) return true;
            }
        }
        return false;
    }
    private static long elapsed(long start) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start); }
    private <T> T post(Map<String,Object> request,Class<T> type) {
        return client.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(type);
    }
    public String keepAlive() { return keepAlive; }
    public <T> T chat(Map<String,Object> request, Class<T> type) {
        long start = System.nanoTime();
        boolean acquired = false;
        try {
            acquired = lock.tryLock(240, TimeUnit.SECONDS);
            if (!acquired) throw new IllegalStateException("Ollama queue timeout");
            long wait = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);
            System.out.println("[OLLAMA_LIFECYCLE] model="+request.get("model")+" queue_ms="+wait+" keep_alive="+request.get("keep_alive"));
            for (int attempt=1; attempt<=2; attempt++) {
                String phase="PRELOAD";
                long phaseStart=System.nanoTime();
                try {
                    JsonNode ready=post(preloadRequest(request),JsonNode.class);
                    if (ready==null || !ready.path("done").asBoolean(false))
                        throw new IllegalStateException("Ollama preload not acknowledged");
                    System.out.println("[OLLAMA_LIFECYCLE] model="+request.get("model")+" phase=PRELOAD attempt="+attempt+" duration_ms="+elapsed(phaseStart)+" load_ns="+ready.path("load_duration"));
                    phase="INFERENCE";
                    phaseStart=System.nanoTime();
                    T result=post(request,type);
                    System.out.println("[OLLAMA_LIFECYCLE] model="+request.get("model")+" phase=INFERENCE attempt="+attempt+" duration_ms="+elapsed(phaseStart));
                    return result;
                } catch (RuntimeException e) {
                    boolean retry=attempt==1 && transientFailure(e);
                    System.err.println("[OLLAMA_LIFECYCLE] model="+request.get("model")+" phase="+phase+" attempt="+attempt+" duration_ms="+elapsed(phaseStart)+" retry="+retry+" error="+e.getClass().getSimpleName());
                    if (!retry) throw e;
                    Thread.sleep(2000);
                }
            }
            throw new IllegalStateException("Ollama attempts exhausted");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama queue interrupted",e);
        } finally { if (acquired) lock.unlock(); }
    }
}
