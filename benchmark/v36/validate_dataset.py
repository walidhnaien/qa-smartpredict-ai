import json, hashlib
from pathlib import Path
from collections import Counter
p = Path(__file__).resolve().parent / "datasets"
def load(name):
    return json.loads((p / name).read_text(encoding="utf-8"))
a = load("candidate-analyses.json")
t = load("ground-truth.json")
m = load("official-metrics.json")
assert len(a) == len(t) == 30
by_id = {x["caseId"]: x for x in a}
assert len(by_id) == 30 and len({x["caseId"] for x in t}) == 30
assert set(by_id) == {x["caseId"] for x in t}
assert len({x["designId"] for x in t}) == 30
assert Counter(x["expectedRelation"] for x in t) == {"CONTRADICTED":14,"NOT_ESTABLISHED":11,"SUPPORTED":3,"NOT_APPLICABLE":2}
for x in t:
    c = by_id[x["caseId"]]
    assert set(c) == {"caseId", "officialMetricsId", "candidate"}
    v = c["candidate"]
    for k in x["targetPath"].split("/")[1:]:
        v = v[int(k)] if isinstance(v,list) else v[k]
    assert isinstance(v,str) and v.strip()
    assert x["annotationStatus"] == "APPROVED"
    assert x["expectedAlert"] is (x["expectedRelation"] in {"CONTRADICTED","NOT_ESTABLISHED"})
    official = m[c["officialMetricsId"]]
    if x["referenceKey"] == "governance":
        assert "Human approval remains required" in x["referenceValue"]
    else:
        assert official[x["referenceKey"]] == x["referenceValue"]
    for k,val in x.get("additionalReferences",{}).items():
        assert official[k] == val
for name, expected in load("immutable-inputs.sha256.json").items():
    assert hashlib.sha256((p/name).read_bytes()).hexdigest() == expected, name
print("30 cases; 25 positive; 5 negative; 28 NLI eligible")
print("DATASET_VALIDATION: PASS")
