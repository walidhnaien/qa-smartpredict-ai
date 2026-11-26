package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class V3532RetryTest {

    @Test
    void shouldRecoverAfterTransient503() throws Exception {

        AtomicInteger calls = new AtomicInteger();

        HttpServer server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext("/api/chat", exchange -> {

            int attempt = calls.incrementAndGet();

            exchange.getRequestBody().readAllBytes();

            int status = attempt == 2 ? 503 : 200;

            String response = status == 503
                ? "{\"error\":\"Temporary injected failure\"}"
                : "{\"done\":true,\"load_duration\":1000}";

            byte[] bytes = response.getBytes(
                StandardCharsets.UTF_8
            );

            exchange.getResponseHeaders().set(
                "Content-Type", "application/json"
            );

            exchange.sendResponseHeaders(status, bytes.length);

            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });

        server.start();

        try {
            var service = new OllamaLifecycleService(
                "http://127.0.0.1:" +
                server.getAddress().getPort(),
                "5m"
            );

            Map<String,Object> request = Map.of(
                "model", "llama3.2:3b",
                "options", Map.of("num_ctx", 4096),
                "keep_alive", "5m",
                "stream", false,
                "messages", List.of(
                    Map.of("role", "user", "content", "Test")
                )
            );

            JsonNode result = service.chat(
                request, JsonNode.class
            );

            assertNotNull(result);
            assertTrue(result.path("done").asBoolean());
            assertEquals(4, calls.get());

        } finally {
            server.stop(0);
        }
    }
}
