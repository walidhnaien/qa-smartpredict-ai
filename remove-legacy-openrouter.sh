#!/usr/bin/env bash
set -euo pipefail
test -f backend/src/main/java/com/qasmartpredict/backend/service/LocalQaAnalysisService.java
rm -f -- backend/src/main/java/com/qasmartpredict/backend/service/OpenAiQaService.java
rm -f -- backend/src/main/java/com/qasmartpredict/backend/service/OpenAiClientService.java
rm -f -- backend/src/main/java/com/qasmartpredict/backend/service/OpenRouterJudgeService.java
rm -f -- backend/src/main/java/com/qasmartpredict/backend/controller/OpenRouterJudgeTestController.java
