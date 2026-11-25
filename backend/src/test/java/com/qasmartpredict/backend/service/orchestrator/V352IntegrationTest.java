package com.qasmartpredict.backend.service.orchestrator;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class V352IntegrationTest {
 @Test void acceptsExactBatch() { UUID id=UUID.randomUUID(); assertDoesNotThrow(()->QaRealWorkflow.verifyBatch(Map.of("sameBatchVerified",true,"batchId",id.toString()),id)); }
 @Test void rejectsDifferentBatch() { assertThrows(IllegalStateException.class,()->QaRealWorkflow.verifyBatch(Map.of("sameBatchVerified",true,"batchId",UUID.randomUUID()),UUID.randomUUID())); }
 @Test void rejectsIncompleteBatch() { UUID id=UUID.randomUUID(); assertThrows(IllegalStateException.class,()->QaRealWorkflow.verifyBatch(Map.of("sameBatchVerified",false,"batchId",id),id)); }
}
