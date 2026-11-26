package com.qasmartpredict.backend.service.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.nio.file.*;
import java.util.*;

@Service
public class ResourcePreflightService {
    private final RestClient client;
    private final List<String> models;
    public ResourcePreflightService(@Value("${qa.llm.ollama.base-url:http://ollama:11434}") String url,
        @Value("${qa.llm.ollama.model:llama3.2:3b}") String llama,
        @Value("${qa.llm.gemma.model:gemma3-qa:4b}") String gemma,
        @Value("${qa.llm.qwen.model:qwen2.5:3b}") String qwen) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); factory.setReadTimeout(5000);
        client=RestClient.builder().baseUrl(url).requestFactory(factory).build();
        models=List.of(llama,gemma,qwen);
    }
    static String status(List<String> blockers,List<String> warnings) {
        return !blockers.isEmpty()?"NOT_READY":warnings.isEmpty()?"READY":"WARNING";
    }
    static void memoryWarnings(long availableKb,long swapTotalKb,long swapFreeKb,List<String> warnings) {
        if(availableKb<0) warnings.add("LINUX_MEMORY_UNKNOWN");
        else if(availableKb<2L*1024*1024) warnings.add("LINUX_MEMORY_CRITICAL");
        else if(availableKb<3L*1024*1024) warnings.add("LINUX_MEMORY_LOW");
        if(swapTotalKb>0 && swapFreeKb>=0 && swapFreeKb<=swapTotalKb && 100.0*(swapTotalKb-swapFreeKb)/swapTotalKb>=25) warnings.add("SWAP_USAGE_ELEVATED");
    }
    public Map<String,Object> inspect() {
        var warnings=new ArrayList<String>(); var blockers=new ArrayList<String>();
        var resources=new LinkedHashMap<String,Object>();
        resources.put("scope","BACKEND_CONTAINER_LINUX_VIEW");
        resources.put("windowsAvailableMemoryMb",null);
        resources.put("windowsStatus","UNKNOWN_USE_HOST_SCRIPT");
        try {
            var mem=new HashMap<String,Long>();
            for(String line:Files.readAllLines(Path.of("/proc/meminfo"))) {
                String[] p=line.trim().split("\\s+");
                if(p.length>=2) mem.put(p[0],Long.parseLong(p[1]));
            }
            long available=mem.getOrDefault("MemAvailable:",-1L), total=mem.getOrDefault("SwapTotal:",-1L), free=mem.getOrDefault("SwapFree:",-1L);
            resources.put("linuxAvailableMemoryMb",available<0?null:available/1024);
            resources.put("swapUsagePercent",total>0 && free>=0?100.0*(total-free)/total:null);
            memoryWarnings(available,total,free,warnings);
        } catch(Exception e) { warnings.add("LINUX_MEMORY_UNKNOWN"); }
        try {
            long bytes=Files.getFileStore(Path.of("/app")).getUsableSpace();
            resources.put("backendFilesystemAvailableGb",bytes/1e9);
            if(bytes<30_000_000_000L) warnings.add("BACKEND_FILESYSTEM_LOW");
        } catch(Exception e) { warnings.add("BACKEND_FILESYSTEM_UNKNOWN"); }
        var installed=new TreeSet<String>(); boolean available=false;
        try {
            JsonNode tags=client.get().uri("/api/tags").retrieve().body(JsonNode.class);
            if(tags==null || !tags.path("models").isArray()) throw new IllegalStateException("Invalid tags response");
            for(JsonNode m:tags.path("models")) installed.add(m.path("name").asText());
            available=true;
        } catch(Exception e) { blockers.add("OLLAMA_UNAVAILABLE"); }
        var missing=models.stream().distinct().filter(m->!installed.contains(m)).toList();
        if(available && !missing.isEmpty()) blockers.add("REQUIRED_MODELS_MISSING");
        var ollama=Map.of("available",available,"requiredModelsAvailable",available&&missing.isEmpty(),"requiredModels",models,"missingModels",missing);
        return Map.of("status",status(blockers,warnings),"readyToRun",blockers.isEmpty(),"resources",resources,"ollama",ollama,"warnings",warnings,"blockers",blockers);
    }
    public Map<String,Object> runtime() {
        var result=new LinkedHashMap<String,Object>();
        try {
            JsonNode ps=client.get().uri("/api/ps").retrieve().body(JsonNode.class);
            if(ps==null || !ps.path("models").isArray()) throw new IllegalStateException("Invalid ps response");
            result.put("available",true); result.put("loadedModels",ps.path("models"));
        } catch(Exception e) { result.put("available",false); result.put("error","OLLAMA_RUNTIME_UNAVAILABLE"); }
        return result;
    }
}
