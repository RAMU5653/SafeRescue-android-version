import json
import struct
import numpy as np
import subprocess
import tflite_runtime.interpreter as tflite

# Weights: 2 classes x 64 features
# Features: [0..63] extracted by VoiceFeatureExtractor
# Let's set meaningful acoustic feature weights:
# index 0 (RMS loudness), index 1 (spectral centroid), etc.
weights = np.zeros((2, 64), dtype=np.float32)
# Non-distress baseline
weights[0, :] = -0.05
weights[0, 0] = -0.5 # lower loudness favors non-distress
# Distress signal: high loudness, high energy in features 0, 1, 2
weights[1, :] = 0.02
weights[1, 0] = 0.8  # high RMS energy correlates with voice shouting/distress
weights[1, 1] = 0.4  # high spectral flux
weights[1, 2] = 0.3  # high zero crossing rate

biases = np.array([0.5, -0.5], dtype=np.float32)

weights_bytes = list(weights.tobytes())
biases_bytes = list(biases.tobytes())

model_dict = {
    "version": 3,
    "operator_codes": [
        {
            "deprecated_builtin_code": 9,
            "version": 1,
            "builtin_code": "FULLY_CONNECTED"
        }
    ],
    "subgraphs": [
        {
            "tensors": [
                {
                    "shape": [1, 64],
                    "type": "FLOAT32",
                    "buffer": 1,
                    "name": "input_features"
                },
                {
                    "shape": [2, 64],
                    "type": "FLOAT32",
                    "buffer": 3,
                    "name": "fc_weights"
                },
                {
                    "shape": [2],
                    "type": "FLOAT32",
                    "buffer": 4,
                    "name": "fc_biases"
                },
                {
                    "shape": [1, 2],
                    "type": "FLOAT32",
                    "buffer": 2,
                    "name": "output_scores"
                }
            ],
            "inputs": [0],
            "outputs": [3],
            "operators": [
                {
                    "opcode_index": 0,
                    "inputs": [0, 1, 2],
                    "outputs": [3],
                    "builtin_options_type": "FullyConnectedOptions",
                    "builtin_options": {
                        "fused_activation_function": "NONE",
                        "keep_num_dims": False
                    }
                }
            ],
            "name": "main"
        }
    ],
    "description": "SafeRescue On-Device Voice Distress Classifier",
    "buffers": [
        {},
        {},
        {},
        {"data": weights_bytes},
        {"data": biases_bytes}
    ]
}

with open("/tmp/model.json", "w") as f:
    json.dump(model_dict, f, indent=2)

cmd = ["flatc", "-b", "--raw-binary", "/tmp/schema.fbs", "/tmp/model.json"]
subprocess.run(cmd, check=True)

# flatc creates /tmp/model.bin
# In TFLite, the file identifier is TFL3 at bytes 4..7
with open("/tmp/model.bin", "rb") as f:
    data = bytearray(f.read())

# Check / add TFL3 identifier if needed
print("Generated binary size:", len(data))

# Test loading with tflite-runtime
interp = tflite.Interpreter(model_content=bytes(data))
interp.allocate_tensors()
input_details = interp.get_input_details()
output_details = interp.get_output_details()

print("Input shape:", input_details[0]['shape'])
print("Output shape:", output_details[0]['shape'])

# Test inference with dummy features
test_input = np.zeros((1, 64), dtype=np.float32)
test_input[0, 0] = 0.95 # simulated loud distress voice
interp.set_tensor(input_details[0]['index'], test_input)
interp.invoke()
output = interp.get_tensor(output_details[0]['index'])
print("Inference successful! Output scores:", output)

# Save to SafeRescue/app/src/main/assets/models/voice_distress.tflite
with open("SafeRescue/app/src/main/assets/models/voice_distress.tflite", "wb") as f:
    f.write(data)
print("Saved to SafeRescue/app/src/main/assets/models/voice_distress.tflite!")
