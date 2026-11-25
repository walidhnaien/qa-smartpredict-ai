package com.qasmartpredict.backend.service.orchestrator;

import com.fasterxml.jackson.databind.*;
import com.qasmartpredict.backend.controller.*;
import com.qasmartpredict.backend.dto.*;
import com.qasmartpredict.backend.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class QaRealWorkflow {
    private final QualityIntelligenceService quality;
    private final QisSonarIntegrationService sonar;
    private final AiIntelligenceService initial;
    private final LocalMultiAgentReviewController reviewers;
    private final AiMultiAgentEvidenceController evidence;
    private final AiMultiAgentRevisionService revision;
    private final JdbcTemplate db;
    private final ObjectMapper mapper;
    public QaRealWorkflow(QualityIntelligenceService quality,QisSonarIntegrationService sonar,AiIntelligenceService initial,LocalMultiAgentReviewController reviewers,AiMultiAgentEvidenceController evidence,AiMultiAgentRevisionService revision,JdbcTemplate db,ObjectMapper mapper) {
        this.quality=quality;this.sonar=sonar;this.initial=initial;this.reviewers=reviewers;this.evidence=evidence;this.revision=revision;this.db=db;this.mapper=mapper;
    }
    static class Context {
        AiIntelligenceRequest metrics;
        UUID validationId, batchId, revisionId;
        Map<String,Object> revised;
    }
    static void require(boolean ok,String message) { if(!ok) throw new IllegalStateException(message); }
    static void verifyBatch(Map<String,Object> result,UUID batch) {
        require(Boolean.TRUE.equals(result.get("sameBatchVerified")) && batch.toString().equals(String.valueOf(result.get("batchId"))),"Review batch mismatch or incomplete");
    }
    Map<String,Object> execute(String step,UUID run,UUID release,Context c) throws Exception {
        switch(step) {
            case "COLLECT_METRICS": {
                var q=quality.calculate(release);
                var s=sonar.calculateFinalQisByRelease(1L,release);
                require(s.getSonarScore()!=null,"REAL mode requires release Sonar data; missing data is not zero");
                require(q.getTotalStories()>0 && q.getTotalRequirements()>0,"REAL mode requires stories and requirements");
                c.metrics=AiIntelligenceRequest.builder().qis(s.getFinalQis()).coverageScore(q.getCoverageScore()).defectScore(q.getDefectScore()).feedbackScore(q.getFeedbackScore()).incidentScore(q.getIncidentScore()).sonarScore(s.getSonarScore()).bugs(s.getBugs()).vulnerabilities(s.getVulnerabilities()).codeSmells(s.getCodeSmells()).sonarCoverage(s.getCoverage()).duplication(s.getDuplicatedLinesDensity()).totalRequirements(q.getTotalRequirements()).coveredRequirements(q.getCoveredRequirements()).uncoveredRequirements(q.getUncoveredRequirements()).totalStories(q.getTotalStories()).totalBugs(q.getTotalBugs()).bugRatio(q.getBugRatio()).build();
                return Map.of("metrics",c.metrics);
            }
            case "INITIAL_ANALYSIS": {
                initial.analyze(c.metrics,id->{c.validationId=id;db.update("UPDATE ai_workflow_run SET validation_id=? WHERE id=?",id,run);});
                require(c.validationId!=null,"Initial analysis not persisted");
                var row=db.queryForMap("SELECT java_status FROM ai_validation_result WHERE id=?",c.validationId);
                require(!"ERROR".equals(row.get("java_status")),"Initial Java validation error");
                return Map.of("validationId",c.validationId,"javaStatus",row.get("java_status"));
            }
            case "EXTRACT_CLAIMS": {
                var raw=db.queryForObject("SELECT llama_response::text FROM ai_validation_result WHERE id=?",String.class,c.validationId);
                var claims=QaAtomicClaimService.extract(mapper,QaStatementRegistryService.build(mapper,mapper.readTree(raw)));
                require(!claims.isEmpty(),"No claims extracted");
                return Map.of("claimCount",claims.size(),"claims",claims);
            }
            case "GOVERNANCE_REVIEW", "METRIC_REVIEW": {
                if(c.batchId==null) { c.batchId=UUID.randomUUID();db.update("UPDATE ai_workflow_run SET batch_id=? WHERE id=?",c.batchId,run); }
                var r=reviewers.reviewRole(c.validationId,c.batchId,step.equals("GOVERNANCE_REVIEW")?"GEMMA":"QWEN");
                require("SUCCESS".equals(r.get("executionStatus")),step+" failed: "+r.get("error"));
                return Map.of("reviewId",r.get("reviewId"),"batchId",c.batchId,"model",r.get("model"),"durationMs",r.get("durationMs"));
            }
            case "CONSOLIDATE": {
                var r=evidence.intelligence(c.validationId);
                verifyBatch(r,c.batchId);require(Boolean.TRUE.equals(r.get("complete")),"Incomplete consolidation");
                return Map.of("batchId",c.batchId,"complete",true);
            }
            case "CONTROLLED_REVISION": {
                c.revised=revision.revise(c.validationId);
                c.revisionId=UUID.fromString(String.valueOf(c.revised.get("revisionId")));
                db.update("UPDATE ai_workflow_run SET revision_id=? WHERE id=?",c.revisionId,run);
                verifyBatch(c.revised,c.batchId);
                require("SUCCESS".equals(c.revised.get("executionStatus")),"Revision failed: "+c.revised.get("error"));
                return Map.of("revisionId",c.revisionId,"batchId",c.batchId);
            }
            case "FINAL_VALIDATION": {
                JsonNode report=mapper.valueToTree(c.revised.get("validationReport"));
                require("PASS_GUARDRAILS".equals(report.path("status").asText()),"Final guardrails require review");
                return Map.of("revisionId",c.revisionId,"validationReport",report,"humanReviewRequired",true,"semanticQualityCertified",false);
            }
            default: throw new IllegalArgumentException("Unknown step "+step);
        }
    }
}
