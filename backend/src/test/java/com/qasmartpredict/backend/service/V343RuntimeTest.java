package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class V343RuntimeTest {
 @Test void softLengthAndStableIds() throws Exception {
  var m=new ObjectMapper(); var service=new QaEvidenceRegistryService(m,null);
  var registry=QaStatementRegistryService.build(m,m.readTree("{\"managerMessage\":\"Example\",\"topRisks\":[{\"dimension\":\"D\",\"severity\":\"HIGH\",\"reason\":\"Reason\"}]}"));
  var q=QaStatementRegistryService.forRole(m,registry,"QWEN");
  assertFalse(q.has("S2")); assertTrue(q.has("S4"));
  var raw=m.readTree("{\"findings\":[{\"statementId\":\"S4\",\"reason\":\"one two three four five six seven eight nine ten eleven twelve thirteen\"}]} ");
  var result=service.validateDiscovery(raw,q);
  assertEquals(1,result.path("newFindings").size());
  assertEquals("REASON_WORD_TARGET_EXCEEDED",result.path("newFindings").get(0).path("validationWarnings").get(0).asText());
 }
}
