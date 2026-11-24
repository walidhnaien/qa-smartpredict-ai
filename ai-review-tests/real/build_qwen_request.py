
import json
from pathlib import Path

base = Path(__file__).resolve().parent

data = json.loads(
    (base / "A01-input.json").read_text(
        encoding="utf-8"
    )
)

system_prompt = """
You are an independent software QA technical reviewer.

SOURCE_OF_TRUTH contains official immutable metrics.
CANDIDATE contains an AI-generated QA analysis.

Review the candidate against the source of truth.

Rules:
1. Never modify official metrics.
2. Higher quality scores mean better quality.
3. Do not confuse a quality score with a bug ratio.
4. Do not confuse confidence with release safety.
5. Report factual contradictions and incorrect values.
6. Report each occurrence separately.
7. Every issue must include its exact JSON location.
8. Evidence must be an exact substring of the
   candidate text at that location.
9. Never invent evidence.
10. Expected and correction_value must match the
    official source value.
11. Do not flag an absent statement as a contradiction.
12. If no verified issue exists, return PASS.
13. Otherwise return FAIL.

Focus on verifiable factual and interpretation errors.
Do not report generic recommendations or omissions
in this first qualification.

Return only the required JSON structure.
"""

schema = {
    "type": "object",
    "properties": {
        "status": {
            "type": "string",
            "enum": ["PASS", "FAIL"]
        },
        "score": {
            "type": "integer",
            "minimum": 0,
            "maximum": 100
        },
        "issues": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "type": {
                        "type": "string",
                        "enum": [
                            "CONTRADICTION",
                            "INCORRECT_VALUE",
                            "INCORRECT_INTERPRETATION"
                        ]
                    },
                    "location": {
                        "type": "string"
                    },
                    "field": {
                        "type": "string"
                    },
                    "evidence": {
                        "type": "string"
                    },
                    "expected": {
                        "type": ["number", "string"]
                    },
                    "actual": {
                        "type": ["number", "string"]
                    },
                    "correction_value": {
                        "type": ["number", "string"]
                    },
                    "suggested_correction": {
                        "type": "string"
                    }
                },
                "required": [
                    "type",
                    "location",
                    "field",
                    "evidence",
                    "expected",
                    "actual",
                    "correction_value",
                    "suggested_correction"
                ],
                "additionalProperties": False
            }
        },
        "explanation": {
            "type": "string"
        }
    },
    "required": [
        "status",
        "score",
        "issues",
        "explanation"
    ],
    "additionalProperties": False
}

user_prompt = (
    "SOURCE_OF_TRUTH:\n"
    + json.dumps(data["source_of_truth"], indent=2)
    + "\n\nCANDIDATE:\n"
    + json.dumps(data["candidate"], indent=2)
)

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
            "content": user_prompt
        }
    ],
    "options": {
        "temperature": 0,
        "num_ctx": 4096,
        "num_predict": 1536
    }
}

output = base / "TC-QWEN-REAL-001-request.json"

output.write_text(
    json.dumps(request, indent=2),
    encoding="utf-8"
)

print(f"Request created: {output}")
