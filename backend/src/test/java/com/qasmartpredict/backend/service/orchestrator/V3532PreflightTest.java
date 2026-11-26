package com.qasmartpredict.backend.service.orchestrator;
import org.junit.jupiter.api.Test;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class V3532PreflightTest {
 @Test void statusClassification() {
  assertEquals("READY",ResourcePreflightService.status(List.of(),List.of()));
  assertEquals("WARNING",ResourcePreflightService.status(List.of(),List.of("LOW")));
  assertEquals("NOT_READY",ResourcePreflightService.status(List.of("MISSING"),List.of("LOW")));
 }
 @Test void lowMemoryIsWarning() {
  var warnings=new ArrayList<String>();
  ResourcePreflightService.memoryWarnings(1024*1024,2048,1024,warnings);
  assertEquals(List.of("LINUX_MEMORY_CRITICAL","SWAP_USAGE_ELEVATED"),warnings);
 }
 @Test void healthyMemory() {
  var warnings=new ArrayList<String>();
  ResourcePreflightService.memoryWarnings(4L*1024*1024,0,0,warnings);
  assertTrue(warnings.isEmpty());
 }
 @Test void blockedRealRunDoesNotCreateDatabaseRecord() {
  var db=mock(JdbcTemplate.class); var preflight=mock(ResourcePreflightService.class);
  when(preflight.inspect()).thenReturn(Map.of("readyToRun",false,"blockers",List.of("OLLAMA_UNAVAILABLE")));
  var service=new QaOrchestratorService(db,mock(PlatformTransactionManager.class),mock(QaRealWorkflow.class),new ObjectMapper(),preflight);
  try { assertEquals(503,assertThrows(ResponseStatusException.class,()->service.start(UUID.randomUUID(),"REAL")).getStatusCode().value()); verifyNoInteractions(db); }
  finally {service.shutdown();}
 }
}
