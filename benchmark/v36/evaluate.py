"""Offline path-level screening; semantic correctness requires separate adjudication."""
import argparse, csv, json, re, hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parent

def read(p): return json.loads(p.read_text(encoding='utf-8'))
def ratio(a,b): return a/b if b else None

def select_cases(cases,limit,case_ids=None):
    if limit<1: raise ValueError('limit must be positive')
    if not case_ids: return cases[:limit]
    ids=[x.strip() for x in case_ids.split(',')]
    if '' in ids or len(set(ids))!=len(ids): raise ValueError('Invalid or duplicate case ID')
    selected=[c for c in cases if c['caseId'] in ids]
    if len(selected)!=len(ids): raise ValueError('Unknown case ID')
    return selected

def evaluate(run,limit,case_ids=None):
    cases=select_cases(read(ROOT/'datasets/candidate-analyses.json'),limit,case_ids)
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
                if p.exists():
                    try: records.append(read(p))
                    except (ValueError,OSError): pass
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
        summary[config]=dict(complete=complete,total=len(cases),TP=tp,FP=fp,FN=fn,TN=tn,precision=ratio(tp,tp+fp),recall=ratio(tp,tp+fn),f1=ratio(2*tp,2*tp+fp+fn),fpr=ratio(fp,fp+tn),target_path_hits=path_hits,extra_path_alerts=extra,target_paths_assessed=covered,component_duration_ms=duration,tokens=tokens,provisional=True,target_localization_recall=ratio(path_hits,positive_complete),strict_nli_eligible=strict_eligible,strict_nli_assessed=strict_assessed,strict_nli_hits=strict_hits,strict_nli_accuracy_end_to_end=ratio(strict_hits,strict_eligible),strict_nli_accuracy_assessed=ratio(strict_hits,strict_assessed),preload_ms=preload if measured_duration else None,inference_ms=inference if measured_duration else None,telemetry_overhead_ms=measured_duration-preload-inference if measured_duration else None,prompt_eval_count=prompt_processed,eval_count=generated,prompt_eval_cached_count=cached if cache_reported else None,cache_reporting_calls=cache_reported,semantic_precision=None,specificity=ratio(tn,tn+fp),incomplete=len(cases)-complete,technical_completion_rate=ratio(complete,len(cases)))
    folder.mkdir(parents=True,exist_ok=True)
    with (folder/'screening.csv').open('w',newline='',encoding='utf-8') as f:
        w=csv.DictWriter(f,fieldnames=['config','caseId','status','expectedAlert','alert','targetPath','pathHit','paths','unmapped']);w.writeheader();w.writerows(rows)
    annotation_file=folder/'semantic-annotations.csv'
    old={}
    if annotation_file.exists():
        with annotation_file.open(newline='',encoding='utf-8') as f:
            old={r.get('annotationId'):r for r in csv.DictReader(f) if r.get('annotationId')}
    for a in annotations:
        checkpoint=read(folder/f"{a['role']}-{a['caseId']}.json")
        identity=[a[k] for k in ('config','caseId','role','sourcePath','finding')]+[checkpoint.get('checkpointSha256')]
        a['annotationId']=hashlib.sha256(json.dumps(identity,ensure_ascii=False).encode()).hexdigest()
    unique={a['annotationId']:a for a in annotations}
    annotations=list(unique.values())
    for a in annotations:
        prior=old.get(a['annotationId'],{})
        a['judgment']=prior.get('judgment',''); a['comment']=prior.get('comment','')
        if a['judgment'] not in ('','TARGET_VALID','OTHER_VALID','FALSE_ALERT'): raise ValueError('Unknown judgment: '+a['judgment'])
    stale=[r for k,r in old.items() if k not in unique]
    if stale:
        import time
        with (folder/f'semantic-annotations-stale-{time.time_ns()}.csv').open('w',newline='',encoding='utf-8') as f:
            w=csv.DictWriter(f,fieldnames=list(stale[0]));w.writeheader();w.writerows(stale)
    with annotation_file.open('w',newline='',encoding='utf-8') as f:
        w=csv.DictWriter(f,fieldnames=['annotationId','config','caseId','role','sourcePath','finding','judgment','comment']);w.writeheader();w.writerows(annotations)
    for config,v in summary.items():
        aa=[a for a in annotations if a['config']==config]
        approved=[a for a in aa if a['judgment']]
        v['annotation_count']=len(aa);v['annotations_reviewed']=len(approved)
        v['annotation_coverage']=ratio(len(approved),len(aa))
        v['semantic_precision']=ratio(sum(a['judgment'] in ('TARGET_VALID','OTHER_VALID') for a in aa),len(aa)) if len(approved)==len(aa) else None
        positives={r['caseId'] for r in rows if r['config']==config and r['status']=='COMPLETE' and r['expectedAlert']}
        hits={a['caseId'] for a in aa if a['judgment']=='TARGET_VALID' and a['caseId'] in positives}
        v['semantic_target_recall']=ratio(len(hits),len(positives)) if len(approved)==len(aa) else None
        v['semantic_target_hits']=len(hits) if len(approved)==len(aa) else None
    (folder/'summary.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
    return summary
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--run',default='smoke');p.add_argument('--limit',type=int,default=3);p.add_argument('--case-ids');a=p.parse_args();print(json.dumps(evaluate(a.run,a.limit,a.case_ids),indent=2))
