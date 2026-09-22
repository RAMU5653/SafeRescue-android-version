# Phase 18 Skill Application

## Impeccable / Taste
AI status is explicit and honest: READY only means a real model loaded successfully.
No fake "AI Ready" badge is allowed.

## Motion
Model loading should use a short progress state only; inference must not block SOS.

## Cybersecurity skills
- local-only inference boundary
- no embedded API keys
- bounded model input
- explicit model version metadata
- fail closed for model availability
- no sensitive AI logging

## Student-friendly architecture
`LocalAiRepository` owns model lifecycle, `TfliteLocalAiModel` owns interpreter
adaptation, and `VoiceFeatureExtractor` only creates numeric features.
