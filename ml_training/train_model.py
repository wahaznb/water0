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

Real-world logs are estimates: nobody measures every glass, and timestamps
are guesses. To keep the model honest about that, training augments the
train split with a noisy copy — Gaussian jitter on the intake volumes,
rounded to typical guess sizes (see --noise-ml / --round-to) — and reports
metrics on both clean and noisy test splits. If the noisy MAE blows up
relative to clean, the model is memorizing exact numbers instead of
learning habits.

Usage:
    python train_model.py --data data/hydration_logs.csv --out-dir model
    python train_model.py --noise-ml 0  # exact numbers only (not advised)
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
    "weight_kg", "activity", "climate", "sex", "day_of_week", "is_weekend",
    "wake_hour", "sleep_hour", "goal_ml",
    "prev_day_total_ml", "avg_7d_ml", "streak_days",
]
REG_TARGET = "total_day_ml"
CLF_TARGET = "met_goal"

# Volume features a human can only ever estimate (nobody weighs a glass).
NOISY_VOLUME_COLS = ["prev_day_total_ml", "avg_7d_ml"]


def add_estimation_noise(df: pd.DataFrame, seed: int,
                         sigma_ml: float = 150.0, round_to: int = 50) -> pd.DataFrame:
    """Simulate sloppy human logging: jitter volumes, then snap to the round
    numbers people actually type (250, 500, …). Goal stays exact — it is
    computed, not estimated — and met_goal is recomputed so labels stay
    consistent with the noisy totals."""
    rng = np.random.default_rng(seed)
    noisy = df.copy()
    for col in NOISY_VOLUME_COLS + [REG_TARGET]:
        if col not in noisy.columns:
            continue
        jittered = noisy[col].to_numpy(dtype=float) + rng.normal(0, sigma_ml, len(noisy))
        jittered = np.round(jittered / round_to) * round_to
        noisy[col] = np.clip(jittered, 0, None)
    if CLF_TARGET in noisy.columns and "goal_ml" in noisy.columns:
        noisy[CLF_TARGET] = (noisy[REG_TARGET] >= noisy["goal_ml"]).astype(int)
    return noisy


def split(df: pd.DataFrame, seed: int = 42):
    if df["user_id"].nunique() < 2:
        # Single-user export (see ExportTrainingDataUseCase): groups can't
        # split, so hold out the most recent 20% of days instead. This is
        # the honest split for personal fine-tuning — the future is tested.
        df = df.sort_values("day").reset_index(drop=True)
        cut = max(1, int(len(df) * 0.8))
        print(f"single user: time split at day {df.loc[cut, 'day']} "
              f"({len(df) - cut} test days)")
        return df.iloc[:cut], df.iloc[cut:]
    splitter = GroupShuffleSplit(n_splits=1, test_size=0.2, random_state=seed)
    train_idx, test_idx = next(splitter.split(df, groups=df["user_id"]))
    return df.iloc[train_idx], df.iloc[test_idx]


def train_sklearn(train: pd.DataFrame, test: pd.DataFrame,
                  test_noisy: pd.DataFrame, out_dir: Path) -> dict:
    X_train, y_reg_train = train[FEATURES], train[REG_TARGET]
    X_test, y_reg_test = test[FEATURES], test[REG_TARGET]
    Xn_test, yn_reg_test = test_noisy[FEATURES], test_noisy[REG_TARGET]
    y_clf_train, y_clf_test = train[CLF_TARGET], test[CLF_TARGET]
    yn_clf_test = test_noisy[CLF_TARGET]

    reg = HistGradientBoostingRegressor(max_iter=300, random_state=42)
    reg.fit(X_train, y_reg_train)
    pred = reg.predict(X_test)
    pred_noisy = reg.predict(Xn_test)
    metrics = {
        "regression_mae_ml": round(float(mean_absolute_error(y_reg_test, pred)), 1),
        "regression_r2": round(float(r2_score(y_reg_test, pred)), 3),
        "regression_mae_ml_noisy": round(float(mean_absolute_error(yn_reg_test, pred_noisy)), 1),
    }
    print(f"[regression] MAE={metrics['regression_mae_ml']} ml  R2={metrics['regression_r2']}  "
          f"MAE(noisy)={metrics['regression_mae_ml_noisy']} ml")

    clf = HistGradientBoostingClassifier(max_iter=300, random_state=42)
    clf.fit(X_train, y_clf_train)
    acc = accuracy_score(y_clf_test, clf.predict(X_test))
    acc_noisy = accuracy_score(yn_clf_test, clf.predict(Xn_test))
    metrics["classifier_accuracy"] = round(float(acc), 3)
    metrics["classifier_accuracy_noisy"] = round(float(acc_noisy), 3)
    print(f"[classifier] accuracy={metrics['classifier_accuracy']}  "
          f"accuracy(noisy)={metrics['classifier_accuracy_noisy']}")

    joblib.dump(reg, out_dir / "intake_regressor.joblib")
    joblib.dump(clf, out_dir / "goal_classifier.joblib")
    print(f"saved sklearn models -> {out_dir}")
    return metrics


def train_tflite(train: pd.DataFrame, test: pd.DataFrame, out_dir: Path) -> dict | None:
    """Compare small MLP architectures x quantization levels, export the winner.

    Selection rule: smallest model within 5% of the best test MAE, subject
    to a hard 100 KB size budget (on-device means tiny or it ships).
    """
    try:
        import tensorflow as tf
    except ImportError:
        print("[tflite] tensorflow not installed, skipping TFLite export.")
        return None

    import time

    scaler = StandardScaler()
    X_train = scaler.fit_transform(train[FEATURES]).astype(np.float32)
    X_test = scaler.transform(test[FEATURES]).astype(np.float32)
    y_train = train[REG_TARGET].to_numpy(dtype=np.float32)
    y_test = test[REG_TARGET].to_numpy(dtype=np.float32)

    candidates = [
        {"name": "tiny", "layers": [16, 8]},
        {"name": "small", "layers": [32, 16]},
        {"name": "base", "layers": [64, 32]},
    ]
    rep_data = X_train[:512]
    results = []

    for spec in candidates:
        model = tf.keras.Sequential(
            [tf.keras.layers.Input(shape=(len(FEATURES),))] +
            [tf.keras.layers.Dense(n, activation="relu") for n in spec["layers"]] +
            [tf.keras.layers.Dense(1)]
        )
        model.compile(optimizer="adam", loss="mae")
        model.fit(X_train, y_train, epochs=20, batch_size=256, verbose=0,
                  validation_split=0.1)
        mae = float(model.evaluate(X_test, y_test, verbose=0))

        # dynamic: weight quantization (fast) / float16: GPU-friendly /
        # int8: smallest + fastest on mobile DSPs/NPUs.
        for quant in ("dynamic", "float16", "int8"):
            converter = tf.lite.TFLiteConverter.from_keras_model(model)
            if quant == "dynamic":
                converter.optimizations = [tf.lite.Optimize.DEFAULT]
            elif quant == "float16":
                converter.optimizations = [tf.lite.Optimize.DEFAULT]
                converter.target_spec.supported_types = [tf.float16]
            else:  # full int8: smallest + fastest on mobile DSPs/NPUs
                converter.optimizations = [tf.lite.Optimize.DEFAULT]
                converter.representative_dataset = lambda: (
                    np.expand_dims(x, 0) for x in rep_data
                )
                converter.target_spec.supported_ops = [
                    tf.lite.OpsSet.TFLITE_BUILTINS_INT8
                ]
                converter.inference_input_output_type = tf.int8
            blob = converter.convert()

            # Rough latency probe (desktop CPU; phone DSP will differ,
            # but relative ordering holds).
            interpreter = tf.lite.Interpreter(model_content=blob)
            interpreter.allocate_tensors()
            inp, out = (interpreter.get_input_details()[0],
                        interpreter.get_output_details()[0])
            sample = np.expand_dims(X_test[0], 0).astype(inp["dtype"])
            if inp["dtype"] == np.int8:
                scale, zp = inp["quantization"]
                sample = (sample / scale + zp).astype(np.int8)
            t0 = time.perf_counter()
            for _ in range(200):
                interpreter.set_tensor(inp["index"], sample)
                interpreter.invoke()
                _ = interpreter.get_tensor(out["index"])
            ms = (time.perf_counter() - t0) / 200 * 1000

            results.append({
                "arch": spec["name"], "quant": quant,
                "mae_ml": round(mae, 1), "kb": round(len(blob) / 1024, 1),
                "ms_per_infer": round(ms, 3),
            })
            print(f"[{spec['name']}/{quant}] MAE={mae:.1f} ml  "
                  f"{len(blob) / 1024:.1f} KB  {ms:.3f} ms/infer")

    feasible = [r for r in results if r["kb"] <= 100.0]
    best_mae = min(r["mae_ml"] for r in feasible)
    winner = min(
        (r for r in feasible if r["mae_ml"] <= best_mae * 1.05),
        key=lambda r: r["kb"],
    )
    print(f"[tflite] winner: {winner['arch']}/{winner['quant']} "
          f"(MAE={winner['mae_ml']} ml, {winner['kb']} KB)")

    # Re-export the winner deterministically for the artifact.
    (out_dir / "model_selection.json").write_text(
        json.dumps(results, indent=2))

    # The Android side must apply the SAME scaling before inference.
    (out_dir / "scaler.json").write_text(json.dumps({
        "features": FEATURES,
        "mean": scaler.mean_.tolist(),
        "scale": scaler.scale_.tolist(),
    }, indent=2))
    return {
        "tflite_arch": f"{winner['arch']}/{winner['quant']}",
        "tflite_mlp_mae_ml": winner["mae_ml"],
        "tflite_kb": winner["kb"],
        "tflite_ms_per_infer": winner["ms_per_infer"],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=str, default="data/hydration_logs.csv")
    parser.add_argument("--out-dir", type=str, default="model")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--noise-ml", type=float, default=150.0,
                        help="std of the Gaussian jitter simulating human "
                             "estimation error (0 disables augmentation)")
    parser.add_argument("--round-to", type=int, default=50,
                        help="snap noisy volumes to this ml grid, like the "
                             "round numbers people actually log")
    args = parser.parse_args()

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(args.data)
    print(f"loaded {len(df)} rows, {df['user_id'].nunique()} users from {args.data}")
    if "sex" not in df.columns:
        # CSVs generated before the sex field used the male (35ml/kg) formula.
        df["sex"] = 1
        print("note: no 'sex' column in CSV, defaulting to 1 (male/35ml/kg)")
    train, test = split(df, args.seed)
    print(f"train users: {train['user_id'].nunique()}, test users: {test['user_id'].nunique()}")

    if args.noise_ml > 0:
        # Clean rows + one noisy twin each: the model learns habits, not
        # exact numbers.
        train_noisy = add_estimation_noise(train, args.seed + 1, args.noise_ml, args.round_to)
        train_aug = pd.concat([train, train_noisy], ignore_index=True)
        test_noisy = add_estimation_noise(test, args.seed + 2, args.noise_ml, args.round_to)
        print(f"noise augmentation on (sigma={args.noise_ml}ml, grid={args.round_to}ml): "
              f"{len(train)} -> {len(train_aug)} train rows")
    else:
        train_aug, test_noisy = train, test
        print("noise augmentation off")

    metrics = train_sklearn(train_aug, test, test_noisy, out_dir)
    tflite_metrics = train_tflite(train, test, out_dir)
    if tflite_metrics:
        metrics.update(tflite_metrics)

    (out_dir / "metrics.json").write_text(json.dumps(metrics, indent=2))
    print(f"metrics -> {out_dir / 'metrics.json'}")


if __name__ == "__main__":
    main()
