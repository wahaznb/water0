"""Custom sequence net: does *yesterday's shape* beat day-stats?

Instead of 12 aggregate features, the model sees the last 7 daily totals
as a sequence (LSTM) and predicts day 8. Same user-holdout discipline as
train_model.py (seed 42, same 40 test users) so numbers compare directly
against the sklearn 555 MAE baseline.

Usage:
    python train_sequence.py --data data/hydration_logs.csv
"""

import argparse

import numpy as np
import pandas as pd
from sklearn.metrics import mean_absolute_error

WINDOW = 7
SCALE = 4000.0  # fixed intake scale: no fitted scaler to ship


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
    parser.add_argument("--epochs", type=int, default=30)
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
    print(f"windows: {len(X)} train / {len(Xte)} test")

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(WINDOW, 1)),
        tf.keras.layers.LSTM(16),
        tf.keras.layers.Dense(8, activation="relu"),
        tf.keras.layers.Dense(1),
    ])
    model.compile(optimizer="adam", loss="mae")
    model.fit(Xtr, ytr, epochs=args.epochs, batch_size=256, verbose=0,
              validation_split=0.1)
    pred = model.predict(Xte, verbose=0).ravel() * SCALE
    y_ml = yte * SCALE
    print(f"[lstm-16] MAE={mean_absolute_error(y_ml, pred):.1f} ml")
    print(f"[repeat-last] MAE={mean_absolute_error(y_ml, Xte[:, -1, 0] * SCALE):.1f} ml")
    print(f"[mean] MAE={mean_absolute_error(y_ml, np.full_like(y_ml, ytr.mean() * SCALE)):.1f} ml")
    model.save("model/sequence_lstm.keras")
    print("saved -> model/sequence_lstm.keras")


if __name__ == "__main__":
    main()
