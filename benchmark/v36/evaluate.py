"""Offline path-level screening; semantic correctness requires separate adjudication."""
import argparse, csv, json, re, hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parent

def read(p): return json.loads(p.read_text(encoding='utf-8'))
def ratio(a,b): return a/b if b else None

def evaluate(run,limit):
    cases=read(ROOT/'datasets/candidate-analyses.json')[:limit]
    truth={x['caseId']:x for x in read(ROOT/'datasets/ground-truth.json')}
    folder=ROOT/'results'/run
    rows=[]; summary={}; annotations=[]
    for config,roles in [('A',['JAVA']),('B',['JAVA','LLAMA']),('C',['JAVA','GEMMA','QWEN'])]:
        tp=fp=fn=tn=0; complete=0; path_hits=0; extra=0; duration=0; tokens=0; covered=0
        strict_hits=0; strict_eligible=0; strict_assessed=0; positive_complete=0; preload=0; inference=0; measured_duration=0; cached=0; cache_reported=0; prompt_processed=0; generated=0
        for c in cases:
            cid=c['caseId']; t=truth[cid]; target=t['targetPath']; positive=t['expectedAlert']
            records=[]
            for role in roles:
                p=folder/f'{role}-{cid}.json'
                if p.exists(): records.append(read(p))
            if len(records)!=len(roles) or any(r['status'] not in ('SUCCESS','NO_PAIRS') for r in records):
                rows.append(dict(config=config,caseId=cid,status='INCOMPLETE'));continue
            complete+=1; paths=set(); unmapped=0; assessed=set(); relations=set(); positive_complete+=positive
            eligible=t["expectedRelation"]!="NOT_APPLICABLE" and config!="A"
            strict_eligible+=eligible
            for r in records:
                duration+=r['durationMs']; resp=r.get('response',{});tokens+=resp.get('prompt_eval_count',0)+resp.get('eval_count',0)
                trace=r.get('lifecycleTrace','')
                if trace:
                    preload+=sum(map(int,re.findall(r'phase=PRELOAD .*?duration_ms=(\d+)',trace)))
                    inference+=sum(map(int,re.findall(r'phase=INFERENCE .*?duration_ms=(\d+)',trace)))
                    measured_duration+=r['durationMs']
                prompt_processed+=resp.get('prompt_eval_count',0); generated+=resp.get('eval_count',0)
                if 'prompt_eval_cached_count' in resp: cached+=resp['prompt_eval_cached_count'];cache_reported+=1
                for f in r.get('checks',[]):
                    path=f.get('sourcePath') or ('/'+f['field'] if f.get('field') in c['candidate'] else None)
                    if path: paths.add(path)
                    else: unmapped+=1
                if r.get('structural',{}).get('issues'): unmapped+=len(r['structural']['issues'])
                for a in r.get('analysis',{}).get('assessments',[]):
                    assessed.add(a['sourcePath'])
                    if a['sourcePath']==target: relations.add(a['relation'])
                for f in r.get('analysis',{}).get('newFindings',[]): paths.add(f['sourcePath'])
                for f in list(r.get('checks',[]))+list(r.get('analysis',{}).get('newFindings',[])):
                    annotations.append(dict(config=config,caseId=cid,role=r['role'],sourcePath=f.get('sourcePath',f.get('field','')),finding=json.dumps(f,ensure_ascii=False),judgment='',comment=''))
                for issue in r.get('structural',{}).get('issues',[]):
                    annotations.append(dict(config=config,caseId=cid,role=r['role'],sourcePath='',finding=json.dumps(issue,ensure_ascii=False),judgment='',comment=''))
            strict_assessed+=eligible and bool(relations)
            strict_hits+=eligible and relations=={t['expectedRelation']}
            alert=bool(paths or unmapped); hit=target in paths
            tp+=positive and alert; fn+=positive and not alert; fp+=not positive and alert; tn+=not positive and not alert
            path_hits+=positive and hit; extra+=len(paths-({target} if positive else set()))+unmapped;covered+=target in assessed
            rows.append(dict(config=config,caseId=cid,status='COMPLETE',expectedAlert=positive,alert=alert,targetPath=target,pathHit=hit,paths=';'.join(sorted(paths)),unmapped=unmapped))
        summary[config]=dict(complete=complete,total=len(cases),TP=tp,FP=fp,FN=fn,TN=tn,precision=ratio(tp,tp+fp),recall=ratio(tp,tp+fn),f1=ratio(2*tp,2*tp+fp+fn),fpr=ratio(fp,fp+tn),target_path_hits=path_hits,extra_path_alerts=extra,target_paths_assessed=covered,component_duration_ms=duration,tokens=tokens,provisional=True,target_localization_recall=ratio(path_hits,positive_complete),strict_nli_eligible=strict_eligible,strict_nli_assessed=strict_assessed,strict_nli_hits=strict_hits,strict_nli_accuracy_end_to_end=ratio(strict_hits,strict_eligible),strict_nli_accuracy_assessed=ratio(strict_hits,strict_assessed),preload_ms=preload if measured_duration else None,inference_ms=inference if measured_duration else None,telemetry_overhead_ms=measured_duration-preload-inference if measured_duration else None,prompt_eval_count=prompt_processed,eval_count=generated,prompt_eval_cached_count=cached if cache_reported else None,cache_reporting_calls=cache_reported,semantic_precision=None)
    folder.mkdir(parents=True,exist_ok=True)
    (folder/'summary.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
    with (folder/'screening.csv').open('w',newline='',encoding='utf-8') as f:
        w=csv.DictWriter(f,fieldnames=['config','caseId','status','expectedAlert','alert','targetPath','pathHit','paths','unmapped']);w.writeheader();w.writerows(rows)
    annotation_file=folder/'semantic-annotations.csv'
    if not annotation_file.exists():
        with annotation_file.open('w',newline='',encoding='utf-8') as f:
            w=csv.DictWriter(f,fieldnames=['config','caseId','role','sourcePath','finding','judgment','comment']);w.writeheader();w.writerows(annotations)
    return summary
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--run',default='smoke');p.add_argument('--limit',type=int,default=3);a=p.parse_args();print(json.dumps(evaluate(a.run,a.limit),indent=2))
