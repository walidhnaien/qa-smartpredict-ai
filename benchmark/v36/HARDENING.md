# V3.6.2.1 — Benchmark hardening

Use a new run name (`smoke-v3621`), not the historical smoke directory. The runner hashes immutable inputs, source files and model digests, records an experiment manifest, checks checkpoint content hashes and identity, and quarantines invalid checkpoints. Atomic move is retained. Run one Maven process at a time; concurrent writers and power-loss durability are not certified. The manifest hashes source files, not compiled bytecode or the Ollama binary; model tags must remain unchanged during execution. Ground truth is not read by Java.

`evaluate.py` preserves case-level screening, adds target localization recall and strict NLI metrics (all target-path relations must equal the expected relation; missing target counts as a miss in end-to-end accuracy). This is path-level screening, not semantic certification. A is not NLI eligible. The annotation CSV lists individual alerts per configuration, including duplicates across configurations; adjudicate and deduplicate before computing semantic precision. The CSV is not overwritten to preserve human work; use a new run for changed results. Semantic precision remains null until adjudication. Telemetry uses captured lifecycle logs; overhead is measured component duration minus logged phases, not pure CPU overhead. Old results lack this telemetry and report null. Cache counts are reported as Ollama provides them, not added to processed counts. Component durations include shared Java baseline in B/C. No production changes.

Commands from repository root:

```
python benchmark/v36/validate_dataset.py
mvn -f backend/pom.xml -Dtest=V36BenchmarkTest#checkpointIntegrity test
mvn -f backend/pom.xml -Dtest=V36BenchmarkTest -Dbenchmark.enabled=true -Dbenchmark.run=smoke-v3621 -Dbenchmark.limit=3 test
python benchmark/v36/evaluate.py --run smoke-v3621 --limit 3
```
