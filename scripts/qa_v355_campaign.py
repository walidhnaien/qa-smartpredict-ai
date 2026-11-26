import argparse, datetime as dt, json, pathlib, statistics, subprocess, time, urllib.request, uuid

EXPECTED={'qis':68.89,'defectScore':22.22222222222223,'bugRatio':77.77777777777777,'confidence':95,'riskLevel':'HIGH','mainRisk':'DEFECT_QUALITY','releaseDecision':'GO_WITH_RISK'}
STEPS=['COLLECT_METRICS','INITIAL_ANALYSIS','EXTRACT_CLAIMS','GOVERNANCE_REVIEW','METRIC_REVIEW','CONSOLIDATE','CONTROLLED_REVISION','FINAL_VALIDATION']
p=argparse.ArgumentParser(); p.add_argument('--base',default='http://localhost:8081/api/ai/orchestrator'); p.add_argument('--release',default='4ba4b317-5d1d-4424-8242-04b86693732a'); p.add_argument('--count',type=int,default=5); p.add_argument('--poll',type=int,default=10); p.add_argument('--watchdog',type=int,default=7200); a=p.parse_args()
assert a.count>0 and a.poll>0 and a.watchdog>0
uuid.UUID(a.release)
out=pathlib.Path('qualification-v355')/dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ'); out.mkdir(parents=True)
def save(name,obj): (out/name).write_text(json.dumps(obj,indent=2,ensure_ascii=False),encoding='utf-8')
def api(path,body=None):
    req=urllib.request.Request(a.base+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(req,timeout=30) as r: return json.load(r)
def sql(query):
    r=subprocess.run(['docker','compose','exec','-T','postgres','sh','-c','psql -X -qAt -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'],input=query,text=True,capture_output=True,check=True)
    return json.loads(r.stdout)
def active(): return sql("SELECT count(*) FROM ai_workflow_run WHERE status IN ('PENDING','RUNNING');")
def ref(s):
    r=s['result_reference']; return json.loads(r['value']) if 'value' in r else r
def uid(x): return str(uuid.UUID(str(x)))
rows=[]; failure=None
try:
    if active()!=0: raise RuntimeError('Existing active workflow; no POST sent')
    for i in range(1,a.count+1):
        n=f'N{i}'; pre=api('/preflight'); save(n+'-preflight.json',pre); save(n+'-runtime-before.json',api('/runtime'))
        if not pre['readyToRun']: raise RuntimeError('Preflight blocked: '+str(pre))
        started=dt.datetime.now(dt.timezone.utc).isoformat(); clock=time.monotonic()
        print(n,'POST REAL',flush=True)
        run=api('/run',{'releaseId':a.release,'mode':'REAL'}); save(n+'-accepted.json',run); rid=uid(run['id']); print(n,rid,flush=True)
        previous=None
        while True:
            run=api('/run/'+rid); save(n+'-run.json',run)
            state=(run['status'],run['current_step'])
            if state!=previous: print(n,state,flush=True); previous=state
            if run['status'] in ('COMPLETED','FAILED'): break
            if time.monotonic()-clock>a.watchdog: raise RuntimeError('Observation watchdog exceeded; workflow NOT cancelled: '+rid)
            time.sleep(a.poll)
        steps=api('/run/'+rid+'/steps'); save(n+'-steps.json',steps)
        if run['status']!='COMPLETED': raise RuntimeError(n+' FAILED: '+str(run.get('error_message')))
        result=api('/run/'+rid+'/result'); save(n+'-result.json',result)
        vid=uid(run['validation_id']); bid=uid(run['batch_id']); rev=uid(run['revision_id'])
        db=sql(f"""SELECT json_build_object('initial',(SELECT count(*) FROM ai_validation_result WHERE id='{vid}'),'reviews',(SELECT count(*) FROM ai_review_result WHERE validation_id='{vid}'),'revision',(SELECT count(*) FROM ai_multi_agent_revision WHERE validation_id='{vid}'),'review_ids',(SELECT json_agg(id) FROM ai_review_result WHERE validation_id='{vid}'),'revision_match',(SELECT count(*) FROM ai_multi_agent_revision WHERE id='{rev}' AND validation_id='{vid}' AND execution_status='SUCCESS' AND input_snapshot->>'batchId'='{bid}'));"""); save(n+'-db.json',db)
        metrics={k:result['revisedAnalysis'].get(k)==v for k,v in EXPECTED.items()}
        s={x['step_name']:x for x in steps}; g=ref(s['GOVERNANCE_REVIEW']); q=ref(s['METRIC_REVIEW']); c=ref(s['CONSOLIDATE']); r=ref(s['CONTROLLED_REVISION'])
        checks={'completed':run['status']=='COMPLETED','steps':len(steps)==8 and [x['step_name'] for x in steps]==STEPS and all(x['status']=='SUCCESS' for x in steps),'metrics':all(metrics.values()),'guardrails':result['validationReport']['status']=='PASS_GUARDRAILS' and result['validationReport']['issues']==[],'human_review':result['humanReviewRequired'] is True and result['semanticQualityCertified'] is False,'no_recovery':run['recovery_count']==0,'no_active':active()==0,'artifact_counts':(db['initial'],db['reviews'],db['revision'])==(1,2,1),'ids':ref(s['INITIAL_ANALYSIS'])['validationId']==vid and all(x['batchId']==bid for x in (g,q,c,r)) and c['complete'] is True and r['revisionId']==rev and set(db['review_ids'])=={g['reviewId'],q['reviewId']} and db['revision_match']==1 and all(run[k]==result['run'][k] for k in ('id','validation_id','batch_id','revision_id'))}
        row={'case':n,'run_id':rid,'started_utc':started,'duration_ms':run['duration_ms'],'steps_success':sum(x['status']=='SUCCESS' for x in steps),'metrics_preserved':sum(metrics.values()),'metrics':metrics,'checks':checks,'step_durations_ms':{x['step_name']:x['duration_ms'] for x in steps}}
        rows.append(row); save(n+'-checks.json',row); print(n,checks,flush=True)
        for service in ('backend','ollama'):
            with (out/f'{n}-{service}.log').open('w',encoding='utf-8') as f:
                subprocess.run(['docker','compose','logs','--since',started,service],stdout=f,stderr=subprocess.STDOUT,check=True)
        if not all(checks.values()): raise RuntimeError(n+' acceptance check failed')
except (Exception,KeyboardInterrupt) as e:
    failure=f'{type(e).__name__}: {e}'; print(failure,flush=True)
finally:
    durations=[r['duration_ms'] for r in rows]; report={'target':a.count,'evaluated':len(rows),'passed':sum(all(r['checks'].values()) for r in rows),'steps_success':sum(r['steps_success'] for r in rows),'metrics_preserved':sum(r['metrics_preserved'] for r in rows),'failure':failure,'duration_ms':{'mean':statistics.mean(durations),'median':statistics.median(durations),'min':min(durations),'max':max(durations)} if durations else None,'runs':rows,'manual_review_required':['Confirm no manual intervention','Confirm configured Ollama read timeout 240000 ms','Review preload/inference logs and retry counts']}; save('summary.json',report); print(json.dumps(report,indent=2)); print('Evidence:',out)
raise SystemExit(0 if failure is None and len(rows)==a.count else 1)
