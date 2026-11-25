package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class V34DiscoveryTest {
 @Test void checksLiteralEvidence() throws Exception {
  var m=new ObjectMapper(); var c=m.readTree("{\"managerMessage\":\"Release approved despite risk\"}");
  var f=m.createObjectNode().put("category","UNSUPPORTED_CLAIM").put("sourcePath","/managerMessage").put("evidence","Release approved").put("reason","No approval evidence");
  assertNull(AiDiscoveryValidationService.error(f,c));
  f.put("evidence","Invented quote"); assertEquals("QUOTE_NOT_FOUND",AiDiscoveryValidationService.error(f,c));
  f.put("sourcePath","/unknown"); assertEquals("INVALID_PATH",AiDiscoveryValidationService.error(f,c));
 }
}
