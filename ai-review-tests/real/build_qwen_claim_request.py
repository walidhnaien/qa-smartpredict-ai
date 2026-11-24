
import json
from pathlib import Path

base = Path(__file__).resolve().parent

data = json.loads(
    (base / "A01-input.json").read_text(
        encoding="utf-8"
    )
)

system_prompt = """
You are an information extraction assistant.

Your task is NOT to review, correct or evaluate
the candidate.

Extract every explicit claim about the severity
of the main risk DEFECT_QUALITY.

Use these normalization rules:
- low = LOW
- moderate or medium = MEDIUM
- high = HIGH
- critical = CRITICAL

For each claim:
1. Return the JSON location as a JSON Pointer.
2. Copy the exact evidence from the candidate.
3. Return the normalized risk level.
4. Do not invent claims.
5. Do not report sections without an explicit
   claim about risk severity.
6. Include repeated claims from different sections.

Do not extract action priorities, expected impact,
confidence levels or quality scores.

Return only JSON.
"""

schema = {
    "type": "object",
    "properties": {
        "claims": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "location": {
                        "type": "string"
                    },
                    "field": {
                        "type": "string",
                        "enum": ["riskLevel"]
                    },
                    "evidence": {
                        "type": "string"
                    },
                    "value": {
                        "type": "string",
                        "enum": [
                            "LOW",
                            "MEDIUM",
                            "HIGH",
                            "CRITICAL"
                        ]
                    }
                },
                "required": [
                    "location",
                    "field",
                    "evidence",
                    "value"
                ],
                "additionalProperties": False
            }
        }
    },
    "required": ["claims"],
    "additionalProperties": False
}

request = {
    "model": "qwen2.5:3b",
    "stream": False,
    "keep_alive": 0,
    "format": schema,
    "messages": [
        {
            "role": "system",
            "content": system_prompt
        },
        {
            "role": "user",
            "content": json.dumps(
                data["candidate"],
                indent=2
            )
        }
    ],
    "options": {
        "temperature": 0,
        "num_ctx": 4096,
        "num_predict": 1024
    }
}

output = base / "TC-QWEN-REAL-002-request.json"

output.write_text(
    json.dumps(request, indent=2),
    encoding="utf-8"
)

print(f"Request created: {output}")
