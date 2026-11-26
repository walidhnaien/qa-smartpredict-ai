# V3.6.1.1 — Ground truth refinement

30 synthetic cases, 25 positive and 5 clean. English candidate text matches production prompts. A01 values are historical reference values from the qualification, not freshly calculated. No backend modification.

Annotations refined before benchmark execution; user approved the proposed corrections. No independent second annotator or inter-rater agreement measurement. Only candidate and official evidence go to reviewers; never transmit ground-truth.json, designId, targetPath or expectedRelation. The catalog is copied unchanged from the supplied archive. Human approval is required by project governance; no approval evidence is supplied.

Primary unit: case-level binary alert. Secondary: target JSON pointer localization; deduplicate by path and report off-target alerts separately. NOT_APPLICABLE marks clean hypotheses and recommendations; exclude these two targets from three-class NLI scoring, but retain them in binary scoring. NOT_ESTABLISHED is advisory, not proven false. Report errors separately, never as true negatives.

Baseline must invoke existing Java rules, not new rules written from labels. AiEvidenceIntelligenceService.assess(source,candidate,List.of()) returns objectiveChecks. AiOutputValidatorService checks structure. AiRevisionValidationService is for revised output and must not be silently applied to initial candidates. QaNliValidationService pairs uses heuristic routing and excludes recommendations/hypotheses. Single Llama reviewer is a new experimental adapter, not the existing initial generator. Freeze comparison protocol before running.

Limitations: single release, imbalanced dataset, reused development patterns, no independent holdout yet. No scientific superiority claim.

Refinement: RC04 relation changed; RG03 policy evidence; TR02 additional Sonar reference; CL04/CL05 NOT_APPLICABLE. RC05 rationale clarified. Candidate and metric files unchanged. Binary evaluation: 30 cases; NLI: 28 eligible targets. Missing NLI predictions must be reported as abstentions, not silently removed. Run `python benchmark/v36/validate_dataset.py` before benchmarking.
