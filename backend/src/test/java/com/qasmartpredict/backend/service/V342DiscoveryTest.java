package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class V342DiscoveryTest {
 @Test void resolvesAndRejects() throws Exception {
  var m=new ObjectMapper(); var service=new QaEvidenceRegistryService(m,null);
  var statements=QaStatementRegistryService.build(m,m.readTree("{\"managerMessage\":\"Example statement\"}"));
  var raw=m.readTree("{\"findings\":[{\"statementId\":\"S1\",\"reason\":\"Example reason\"},{\"statementId\":\"S99\",\"reason\":\"Unknown\"}]}");
  var result=service.validateDiscovery(raw,statements);
  assertEquals(1,result.path("newFindings").size());
  assertEquals("Example statement",result.path("newFindings").get(0).path("evidence").asText());
  assertEquals(1,result.path("rejectedDiscoveries").size());
  assertEquals("PASS",service.validateDiscovery(m.readTree("{\"findings\":[]}"),statements).path("verdict").asText());
 }
}
