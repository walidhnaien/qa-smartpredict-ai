
import json
from pathlib import Path

base = Path(__file__).resolve().parent

for test_id in ["001", "002"]:

    source = base / (
        f"TC-QWEN-REAL-{test_id}-request.json"
    )

    request = json.loads(
        source.read_text(encoding="utf-8")
    )

    request["model"] = "gemma3:4b"

    destination = base / (
        f"TC-GEMMA-REAL-{test_id}-request.json"
    )

    destination.write_text(
        json.dumps(request, indent=2),
        encoding="utf-8"
    )

    print(f"Request created: {destination}")
