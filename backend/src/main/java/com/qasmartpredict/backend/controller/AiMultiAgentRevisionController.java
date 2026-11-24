package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.service.AiMultiAgentRevisionService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ai/multi-agent/revision")
public class AiMultiAgentRevisionController {
    private final AiMultiAgentRevisionService service;
    public AiMultiAgentRevisionController(AiMultiAgentRevisionService service) {
        this.service = service;
    }
    @PostMapping("/{validationId}")
    public Map<String,Object> revise(@PathVariable UUID validationId) throws Exception {
        return service.revise(validationId);
    }
    @GetMapping("/{validationId}")
    public List<Map<String,Object>> history(@PathVariable UUID validationId) {
        return service.history(validationId);
    }
}
