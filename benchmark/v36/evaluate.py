"""Offline path-level screening; semantic correctness requires separate adjudication."""
import argparse, csv, json
from pathlib import Path
ROOT=Path(__file__).resolve().parent

def read(p): return json.loads(p.read_text(encoding='utf-8'))
def ratio(a,b): return a/b if b else None

def evaluate(run,limit):
    cases=read(ROOT/'datasets/candidate-analyses.json')[:limit]
    truth={x['caseId']:x for x in read(ROOT/'datasets/ground-truth.json')}
    folder=ROOT/'results'/run
    rows=[]; summary={}
    for config,roles in [('A',['JAVA']),('B',['JAVA','LLAMA']),('C',['JAVA','GEMMA','QWEN'])]:
        tp=fp=fn=tn=0; complete=0; path_hits=0; extra=0; duration=0; tokens=0; covered=0
        for c in cases:
            cid=c['caseId']; t=truth[cid]; target=t['targetPath']; positive=t['expectedAlert']
            records=[]
            for role in roles:
                p=folder/f'{role}-{cid}.json'
                if p.exists(): records.append(read(p))
            if len(records)!=len(roles) or any(r['status'] not in ('SUCCESS','NO_PAIRS') for r in records):
                rows.append(dict(config=config,caseId=cid,status='INCOMPLETE'));continue
            complete+=1; paths=set(); unmapped=0; assessed=set()
            for r in records:
                duration+=r['durationMs']; resp=r.get('response',{});tokens+=resp.get('prompt_eval_count',0)+resp.get('eval_count',0)
                for f in r.get('checks',[]):
                    path=f.get('sourcePath') or ('/'+f['field'] if f.get('field') in c['candidate'] else None)
                    if path: paths.add(path)
                    else: unmapped+=1
                if r.get('structural',{}).get('issues'): unmapped+=len(r['structural']['issues'])
                for a in r.get('analysis',{}).get('assessments',[]): assessed.add(a['sourcePath'])
                for f in r.get('analysis',{}).get('newFindings',[]): paths.add(f['sourcePath'])
            alert=bool(paths or unmapped); hit=target in paths
            tp+=positive and alert; fn+=positive and not alert; fp+=not positive and alert; tn+=not positive and not alert
            path_hits+=positive and hit; extra+=len(paths-({target} if positive else set()))+unmapped;covered+=target in assessed
            rows.append(dict(config=config,caseId=cid,status='COMPLETE',expectedAlert=positive,alert=alert,targetPath=target,pathHit=hit,paths=';'.join(sorted(paths)),unmapped=unmapped))
        summary[config]=dict(complete=complete,total=len(cases),TP=tp,FP=fp,FN=fn,TN=tn,precision=ratio(tp,tp+fp),recall=ratio(tp,tp+fn),f1=ratio(2*tp,2*tp+fp+fn),fpr=ratio(fp,fp+tn),target_path_hits=path_hits,extra_path_alerts=extra,target_paths_assessed=covered,component_duration_ms=duration,tokens=tokens,provisional=True)
    folder.mkdir(parents=True,exist_ok=True)
    (folder/'summary.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
    with (folder/'screening.csv').open('w',newline='',encoding='utf-8') as f:
        w=csv.DictWriter(f,fieldnames=['config','caseId','status','expectedAlert','alert','targetPath','pathHit','paths','unmapped']);w.writeheader();w.writerows(rows)
    return summary
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--run',default='smoke');p.add_argument('--limit',type=int,default=3);a=p.parse_args();print(json.dumps(evaluate(a.run,a.limit),indent=2))
