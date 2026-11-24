---
name: qa-smartpredict
description: Global QA SmartPredict agent governance and multi-model skill policy.
---

# QA SmartPredict multi-agent supervisor

See `backend/src/main/resources/qa-agents/global-policy.md` for constraints.

The runtime orchestrator is Java/Spring Boot: Llama generates the analysis; Gemma and Qwen conduct independent reviews; Llama revises while Java validates. This Markdown file is useful for IDE agents and human documentation but is **not** automatically executed by Ollama.

Required skills: metric-interpretation, qa-analysis, independent-review, critique-resolution, quality-validation.

Model outputs are advisory. Official Java metrics and release decisions must never be changed by AI.
