# V3.5.3.2 — lot L1/L2 : diagnostic et admission

Patch sur V3.5.3.1. Aucun changement de modèle, prompt, timeout inference (240 s), Docker Compose, base de données ou workflow à huit étapes.

- GET /api/ai/orchestrator/preflight : mémoire Linux visible du backend, swap, filesystem backend, modèles installés via /api/tags. Les mesures Linux ne sont pas une mesure de RAM Windows ni une limite cgroup garantie.
- GET /api/ai/orchestrator/runtime : modèles chargés via /api/ps.
- POST /run REAL : contrôle préalable automatique. HTTP 503 avant insertion si Ollama indisponible ou modèle manquant. SIMULATION inchangée. Avertissements mémoire/disque non bloquants. Le contrôle précède la vérification DB (priorité des erreurs différente en cas de panne Ollama).
- Script Windows local : mesures hôte et diagnostic backend, sans accès privilégié Docker ni transmission des mesures Windows au serveur. Seuils expérimentaux, disque C uniquement. Windows inconnu dans l'API seule.
- Aucun préchargement supplémentaire : les appels de diagnostic ont des délais courts distincts de l'inférence.

Installation Git Bash, racine du projet : sauvegarder les changements locaux, extraire le ZIP, puis `cd backend && mvn clean test`, `cd .. && docker compose up -d --build backend`.

Vérifier `curl -sS http://localhost:8081/api/ai/orchestrator/preflight` et `/runtime`, puis `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/qa-resource-check.ps1`.

4 nouveaux tests JUnit. Compilation non exécutée dans l'environnement de génération (Maven absent). Le retry réel, la reprise durable et les optimisations de chargement restent à tester/développer après mesure. Le diagnostic n'accélère pas lui-même les modèles. Ne pas modifier la mémoire WSL ou le timeout avant validation.
