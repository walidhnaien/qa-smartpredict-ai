package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class V346GroundingTest {
 private final ObjectMapper m=new ObjectMapper();
 @Test void preservesArrayLimitations() throws Exception {
  assertEquals("First. Second.",QaNliValidationService.limitations(m.readTree("[\"First.\",\"Second.\"]")));
 }
 @Test void confidenceUsesGovernanceReference() throws Exception {
  var c=m.readTree("{\"confidenceExplanation\":\"Confidence does not justify release approval.\"}");
  var pairs=QaNliValidationService.pairs(m,QaStatementRegistryService.build(m,c),m.createObjectNode().put("confidence",95),m.createObjectNode(),"QWEN");
  assertEquals("M_CONFIDENCE_GOVERNANCE",pairs.path("C1").path("referenceId").asText());
  assertTrue(pairs.path("C1").path("premise").asText().contains("does not justify release approval"));
 }
 @Test void uncertaintyIsNotContradiction() throws Exception {
  var pairs=m.readTree("{\"C1\":{\"evidence\":\"Claim\"}}");
  var raw=m.readTree("{\"assessments\":[{\"statementId\":\"C1\",\"relation\":\"NOT_ESTABLISHED\"}]}");
  assertEquals("EVIDENCE_GAP",QaNliValidationService.validate(m,raw,pairs).path("assessments").get(0).path("findingNature").asText());
 }
}
