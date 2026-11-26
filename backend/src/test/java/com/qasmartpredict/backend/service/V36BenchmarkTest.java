package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.qasmartpredict.backend.dto.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;

class V36BenchmarkTest {
    final ObjectMapper m = new ObjectMapper();
    @Test void campaign() throws Exception {
        if (!Boolean.getBoolean("benchmark.enabled")) return;
        Path root=Path.of(System.getProperty("benchmark.root", "../benchmark/v36")).toAbsolutePath();
        Path out=root.resolve("results").resolve(System.getProperty("benchmark.run","smoke"));
        Files.createDirectories(out);
        JsonNode cases=m.readTree(root.resolve("datasets/candidate-analyses.json").toFile());
        JsonNode metrics=m.readTree(root.resolve("datasets/official-metrics.json").toFile());
        var catalog=new QaMetricsCatalogService(m);
        var skills=new QaAgentSkillsService(catalog);
        var lifecycle=new OllamaLifecycleService("http://localhost:11434","5m");
        var javaService=new AiEvidenceIntelligenceService(new AiAgentRecommendationService());
        int limit=Integer.getInteger("benchmark.limit",3);
        int errors=0;
        for(String role:System.getProperty("benchmark.roles","JAVA,LLAMA,GEMMA,QWEN").split(",")) {
            if(!List.of("JAVA","LLAMA","GEMMA","QWEN").contains(role)) throw new IllegalArgumentException(role);
            for(int i=0;i<Math.min(limit,cases.size());i++) {
                JsonNode c=cases.get(i), candidate=c.path("candidate"), official=metrics.path(c.path("officialMetricsId").asText());
                String id=c.path("caseId").asText();
                Path file=out.resolve(role+"-"+id+".json");
                if(Files.exists(file)) { if(!m.readTree(file.toFile()).path("status").asText().equals("ERROR")) continue; }
                ObjectNode result=m.createObjectNode().put("caseId",id).put("role",role);
                long start=System.nanoTime();
                try {
                    if(role.equals("JAVA")) {
                        result.set("checks",m.valueToTree(javaService.assess(official,candidate,List.of()).get("objectiveChecks")));
                        AiIntelligenceRequest req=m.readerFor(AiIntelligenceRequest.class).without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(official.toString());
                        var analysis=m.treeToValue(candidate,AiGenerativeAnalysisDto.class);
                        result.set("structural",m.valueToTree(new AiOutputValidatorService().validate(req,analysis,official.path("riskLevel").asText(),official.path("releaseDecision").asText())));
                        result.put("status","SUCCESS");
                    } else {
                        JsonNode registry=QaStatementRegistryService.build(m,candidate);
                        ObjectNode pairs=m.createObjectNode();
                        for(String r:role.equals("LLAMA")?List.of("GEMMA","QWEN"):List.of(role)) {
                            JsonNode selected=QaStatementRegistryService.forRole(m,registry,r);
                            JsonNode p=QaNliValidationService.pairs(m,selected,skills.compactOfficial(official),catalog.catalog(),r);
                            p.fields().forEachRemaining(e->pairs.set(r+"_"+e.getKey(),e.getValue()));
                        }
                        result.set("pairs",pairs);
                        if(pairs.isEmpty()) result.put("status","NO_PAIRS");
                        else {
                            String model=switch(role){case "LLAMA"->"llama3.2:3b";case "GEMMA"->"gemma3-qa:4b";default->"qwen2.5:3b";};
                            String system=role.equals("LLAMA")?"Classify every premise/hypothesis pair as SUPPORTED, CONTRADICTED or NOT_ESTABLISHED. Use only the premise. Return JSON assessments with statementId and relation. Do not infer approval or missing evidence.":skills.nliSystem(role);
                            Map<String,Object> request=new LinkedHashMap<>();
                            request.put("model",model);request.put("stream",false);request.put("keep_alive",lifecycle.keepAlive());
                            request.put("format",QaNliValidationService.schema(pairs));
                            request.put("options",Map.of("temperature",0.1,"num_ctx",4096,"num_predict",256,"repeat_penalty",1.0));
                            request.put("messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",QaNliValidationService.prompt(pairs))));
                            result.set("request",m.valueToTree(request));
                            JsonNode response=lifecycle.chat(request,JsonNode.class);result.set("response",response);
                            if(!response.path("done").asBoolean() || "length".equals(response.path("done_reason").asText())) throw new IllegalStateException("Incomplete response");
                            JsonNode raw=m.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(response.path("message").path("content").asText());
                            result.set("analysis",QaNliValidationService.validate(m,raw,pairs));result.put("status","SUCCESS");
                        }
                    }
                } catch(Exception ex) {result.put("status","ERROR").put("error",ex.toString());errors++;}
                result.put("durationMs",(System.nanoTime()-start)/1_000_000);
                Path tmp=file.resolveSibling(file.getFileName()+".tmp");m.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(),result);
                Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
                System.out.println("BENCHMARK "+role+" "+id+" "+result.path("status").asText());
            }
        }
        if(errors>0) throw new AssertionError(errors+" benchmark errors; checkpoints retained");
    }
}
