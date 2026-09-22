# SafeRescue Phase 9 — Local Risk Engine

## Scope
Phase 9 adds a deterministic, local, auditable 0–100 heuristic risk engine. It does not use cloud AI and does not claim to detect a crime or distress.

## Inputs
- Manual SOS active state (authoritative baseline)
- Location availability and latest accuracy
- Victim-voice acoustic signal metrics from Phase 8

## Scoring
- Manual SOS: +40
- Location unavailable/error: +5
- Location permission unavailable: +3
- Location accuracy above 100 m: +3
- Elevated voice RMS signal: +4 to +12 depending on threshold
- Elevated voice peak signal: +1 to +3 depending on threshold
- Final score is clamped to 0–100

Severity follows the master specification:
- 0–24 LOW
- 25–49 MEDIUM
- 50–74 HIGH
- 75–100 CRITICAL

## Safety/security
- Score is local and deterministic.
- Every contribution has an ID, label, points and explanation.
- No cloud transmission or secrets are introduced.
- Risk never starts, stops, cancels or downgrades a manual SOS.
- Acoustic thresholds are engineering heuristics, not scientifically validated distress detection.
- No movement anomaly signal is claimed yet because a durable movement history is not implemented.

## Test plan
1. Inactive state returns 0/LOW.
2. Active manual SOS produces the baseline score of 40/MEDIUM.
3. Location and voice signals increase the score only through explicit factors.
4. Score is always bounded to 0–100.
5. Severity boundaries are 24/25/50/75.
6. Cancelled emergency resets the live risk score.
