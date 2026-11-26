package com.qasmartpredict.backend.service.orchestrator;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
public final class WorkflowArtifactId {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();
    private WorkflowArtifactId() {}
    static void bind(UUID run,String step) { CURRENT.set(UUID.nameUUIDFromBytes((run+":"+step).getBytes(StandardCharsets.UTF_8))); }
    static void clear() { CURRENT.remove(); }
    public static UUID next() { UUID id=CURRENT.get(); return id==null?UUID.randomUUID():id; }
}
