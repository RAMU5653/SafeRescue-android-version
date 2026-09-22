# SafeRescue local model slot

Expected asset: `voice_distress.tflite`

This project intentionally does **not** ship a made-up or synthetic distress model.
A real, validated, license-compatible model must be supplied before enabling voice
classification in production.

Required runtime contract:
- TensorFlow Lite
- input: 64 float features
- output: 2 float scores, index 0 = non-signal, index 1 = voice-signal probability
- model execution is entirely on-device
- no cloud inference

The app reports `NOT_INSTALLED` when the asset is absent. It never substitutes a
heuristic and never claims "crime detected".
