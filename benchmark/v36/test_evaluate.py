import unittest, tempfile, json, csv
from pathlib import Path
import evaluate as e
class TestEvaluator(unittest.TestCase):
    def test_selection(self):
        cases=[{'caseId':x} for x in 'abc']
        self.assertEqual(e.select_cases(cases,1,'c,a'),[cases[0],cases[2]])
        for ids in ('a,a','z','a,'):
            with self.assertRaises(ValueError): e.select_cases(cases,1,ids)
    def test_metrics(self):
        old=e.ROOT
        with tempfile.TemporaryDirectory() as d:
            try:
                e.ROOT=Path(d); ds=e.ROOT/'datasets';ds.mkdir();out=e.ROOT/'results'/'test';out.mkdir(parents=True)
                ids=['p','n','f','missing']; cases=[dict(caseId=i,candidate={'x':'text'}) for i in ids]
                (ds/'candidate-analyses.json').write_text(json.dumps(cases));(ds/'ground-truth.json').write_text(json.dumps([dict(caseId=i,targetPath='/x',expectedAlert=i in ('p','missing'),expectedRelation='CONTRADICTED' if i in ('p','missing') else 'SUPPORTED') for i in ids]))
                for i in ids[:3]:
                    (out/f'JAVA-{i}.json').write_text(json.dumps(dict(role='JAVA',status='SUCCESS',durationMs=1,checkpointSha256=i,checks=[{'sourcePath':'/x'}] if i!='n' else [])))
                a=e.evaluate('test',4)['A'];self.assertEqual((a['TP'],a['FP'],a['TN'],a['FN'],a['incomplete']),(1,1,1,0,1));self.assertEqual(a['specificity'],.5);self.assertIsNone(a['semantic_precision'])
                p=out/'semantic-annotations.csv'
                with p.open() as f: rows=list(csv.DictReader(f))
                for r in rows:r['judgment']='TARGET_VALID' if r['caseId']=='p' else 'FALSE_ALERT'
                with p.open('w',newline='') as f:w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
                self.assertEqual(e.evaluate('test',4)['A']['semantic_precision'],.5)
                p=out/'JAVA-p.json';r=json.loads(p.read_text());r['checkpointSha256']='changed';p.write_text(json.dumps(r));self.assertIsNone(e.evaluate('test',4)['A']['semantic_precision'])
            finally:e.ROOT=old
if __name__=='__main__':unittest.main()
