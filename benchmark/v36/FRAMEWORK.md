# V3.6.2 — experimental adapter (pilot)
Run from repository root, with no production workflow running. Validate dataset first. Use a fresh run name if inputs, code or models change. Do not run two campaigns concurrently. The adapter never reads ground-truth.json; evaluate.py reads it offline. Java test is opt-in. Checkpoints are atomic per role/case, errors are retried on next invocation, successful results and NO_PAIRS are reused. No database writes. No production code changed.

    python benchmark/v36/validate_dataset.py
    mvn -f backend/pom.xml test-compile
    mvn -f backend/pom.xml -Dtest=V36BenchmarkTest -Dbenchmark.enabled=true -Dbenchmark.run=smoke -Dbenchmark.limit=3 test
    python benchmark/v36/evaluate.py --run smoke --limit 3

Default first three cases in fixed dataset order; not representative. Llama receives union of role-specific pairs (role-prefixed IDs preserve distinct premises), with experimental generalist prompt; specialized reviewers use existing skills. Same 256 output budget may constrain Llama more; check truncation and token counts. NO_PAIRS means no review, not a supported judgment. Case-level metrics are screening only; path hits do not prove semantic correctness. Unmapped Java checks retained, not guessed. Final finding-level precision/recall and NLI accuracy require independent adjudication of claims, including false alerts and conflicting assessments. Incomplete cases excluded and reported; compare common complete cases before scientific conclusions. Component times are not production workflow latency; Java reused in B/C. No automatic full campaign until smoke reviewed. No Maven or Ollama available in authoring environment; compile/runtime unverified.
