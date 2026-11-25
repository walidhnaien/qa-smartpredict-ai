package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class V345AtomicClaimTest {
 @Test void separatesCoverageClaimsAndPreservesQuote() throws Exception {
  var m=new ObjectMapper();
  var c=m.readTree("{\"priorityActions\":[{\"action\":\"Enhance Epic Coverage Score by Addressing Uncovered Requirements (1/5) and Ensuring All Stories are Done\"}]}");
  var registry=QaStatementRegistryService.build(m,c);
  var pairs=QaNliValidationService.pairs(m,registry,m.createObjectNode(),m.createObjectNode(),"QWEN");
  assertEquals(2,pairs.size());
  assertEquals("M_COVERAGE_RELATION",pairs.path("C1").path("referenceId").asText());
  assertEquals("M_EPIC_COMPLETION",pairs.path("C2").path("referenceId").asText());
  assertEquals(c.at("/priorityActions/0/action").asText(),pairs.path("C1").path("evidence").asText());
 }
 @Test void doesNotTreatProspectiveImpactAsFact() throws Exception {
  var m=new ObjectMapper(); var c=m.readTree("{\"priorityActions\":[{\"expectedImpact\":\"Bug ratio may improve\"}]}");
  assertTrue(QaNliValidationService.pairs(m,QaStatementRegistryService.build(m,c),m.createObjectNode(),m.createObjectNode(),"QWEN").isEmpty());
 }
}
