package com.qasmartpredict.backend.controller;
import java.util.*;
import com.qasmartpredict.backend.service.orchestrator.QaOrchestratorService;
import com.qasmartpredict.backend.service.orchestrator.ResourcePreflightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/ai/orchestrator")
@CrossOrigin(origins="http://localhost:4200")
public class QaOrchestratorController {
    private final QaOrchestratorService service;
    private final ResourcePreflightService preflight;
    public QaOrchestratorController(QaOrchestratorService service, ResourcePreflightService preflight) { this.service=service; this.preflight=preflight; }
    @GetMapping("/preflight") public Map<String,Object> preflight() { return preflight.inspect(); }
    @GetMapping("/runtime") public Map<String,Object> runtime() { return preflight.runtime(); }
    public record RunRequest(UUID releaseId,String mode) {}
    @PostMapping("/run") public ResponseEntity<Map<String,Object>> start(@RequestBody RunRequest request) {
        return ResponseEntity.accepted().body(service.start(request.releaseId(),request.mode()));
    }
    @GetMapping("/run/{runId}") public Map<String,Object> get(@PathVariable UUID runId) { return service.get(runId); }
    @GetMapping("/run/{runId}/steps") public List<Map<String,Object>> steps(@PathVariable UUID runId) { return service.steps(runId); }
    @GetMapping("/run/{runId}/result") public Map<String,Object> result(@PathVariable UUID runId) throws Exception { return service.result(runId); }
    @GetMapping("/runs") public List<Map<String,Object>> runs() { return service.runs(); }
}
