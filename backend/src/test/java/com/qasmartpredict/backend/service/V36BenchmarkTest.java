package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.qasmartpredict.backend.dto.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

class V36BenchmarkTest {
    final ObjectMapper m = new ObjectMapper();
    String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    String sha(Path p) throws Exception { return sha(Files.readAllBytes(p)); }
    ObjectNode modelDigests() throws Exception {
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var response=client.send(HttpRequest.newBuilder(URI.create("http://localhost:11434/api/tags")).timeout(Duration.ofSeconds(240)).GET().build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200) throw new IOException("Ollama tags HTTP "+response.statusCode());
        ObjectNode result=m.createObjectNode();
        for(JsonNode model:m.readTree(response.body()).path("models")) {
            String name=model.path("name").asText();
            if(List.of("llama3.2:3b","gemma3-qa:4b","qwen2.5:3b").contains(name)) result.put(name,model.path("digest").asText());
        }
        if(result.size()!=3) throw new IOException("Required models missing");
        ObjectNode ordered=m.createObjectNode();
        for(String name:List.of("llama3.2:3b","gemma3-qa:4b","qwen2.5:3b")) {
            String digest=result.path(name).asText();
            if(!digest.matches("[0-9a-f]{64}")) throw new IOException("Invalid digest: "+name);
            ordered.put(name,digest);
        }
        return ordered;
    }
    ObjectNode manifest(Path root) throws Exception {
        ObjectNode result=m.createObjectNode().put("version","V3.6.2.2").put("timeoutMs",240000).put("keepAlive","5m");
        ObjectNode inputs=result.putObject("inputs");
        JsonNode expected=m.readTree(root.resolve("datasets/immutable-inputs.sha256.json").toFile());
        for(String name:List.of("candidate-analyses.json","official-metrics.json","qa-metrics-catalog.json")) {
            String hash=sha(root.resolve("datasets").resolve(name));
            if(!hash.equals(expected.path(name).asText())) throw new IOException("Dataset hash mismatch: "+name);
            inputs.put(name,hash);
        }
        Path backend=root.resolve("../../backend").normalize();
        ObjectNode sources=result.putObject("sources");
        for(String dir:List.of("src/main/java","src/main/resources")) {
            try(var files=Files.walk(backend.resolve(dir))) {
                for(Path f:files.filter(Files::isRegularFile).sorted().toList()) sources.put(backend.relativize(f).toString().replace('\\','/'),sha(f));
            }
        }
        for(String name:List.of("pom.xml","src/test/java/com/qasmartpredict/backend/service/V36BenchmarkTest.java")) sources.put(name,sha(backend.resolve(name)));
        result.set("models",modelDigests());
        return result;
    }
    boolean reusable(Path file,String id,String role,String fingerprint) {
        try {
            ObjectNode r=(ObjectNode)m.readTree(file.toFile());
            String hash=r.remove("checkpointSha256").asText();
            return hash.equals(sha(m.writeValueAsBytes(r))) && id.equals(r.path("caseId").asText()) && role.equals(r.path("role").asText()) && fingerprint.equals(r.path("experimentSha256").asText()) && List.of("SUCCESS","NO_PAIRS").contains(r.path("status").asText());
        } catch(Exception e) { return false; }
    }
    void atomic(Path file,JsonNode result) throws Exception {
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        m.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(),result);
        Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    @Test void checkpointIntegrity(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Path file=dir.resolve("JAVA-test.json");
        ObjectNode r=m.createObjectNode().put("caseId","test").put("role","JAVA").put("experimentSha256","experiment").put("status","SUCCESS");
        r.put("checkpointSha256",sha(m.writeValueAsBytes(r))); atomic(file,r);
        org.junit.jupiter.api.Assertions.assertTrue(reusable(file,"test","JAVA","experiment"));
        org.junit.jupiter.api.Assertions.assertFalse(reusable(file,"test","JAVA","different"));
        r.put("status","NO_PAIRS"); atomic(file,r);
        org.junit.jupiter.api.Assertions.assertFalse(reusable(file,"test","JAVA","experiment"));
        Files.writeString(file,"{");
        org.junit.jupiter.api.Assertions.assertFalse(reusable(file,"test","JAVA","experiment"));
    }
    @Test void campaign() throws Exception {
        if (!Boolean.getBoolean("benchmark.enabled")) return;
        Path root=Path.of(System.getProperty("benchmark.root", "../benchmark/v36")).toAbsolutePath();
        Path out=root.resolve("results").resolve(System.getProperty("benchmark.run","smoke"));
        Files.createDirectories(out);
        ObjectNode manifest=manifest(root);
        String fingerprint=sha(m.writeValueAsBytes(manifest));
        Path manifestFile=out.resolve("experiment.json");
        if(Files.exists(manifestFile) && !manifest.equals(m.readTree(manifestFile.toFile()))) throw new IllegalStateException("Experiment changed; choose a new run name");
        if(!Files.exists(manifestFile)) atomic(manifestFile,manifest);
        JsonNode cases=m.readTree(root.resolve("datasets/candidate-analyses.json").toFile());
        JsonNode metrics=m.readTree(root.resolve("datasets/official-metrics.json").toFile());
        var catalog=new QaMetricsCatalogService(m);
        var skills=new QaAgentSkillsService(catalog);
        var lifecycle=new OllamaLifecycleService("http://localhost:11434","5m");
        var javaService=new AiEvidenceIntelligenceService(new AiAgentRecommendationService());
        int limit=Integer.getInteger("benchmark.limit",3);
        if(limit<1) throw new IllegalArgumentException("benchmark.limit must be positive");
        String ids=System.getProperty("benchmark.caseIds", "").trim();
        Set<String> requested=new LinkedHashSet<>();
        if(!ids.isEmpty()) for(String id:ids.split(",",-1)) {
            if(id.isBlank() || !requested.add(id.trim())) throw new IllegalArgumentException("Invalid or duplicate case ID");
        }
        List<JsonNode> selectedCases=new ArrayList<>();
        for(JsonNode c:cases) if(requested.isEmpty() || requested.contains(c.path("caseId").asText())) selectedCases.add(c);
        if(!requested.isEmpty() && selectedCases.size()!=requested.size()) throw new IllegalArgumentException("Unknown case ID");
        if(requested.isEmpty()) selectedCases=selectedCases.subList(0,Math.min(limit,selectedCases.size()));
        int errors=0;
        for(String role:System.getProperty("benchmark.roles","JAVA,LLAMA,GEMMA,QWEN").split(",")) {
            if(!List.of("JAVA","LLAMA","GEMMA","QWEN").contains(role)) throw new IllegalArgumentException(role);
            for(JsonNode selectedCase:selectedCases) {
                JsonNode c=selectedCase, candidate=c.path("candidate"), official=metrics.path(c.path("officialMetricsId").asText());
                String id=c.path("caseId").asText();
                Path file=out.resolve(role+"-"+id+".json");
                if(Files.exists(file)) {
                    if(reusable(file,id,role,fingerprint)) { System.out.println("BENCHMARK SKIP "+role+" "+id); continue; }
                    Files.move(file,file.resolveSibling(file.getFileName()+".invalid-"+UUID.randomUUID()));
                }
                ObjectNode result=m.createObjectNode().put("caseId",id).put("role",role).put("experimentSha256",fingerprint);
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
                            result.put("modelDigest",manifest.path("models").path(model).asText());
                            ByteArrayOutputStream capture=new ByteArrayOutputStream();
                            PrintStream oldOut=System.out, oldErr=System.err;
                            JsonNode response;
                            try(PrintStream log=new PrintStream(capture,true,StandardCharsets.UTF_8)) {
                                System.setOut(log); System.setErr(log);
                                response=lifecycle.chat(request,JsonNode.class);
                            } finally {
                                System.setOut(oldOut); System.setErr(oldErr);
                                String trace=capture.toString(StandardCharsets.UTF_8);
                                result.put("lifecycleTrace",trace); oldOut.print(trace);
                            }
                            result.set("response",response);
                            if(!response.path("done").asBoolean() || "length".equals(response.path("done_reason").asText())) throw new IllegalStateException("Incomplete response");
                            JsonNode raw=m.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(response.path("message").path("content").asText());
                            result.set("analysis",QaNliValidationService.validate(m,raw,pairs));result.put("status","SUCCESS");
                        }
                    }
                } catch(Exception ex) {result.put("status","ERROR").put("error",ex.toString());errors++;}
                result.put("durationMs",(System.nanoTime()-start)/1_000_000);
                result.put("checkpointSha256",sha(m.writeValueAsBytes(result)));
                atomic(file,result);
                System.out.println("BENCHMARK "+role+" "+id+" "+result.path("status").asText());
            }
        }
        if(errors>0) throw new AssertionError(errors+" benchmark errors; checkpoints retained");
    }
}
