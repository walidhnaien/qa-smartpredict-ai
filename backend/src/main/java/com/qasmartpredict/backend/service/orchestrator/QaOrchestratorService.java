package com.qasmartpredict.backend.service.orchestrator;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Foundation only: no LLM calls and no real quality report. */
@Service
public class QaOrchestratorService {
    private static final Logger LOG = LoggerFactory.getLogger(QaOrchestratorService.class);
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final QaRealWorkflow real;
    private final ObjectMapper mapper;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    public QaOrchestratorService(JdbcTemplate db, PlatformTransactionManager tm, QaRealWorkflow real, ObjectMapper mapper) {
        this.db=db; this.tx=new TransactionTemplate(tm); this.real=real; this.mapper=mapper;
    }
    public Map<String,Object> start(UUID releaseId, String mode) {
        if (!List.of("SIMULATION","REAL").contains(mode == null ? "" : mode)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Explicit SIMULATION or REAL mode required");
        if (releaseId==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"releaseId required");
        UUID id=UUID.randomUUID();
        try {
            tx.executeWithoutResult(s -> {
                if (!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM release WHERE id=?)",Boolean.class,releaseId)))
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Release not found");
                db.update("INSERT INTO ai_workflow_run(id,release_id,workflow_version,mode,status) VALUES (?,?,'V3.5.2',?,'PENDING')",id,releaseId,mode);
                int order=0;
                for (var step:QaWorkflowPlan.STEPS) db.update("INSERT INTO ai_workflow_step(id,run_id,step_name,step_order,status,model) VALUES (?,?,?,?,'PENDING',?)",UUID.randomUUID(),id,step.name(),++order,step.model());
            });
        } catch (DuplicateKeyException e) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Another workflow is active",e); }
        try { worker.submit(() -> execute(id,releaseId,mode)); }
        catch (RejectedExecutionException e) { fail(id); throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Worker unavailable",e); }
        return Map.of("runId",id,"status","PENDING","mode",mode,"workflowVersion","V3.5.2","totalSteps",8);
    }
    private void execute(UUID id,UUID releaseId,String mode) {
        var context=new QaRealWorkflow.Context();
        try {
            if(db.update("UPDATE ai_workflow_run SET status='RUNNING',started_at=now() WHERE id=? AND status='PENDING'",id)!=1) return;
            for(var step:QaWorkflowPlan.STEPS) {
                tx.executeWithoutResult(s -> {
                    db.update("UPDATE ai_workflow_run SET current_step=? WHERE id=?",step.name(),id);
                    db.update("UPDATE ai_workflow_step SET status='RUNNING',attempt=1,started_at=now() WHERE run_id=? AND step_name=?",id,step.name());
                });
                // Explicit simulation, not a successful agent execution.
                String status; String reference=null;
                if("SIMULATION".equals(mode)) { Thread.sleep(750); status="SIMULATED"; }
                else { reference=mapper.writeValueAsString(real.execute(step.name(),id,releaseId,context)); status="SUCCESS"; }
                db.update("UPDATE ai_workflow_step SET status=?,result_reference=?::jsonb,completed_at=now(),duration_ms=(extract(epoch FROM (now()-started_at))*1000)::bigint WHERE run_id=? AND step_name=?",status,reference,id,step.name());
            }
            db.update("UPDATE ai_workflow_run SET status='COMPLETED',current_step=NULL,completed_at=now(),duration_ms=(extract(epoch FROM (now()-started_at))*1000)::bigint WHERE id=?",id);
        } catch (Exception e) {
            LOG.error("Workflow {} failed",id,e);
            try { fail(id, e.getMessage()); } catch (Exception persistenceError) { LOG.error("Cannot persist workflow failure {}",id,persistenceError); }
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }
    private void fail(UUID id) { fail(id,"Worker unavailable"); }
    private void fail(UUID id,String error) {
        String message=error==null?"Workflow failed":error.substring(0,Math.min(error.length(),1000));
        tx.executeWithoutResult(s -> {
            db.update("UPDATE ai_workflow_step SET status='FAILED',completed_at=now(),error_message=?,duration_ms=(extract(epoch FROM (now()-started_at))*1000)::bigint WHERE run_id=? AND status='RUNNING'",message,id);
            db.update("UPDATE ai_workflow_step SET status='SKIPPED' WHERE run_id=? AND status='PENDING'",id);
            db.update("UPDATE ai_workflow_run SET status='FAILED',completed_at=now(),error_message=?,duration_ms=(extract(epoch FROM (now()-coalesce(started_at,created_at)))*1000)::bigint WHERE id=? AND status IN ('PENDING','RUNNING')",message,id);
        });
    }
    public Map<String,Object> get(UUID id) {
        var rows=db.queryForList("SELECT * FROM ai_workflow_run WHERE id=?",id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Workflow not found");
        return rows.get(0);
    }
    public List<Map<String,Object>> steps(UUID id) { get(id); return db.queryForList("SELECT * FROM ai_workflow_step WHERE run_id=? ORDER BY step_order",id); }
    public Map<String,Object> result(UUID id) throws Exception {
        var run=get(id);
        if(!"REAL".equals(run.get("mode")) || !"COMPLETED".equals(run.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"No completed real report");
        var row=db.queryForMap("SELECT revised_analysis::text AS analysis,validation_report::text AS report FROM ai_multi_agent_revision WHERE id=? AND validation_id=?",run.get("revision_id"),run.get("validation_id"));
        return Map.of("run",run,"revisedAnalysis",mapper.readTree((String)row.get("analysis")),"validationReport",mapper.readTree((String)row.get("report")),"humanReviewRequired",true,"semanticQualityCertified",false);
    }
    public List<Map<String,Object>> runs() { return db.queryForList("SELECT * FROM ai_workflow_run ORDER BY created_at DESC LIMIT 100"); }
    @PreDestroy public void shutdown() { worker.shutdownNow(); try { worker.awaitTermination(5,TimeUnit.SECONDS); } catch(InterruptedException e) { Thread.currentThread().interrupt(); } }
}
