package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class V341StatementTest {
 @Test void indexesAndResolvesWithoutModelQuotes() throws Exception {
  var m=new ObjectMapper(); var c=m.readTree("{\"managerMessage\":\"Example statement\",\"priorityActions\":[{\"action\":\"Example action\"}]}");
  var r=QaStatementRegistryService.build(m,c);
  assertEquals(2,r.size());
  var f=m.createObjectNode().put("statementId","S2").put("category","OTHER").put("reason","Example reason");
  assertNull(QaStatementRegistryService.error(f,r));
  var resolved=QaStatementRegistryService.resolve(f,r);
  assertEquals("/priorityActions/0/action",resolved.path("sourcePath").asText());
  assertEquals("Example action",resolved.path("evidence").asText());
  f.put("statementId","S99"); assertEquals("UNKNOWN_STATEMENT",QaStatementRegistryService.error(f,r));
 }
}
