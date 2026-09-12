"""Train directly on NHANES 2017-2018 (real data).

Different task than train_model.py, by necessity: NHANES has ONE 24h
recall per person — no sequences, no streaks, no "tomorrow". So this
trains the cross-sectional task: predict a person's total water intake
(all food/drink moisture, DR1TMOIS) from demographics the app knows at
signup (weight, activity, sex, age). Goal-met classification uses the
app's own formula as the bar, same as the simulator.

Needs data/nhanes_daily.csv first:
    python load_nhanes.py --out data/nhanes_daily.csv

Usage:
    python train_nhanes.py --data data/nhanes_daily.csv

Outputs (model/ is gitignored, JSON is committed):
    model/nhanes_intake_regressor.joblib
    model/nhanes_goal_classifier.joblib
    nhanes_model_metrics.json
"""

import argparse
import json
from pathlib import Path

import joblib
import pandas as pd
from sklearn.ensemble import HistGradientBoostingClassifier, HistGradientBoostingRegressor
from sklearn.inspection import permutation_importance
from sklearn.metrics import accuracy_score, mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split

FEATURES = ["weight_kg", "activity", "sex", "age_yr", "day_of_week", "goal_ml"]
REG_TARGET = "total_water_ml"
CLF_TARGET = "met_goal_total_water"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="data/nhanes_daily.csv")
    parser.add_argument("--out-dir", default="model")
    parser.add_argument("--report", default="nhanes_model_metrics.json")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    df = pd.read_csv(args.data).dropna(subset=FEATURES + [REG_TARGET, CLF_TARGET])
    print(f"loaded {len(df)} rows from {args.data}")
    # One row per person -> plain random split (no user groups to leak).
    train, test = train_test_split(df, test_size=0.2, random_state=args.seed)
    X_train, X_test = train[FEATURES], test[FEATURES]

    reg = HistGradientBoostingRegressor(max_iter=300, random_state=args.seed)
    reg.fit(X_train, train[REG_TARGET])
    pred = reg.predict(X_test)
    mean_pred = float(train[REG_TARGET].mean())
    metrics = {
        "n_train": int(len(train)),
        "n_test": int(len(test)),
        "regression_mae_ml": round(float(mean_absolute_error(test[REG_TARGET], pred)), 1),
        "regression_r2": round(float(r2_score(test[REG_TARGET], pred)), 3),
        "regression_mean_baseline_mae_ml": round(
            float((test[REG_TARGET] - mean_pred).abs().mean()), 1),
    }
    print(f"[regression] MAE={metrics['regression_mae_ml']} ml  "
          f"R2={metrics['regression_r2']}  "
          f"(mean baseline MAE={metrics['regression_mean_baseline_mae_ml']} ml)")

    clf = HistGradientBoostingClassifier(max_iter=300, random_state=args.seed)
    clf.fit(X_train, train[CLF_TARGET])
    acc = float(accuracy_score(test[CLF_TARGET], clf.predict(X_test)))
    majority = float(test[CLF_TARGET].mode()[0])
    maj_acc = float((test[CLF_TARGET] == majority).mean())
    metrics["classifier_accuracy"] = round(acc, 3)
    metrics["classifier_majority_baseline"] = round(maj_acc, 3)
    print(f"[classifier] accuracy={metrics['classifier_accuracy']}  "
          f"(majority baseline={metrics['classifier_majority_baseline']})")

    perm = permutation_importance(reg, X_test, test[REG_TARGET],
                                  n_repeats=5, random_state=args.seed)
    metrics["permutation_importance_r2_drop"] = {
        f: round(float(v), 4)
        for f, v in zip(FEATURES, perm.importances_mean)
    }
    print(f"[importance] {metrics['permutation_importance_r2_drop']}")

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    joblib.dump(reg, out_dir / "nhanes_intake_regressor.joblib")
    joblib.dump(clf, out_dir / "nhanes_goal_classifier.joblib")
    Path(args.report).write_text(json.dumps(metrics, indent=2))
    print(f"models -> {out_dir}/nhanes_*.joblib, report -> {args.report}")


if __name__ == "__main__":
    main()
