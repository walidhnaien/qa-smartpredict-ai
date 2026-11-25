package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class V344NliTest {
 @Test void validatesCoverageAndPreservesEvidence() throws Exception {
  var m=new ObjectMapper(); var pairs=m.readTree("{\"S1\":{\"sourcePath\":\"/managerMessage\",\"evidence\":\"Approved\",\"referenceId\":\"G_APPROVAL\",\"premise\":\"Approval not documented\"}}");
  var raw=m.readTree("{\"assessments\":[{\"statementId\":\"S1\",\"relation\":\"NOT_ESTABLISHED\"}]}");
  var result=QaNliValidationService.validate(m,raw,pairs);
  assertEquals("Approved",result.path("newFindings").get(0).path("evidence").asText());
  assertFalse(result.path("semanticQualityCertified").asBoolean());
  assertThrows(IllegalStateException.class,()->QaNliValidationService.validate(m,m.readTree("{\"assessments\":[]}"),pairs));
 }
}
