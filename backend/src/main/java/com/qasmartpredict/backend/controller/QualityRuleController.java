package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.domain.QualityRuleEntity;
import com.qasmartpredict.backend.repository.QualityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/api/quality-rules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QualityRuleController {

    private final QualityRuleRepository repository;

    @GetMapping
    public List<QualityRuleEntity> getRules() {
        return repository.findAll();
    }


@PutMapping("/{id}/weight")
public QualityRuleEntity updateWeight(
        @PathVariable UUID id,
        @RequestParam Double weight
) {
    QualityRuleEntity rule = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Quality rule not found"));

    rule.setWeight(BigDecimal.valueOf(weight));

    return repository.save(rule);
}







}