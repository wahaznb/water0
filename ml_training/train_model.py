"""Train hydration models on the synthetic dataset.

Two tasks (same features, mirroring what the Android app knows at
prediction time):
  1. Regression: predict tomorrow's total intake (ml).
  2. Classification: predict whether the user will meet their goal.

Split is BY USER (GroupShuffleSplit) so no user appears in both train
and test — otherwise the model just memorizes personal habits.

Models:
  - sklearn HistGradientBoosting baseline (always runs).
  - Optional small Keras MLP -> TFLite export for on-device use
    (runs only if `tensorflow` is installed; see requirements.txt).

Usage:
    python train_model.py --data data/hydration_logs.csv --out-dir model
"""

import argparse
import json
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import HistGradientBoostingClassifier, HistGradientBoostingRegressor
from sklearn.metrics import accuracy_score, mean_absolute_error, r2_score
from sklearn.model_selection import GroupShuffleSplit
from sklearn.preprocessing import StandardScaler

FEATURES = [
    "weight_kg", "activity", "climate", "day_of_week", "is_weekend",
    "wake_hour", "sleep_hour", "goal_ml",
    "prev_day_total_ml", "avg_7d_ml", "streak_days",
]
REG_TARGET = "total_day_ml"
CLF_TARGET = "met_goal"


def split(df: pd.DataFrame, seed: int = 42):
    splitter = GroupShuffleSplit(n_splits=1, test_size=0.2, random_state=seed)
    train_idx, test_idx = next(splitter.split(df, groups=df["user_id"]))
    return df.iloc[train_idx], df.iloc[test_idx]


def train_sklearn(train: pd.DataFrame, test: pd.DataFrame, out_dir: Path) -> dict:
    X_train, y_reg_train = train[FEATURES], train[REG_TARGET]
    X_test, y_reg_test = test[FEATURES], test[REG_TARGET]
    y_clf_train, y_clf_test = train[CLF_TARGET], test[CLF_TARGET]

    reg = HistGradientBoostingRegressor(max_iter=300, random_state=42)
    reg.fit(X_train, y_reg_train)
    pred = reg.predict(X_test)
    metrics = {
        "regression_mae_ml": round(float(mean_absolute_error(y_reg_test, pred)), 1),
        "regression_r2": round(float(r2_score(y_reg_test, pred)), 3),
    }
    print(f"[regression] MAE={metrics['regression_mae_ml']} ml  R2={metrics['regression_r2']}")

    clf = HistGradientBoostingClassifier(max_iter=300, random_state=42)
    clf.fit(X_train, y_clf_train)
    acc = accuracy_score(y_clf_test, clf.predict(X_test))
    metrics["classifier_accuracy"] = round(float(acc), 3)
    print(f"[classifier] accuracy={metrics['classifier_accuracy']}")

    joblib.dump(reg, out_dir / "intake_regressor.joblib")
    joblib.dump(clf, out_dir / "goal_classifier.joblib")
    print(f"saved sklearn models -> {out_dir}")
    return metrics


def train_tflite(train: pd.DataFrame, test: pd.DataFrame, out_dir: Path) -> dict | None:
    try:
        import tensorflow as tf
    except ImportError:
        print("[tflite] tensorflow not installed, skipping TFLite export.")
        return None

    scaler = StandardScaler()
    X_train = scaler.fit_transform(train[FEATURES]).astype(np.float32)
    X_test = scaler.transform(test[FEATURES]).astype(np.float32)
    y_train = train[REG_TARGET].to_numpy(dtype=np.float32)
    y_test = test[REG_TARGET].to_numpy(dtype=np.float32)

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(len(FEATURES),)),
        tf.keras.layers.Dense(32, activation="relu"),
        tf.keras.layers.Dense(16, activation="relu"),
        tf.keras.layers.Dense(1),
    ])
    model.compile(optimizer="adam", loss="mae")
    model.fit(X_train, y_train, epochs=20, batch_size=256, verbose=0,
              validation_split=0.1)
    mae = float(model.evaluate(X_test, y_test, verbose=0))
    print(f"[tflite-mlp] test MAE={mae:.1f} ml")

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]  # small enough for any phone
    tflite_bytes = converter.convert()
    (out_dir / "hydration_goal.tflite").write_bytes(tflite_bytes)
    print(f"saved {(out_dir / 'hydration_goal.tflite').stat().st_size / 1024:.1f} KB -> {out_dir / 'hydration_goal.tflite'}")

    # The Android side must apply the SAME scaling before inference.
    (out_dir / "scaler.json").write_text(json.dumps({
        "features": FEATURES,
        "mean": scaler.mean_.tolist(),
        "scale": scaler.scale_.tolist(),
    }, indent=2))
    return {"tflite_mlp_mae_ml": round(mae, 1)}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=str, default="data/hydration_logs.csv")
    parser.add_argument("--out-dir", type=str, default="model")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(args.data)
    print(f"loaded {len(df)} rows, {df['user_id'].nunique()} users from {args.data}")
    train, test = split(df, args.seed)
    print(f"train users: {train['user_id'].nunique()}, test users: {test['user_id'].nunique()}")

    metrics = train_sklearn(train, test, out_dir)
    tflite_metrics = train_tflite(train, test, out_dir)
    if tflite_metrics:
        metrics.update(tflite_metrics)

    (out_dir / "metrics.json").write_text(json.dumps(metrics, indent=2))
    print(f"metrics -> {out_dir / 'metrics.json'}")


if __name__ == "__main__":
    main()
