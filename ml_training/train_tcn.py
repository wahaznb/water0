"""Custom temporal-conv net: TCN-style over the 7-day window.

Same windows/split as train_sequence.py. Conv1D lowers cleanly to TFLite
builtins (no flex delegate), so whatever wins here can actually ship.
"""
import argparse

import numpy as np
import pandas as pd
from sklearn.metrics import mean_absolute_error

WINDOW = 7
SCALE = 4000.0


def windows(df: pd.DataFrame):
    Xs, ys, groups = [], [], []
    for user, d in df.sort_values(["user_id", "day"]).groupby("user_id"):
        totals = d["total_day_ml"].to_numpy(dtype=float) / SCALE
        for i in range(len(totals) - WINDOW):
            Xs.append(totals[i:i + WINDOW])
            ys.append(totals[i + WINDOW])
            groups.append(user)
    return (np.array(Xs)[..., None],
            np.array(ys),
            np.array(groups))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="data/hydration_logs.csv")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--epochs", type=int, default=40)
    args = parser.parse_args()

    import os
    os.environ["CUDA_VISIBLE_DEVICES"] = ""
    import tensorflow as tf

    df = pd.read_csv(args.data)
    X, y, groups = windows(df)
    rng = np.random.default_rng(args.seed)
    test_users = set(rng.choice(df["user_id"].unique(), size=40, replace=False))
    te_mask = np.array([g in test_users for g in groups])
    Xtr, ytr, Xte, yte = X[~te_mask], y[~te_mask], X[te_mask], y[te_mask]

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(WINDOW, 1)),
        tf.keras.layers.Conv1D(16, 3, activation="relu"),
        tf.keras.layers.Conv1D(8, 3, activation="relu"),
        tf.keras.layers.Flatten(),
        tf.keras.layers.Dense(8, activation="relu"),
        tf.keras.layers.Dense(1),
    ])
    model.compile(optimizer="adam", loss="mae")
    model.fit(Xtr, ytr, epochs=args.epochs, batch_size=256, verbose=0,
              validation_split=0.1)
    pred = model.predict(Xte, verbose=0).ravel() * SCALE
    y_ml = yte * SCALE
    print(f"[tcn] keras MAE={mean_absolute_error(y_ml, pred):.1f} ml")

    conv = tf.lite.TFLiteConverter.from_keras_model(model)
    conv.optimizations = [tf.lite.Optimize.DEFAULT]
    blob = conv.convert()
    open("model/sequence_tcn.tflite", "wb").write(blob)
    print("tflite KB:", round(len(blob) / 1024, 1))

    it = tf.lite.Interpreter(model_content=blob)
    it.allocate_tensors()
    inp, out = it.get_input_details()[0], it.get_output_details()[0]
    preds = []
    for r in Xte.astype(np.float32):
        it.set_tensor(inp["index"], np.expand_dims(r, 0))
        it.invoke()
        preds.append(float(it.get_tensor(out["index"])[0][0]) * SCALE)
    print(f"[tcn] tflite MAE={mean_absolute_error(y_ml, np.array(preds)):.1f} ml")


if __name__ == "__main__":
    main()
