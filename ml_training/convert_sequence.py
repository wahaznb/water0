"""Convert sequence_lstm.keras -> TFLite and verify MAE through the runtime."""
import os

os.environ["CUDA_VISIBLE_DEVICES"] = ""

import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.metrics import mean_absolute_error

m = tf.keras.models.load_model("model/sequence_lstm.keras")
conv = tf.lite.TFLiteConverter.from_keras_model(m)
conv.optimizations = [tf.lite.Optimize.DEFAULT]
blob = conv.convert()
open("model/sequence_lstm.tflite", "wb").write(blob)
print("tflite KB:", round(len(blob) / 1024, 1))

it = tf.lite.Interpreter(model_content=blob)
it.allocate_tensors()
inp, out = it.get_input_details()[0], it.get_output_details()[0]
df = pd.read_csv("data/hydration_logs.csv")
rng = np.random.default_rng(42)
test_users = set(rng.choice(df["user_id"].unique(), size=40, replace=False))
preds, ys = [], []
for u, d in df.sort_values(["user_id", "day"]).groupby("user_id"):
    t = d["total_day_ml"].to_numpy(float) / 4000.0
    for i in range(len(t) - 7):
        if u in test_users:
            w = t[i:i + 7].reshape(1, 7, 1).astype(np.float32)
            it.set_tensor(inp["index"], w)
            it.invoke()
            preds.append(float(it.get_tensor(out["index"])[0][0]) * 4000)
            ys.append(t[i + 7] * 4000)
print("tflite-lstm MAE:", round(mean_absolute_error(ys, np.array(preds)), 1))
