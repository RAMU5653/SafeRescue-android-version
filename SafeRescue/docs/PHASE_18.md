# Phase 18 — Real Local AI Model Runtime

## Purpose
Introduce a real on-device ML runtime boundary without fabricating an AI model.
The Android app can load a TensorFlow Lite model bundled in APK assets and execute
inference locally. If the validated model is absent or fails to load, the app
reports that state explicitly.

## Architecture
```text
Victim voice PCM
      |
      v
VoiceFeatureExtractor
      |
      v
LocalAiRepository
      |
      v
TensorFlow Lite Interpreter
      |
      v
LocalAiSignal
      |
      v
Risk engine / incident timeline (future wiring)
```

## Model contract
Expected asset: `app/src/main/assets/models/voice_distress.tflite`.
The runtime expects a 64-float input vector and two output scores. The project does
not include a synthetic classifier merely to make the feature appear complete.

## Privacy
- Model inference occurs locally on the Android device.
- Voice data is not sent to cloud AI by this runtime.
- No API key or model endpoint is stored in the app.
- Raw voice remains governed by the existing evidence/cancellation lifecycle.

## Safety language
The local model can provide a **voice signal indicator** only. It must not be
presented as proof of a crime, identity, attacker detection, or guaranteed distress.
Manual SOS remains authoritative.

## Failure modes
- model asset missing → `NOT_INSTALLED`
- invalid/corrupt model → `ERROR`
- inference failure → caller receives no signal; emergency continues
- no microphone → existing voice pipeline handles permission/unavailability

## Testing
1. Confirm missing model produces `NOT_INSTALLED` rather than a fake result.
2. With a validated compatible TFLite model installed, confirm `READY` and local
   inference on a test feature vector.
3. Verify airplane mode does not prevent model loading/inference.
4. Verify no network client is created by the AI package.

## NOT PRODUCTION READY
A validated, representative, license-compatible voice model has not been bundled.
Model quality, bias, false-positive/false-negative rates and safety thresholds must
be independently evaluated before production use.
