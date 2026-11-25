package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class V3461TraceabilityTest {
 @Test void changedTextDoesNotCertifyResolution() throws Exception {
  var m=new ObjectMapper();
  var a=m.readTree("[{\"model\":\"gemma\",\"assessment\":{\"relation\":\"CONTRADICTED\",\"sourcePath\":\"/managerMessage\"}}]");
  var r=QaSemanticTraceability.build(m,a,m.readTree("{\"managerMessage\":\"approved\"}"),m.readTree("{\"managerMessage\":\"review required\"}"));
  assertEquals(1,r.path("changedAlertedFields").asInt());
  assertTrue(r.path("semanticIssueResolutionRate").isNull());
 }
}
