# V3.6.1 — Dataset draft

30 synthetic cases, 25 positive and 5 clean. English candidate text matches production prompts. A01 values are historical reference values from the qualification, not freshly calculated. No backend modification.

Ground truth is proposed by the assistant, not independently approved. Review all labels before freezing. Only candidate and official evidence go to reviewers; never transmit ground-truth.json, designId, targetPath or expectedRelation. The catalog is copied unchanged from the supplied archive. Human approval is required by project governance; no approval evidence is supplied.

Primary unit: case-level binary alert. Secondary: target JSON pointer localization; deduplicate by path and report off-target alerts separately. SUPPORTED for clean recommendations means acceptable wording, not proof of future outcomes. NOT_ESTABLISHED is advisory, not proven false. Report errors separately, never as true negatives.

Baseline must invoke existing Java rules, not new rules written from labels. AiEvidenceIntelligenceService.assess(source,candidate,List.of()) returns objectiveChecks. AiOutputValidatorService checks structure. AiRevisionValidationService is for revised output and must not be silently applied to initial candidates. QaNliValidationService pairs uses heuristic routing and excludes recommendations/hypotheses. Single Llama reviewer is a new experimental adapter, not the existing initial generator. Freeze comparison protocol before running.

Limitations: single release, imbalanced dataset, reused development patterns, no independent holdout yet. No scientific superiority claim.
