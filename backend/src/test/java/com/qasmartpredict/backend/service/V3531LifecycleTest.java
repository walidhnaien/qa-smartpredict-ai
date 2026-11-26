package com.qasmartpredict.backend.service;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.List;
import java.net.SocketTimeoutException;
import static org.junit.jupiter.api.Assertions.*;
class V3531LifecycleTest {
 @Test void preloadPreservesOptions() {
   var options=Map.of("num_ctx",4096,"num_predict",256);
   var request=Map.<String,Object>of("model","llama3.2:3b","options",options,"keep_alive","5m","messages",List.of("prompt"),"format","json");
   var preload=OllamaLifecycleService.preloadRequest(request);
   assertEquals(options,preload.get("options"));
   assertEquals("llama3.2:3b",preload.get("model"));
   assertEquals("5m",preload.get("keep_alive"));
   assertEquals(List.of(),preload.get("messages"));
   assertFalse(preload.containsKey("format"));
 }
 @Test void nestedTimeoutIsRetryable() { assertTrue(OllamaLifecycleService.transientFailure(new IllegalStateException(new SocketTimeoutException()))); }
 @Test void semanticFailureIsNotRetryable() { assertFalse(OllamaLifecycleService.transientFailure(new IllegalArgumentException("Invalid JSON"))); }
}
