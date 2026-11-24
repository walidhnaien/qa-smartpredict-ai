package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class V33SemanticTest {
 @Test void detectsA01AndAcceptsCorrectWording() throws Exception {
  var mapper=new ObjectMapper();
  var recommendations=mock(AiAgentRecommendationService.class);
  var service=new AiEvidenceIntelligenceService(recommendations);
  var official=mapper.readTree("{\"defectScore\":22.22,\"confidence\":95,\"riskLevel\":\"HIGH\"}");
  var bad=mapper.readTree("{\"topRisks\":[{\"reason\":\"Higher than expected defect quality score indicates potential issues with reliability.\"}],\"confidenceExplanation\":\"High confidence in analysis (95%) supports the deterministic release decision.\"}");
  var checks=mapper.valueToTree(service.assess(official,bad,List.of())).path("objectiveChecks");
  assertEquals(2,checks.size());
  assertEquals("DEFECT_SCORE_DIRECTION",checks.get(0).path("type").asText());
  assertEquals("CONFIDENCE_RELEASE_JUSTIFICATION",checks.get(1).path("type").asText());
  var good=mapper.readTree("{\"topRisks\":[{\"reason\":\"Low defect quality score indicates weaker quality.\"}],\"confidenceExplanation\":\"Confidence does not establish release safety.\"}");
  assertEquals(0,mapper.valueToTree(service.assess(official,good,List.of())).path("objectiveChecks").size());
 }
}
