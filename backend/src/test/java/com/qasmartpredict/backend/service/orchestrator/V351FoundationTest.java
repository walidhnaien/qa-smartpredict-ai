package com.qasmartpredict.backend.service.orchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class V351FoundationTest {
    @Test void planHasEightUniqueStepsAndFourModelCalls() {
        assertEquals(8,QaWorkflowPlan.STEPS.size());
        assertEquals(8,QaWorkflowPlan.STEPS.stream().map(QaWorkflowPlan.Step::name).distinct().count());
        assertEquals(4,QaWorkflowPlan.STEPS.stream().filter(s->s.model()!=null).count());
    }
    @Test void rejectsUnknownModeBeforeDatabaseAccess() {
        var db=mock(JdbcTemplate.class);
        var service=new QaOrchestratorService(db,mock(PlatformTransactionManager.class),mock(QaRealWorkflow.class),new com.fasterxml.jackson.databind.ObjectMapper(),mock(ResourcePreflightService.class));
        try { assertEquals(400,assertThrows(ResponseStatusException.class,()->service.start(UUID.randomUUID(),"UNKNOWN")).getStatusCode().value()); verifyNoInteractions(db); }
        finally { service.shutdown(); }
    }
    @Test void requiresReleaseId() {
        var db=mock(JdbcTemplate.class);
        var service=new QaOrchestratorService(db,mock(PlatformTransactionManager.class),mock(QaRealWorkflow.class),new com.fasterxml.jackson.databind.ObjectMapper(),mock(ResourcePreflightService.class));
        try { assertEquals(400,assertThrows(ResponseStatusException.class,()->service.start(null,"SIMULATION")).getStatusCode().value()); verifyNoInteractions(db); }
        finally { service.shutdown(); }
    }
}
