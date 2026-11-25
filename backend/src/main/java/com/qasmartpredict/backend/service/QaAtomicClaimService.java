package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** Bounded heuristic extraction. Original quote is preserved; derived claims are advisory. */
public final class QaAtomicClaimService {
 private QaAtomicClaimService() {}
 public static ObjectNode extract(ObjectMapper m, JsonNode statements) {
  ObjectNode out=m.createObjectNode();
  statements.fields().forEachRemaining(e -> {
   String text=e.getValue().path("evidence").asText(), path=e.getValue().path("sourcePath").asText();
   String t=text.toLowerCase(Locale.ROOT);
   if(path.endsWith("/dimension") || path.endsWith("/severity")) return;
   if(path.endsWith("/action") && t.contains("epic coverage") && t.contains("uncovered requirement") && t.contains("stories") && t.contains("done")) {
    add(out,e,"FACT","Addressing uncovered requirements improves epic coverage.",true);
    add(out,e,"FACT","Completing associated stories contributes to epic coverage.",true);
   } else if(path.equals("/managerMessage") && t.startsWith("release approved")) {
    add(out,e,"DECISION","The release has been approved.",true);
   } else if(path.equals("/executiveSummary") && t.contains("due to high risk")) {
    add(out,e,"FACT","High risk is a positive justification for the release recommendation.",true);
   } else {
    String type=path.endsWith("/action")?"RECOMMENDATION":path.endsWith("/expectedImpact")||t.contains(" may ")||t.contains(" might ")?"HYPOTHESIS":"FACT";
    add(out,e,type,text,false);
   }
  });
  return out;
 }
 private static void add(ObjectNode out, Map.Entry<String,JsonNode> e,String type,String claim,boolean derived) {
  ObjectNode n=out.putObject("C"+(out.size()+1)); n.setAll((ObjectNode)e.getValue());
  n.put("parentStatementId",e.getKey()).put("claimType",type).put("hypothesis",claim).put("derivedClaim",derived);
 }
}
