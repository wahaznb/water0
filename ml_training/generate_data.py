"""Generate a synthetic hydration dataset that mirrors the Water0 app domain.

Each row is one user-day. The simulation uses the SAME goal formula as
RecommendationEngine (31/33ml x weight x activity multiplier + climate bonus,
by sex),
so models trained here stay consistent with the app's rule-based v1.

Usage:
    python generate_data.py --users 200 --days 60 --seed 42 --out data/hydration_logs.csv
"""

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

ACTIVITY_MULT = np.array([1.0, 1.1, 1.2, 1.3, 1.4])  # sedentary..very_active
CLIMATE_BONUS = np.array([0, 200, 500, 800])  # cold..very_hot

# Probability of a drink event in each hour (peaks: morning, midday, evening).
# Calibrated so a typical day yields ~10 events x ~350 ml ~= the goal formula.
HOURLY_P = np.array([
    0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.06,  # 0-6
    0.75, 0.90, 0.54, 0.45, 0.66, 0.90,        # 7-12
    0.84, 0.60, 0.48, 0.54, 0.72, 0.84,        # 13-18
    0.78, 0.60, 0.30, 0.12, 0.03, 0.00,        # 19-23
])


def daily_goal(weight_kg: float, activity: int, climate: int, sex: int = 1) -> float:
    per_kg = 33.0 if sex == 1 else 31.0
    return per_kg * weight_kg * ACTIVITY_MULT[activity] + CLIMATE_BONUS[climate]


def simulate_user(rng: np.random.Generator, user_id: int, n_days: int) -> list[dict]:
    weight = float(np.clip(rng.normal(72, 14), 45, 120))
    activity = int(rng.choice(5, p=[0.25, 0.25, 0.25, 0.15, 0.10]))
    climate = int(rng.choice(4, p=[0.15, 0.45, 0.30, 0.10]))
    sex = int(rng.choice(2, p=[0.5, 0.5]))  # 0=female (31ml/kg), 1=male (33ml/kg)
    wake = int(rng.integers(5, 9))
    sleep = int(rng.integers(21, 24))
    # Each user has a personal "discipline": fraction of their need they drink.
    # Calibrated against NHANES 2017-2018 (see load_nhanes.py): population
    # total-water met-rate vs the app formula is ~0.28; mean 0.90 overshot
    # it (sim met-rate 0.47). Mean 0.80 lands the simulator near ~0.34
    # (exact match isn't the goal: the NHANES sample is heavier, so its
    # formula goals run harder than the simulator's).
    discipline = float(np.clip(rng.normal(0.80, 0.18), 0.40, 1.20))

    goal = daily_goal(weight, activity, climate, sex)
    rows: list[dict] = []
    prev_total = goal  # warm start
    recent = [goal] * 7
    streak = 0

    for day in range(n_days):
        dow = day % 7
        is_weekend = int(dow >= 5)
        # Weekends: slightly lazier drinking.
        day_factor = 0.92 if is_weekend else 1.0

        total = 0.0
        for hour in range(24):
            if hour < wake or hour >= sleep:
                continue
            p = HOURLY_P[hour] * day_factor
            if rng.random() < p:
                # Amount scales with personal need + noise.
                amount = np.clip(rng.normal(320, 90), 100, 800)
                amount *= (goal / 2500.0) ** 0.5
                total += amount

        total *= discipline * rng.normal(1.0, 0.06)
        total = round(float(total))
        met = int(total >= goal)

        rows.append({
            "user_id": user_id,
            "day": day,
            "day_of_week": dow,
            "is_weekend": is_weekend,
            "weight_kg": round(weight, 1),
            "activity": activity,
            "climate": climate,
            "sex": sex,
            "wake_hour": wake,
            "sleep_hour": sleep,
            "goal_ml": round(goal),
            "prev_day_total_ml": round(prev_total),
            "avg_7d_ml": round(float(np.mean(recent))),
            # NOTE: streak as known at the START of the day (past only).
            # Recording the post-update streak here would leak today's
            # label into the features (streak == 0 would mean "not met").
            "streak_days": streak,
            "total_day_ml": total,
            "met_goal": met,
        })
        streak = streak + 1 if met else 0
        prev_total = total
        recent.append(total)
        recent = recent[-7:]

    return rows


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--users", type=int, default=200)
    parser.add_argument("--days", type=int, default=60)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--out", type=str, default="data/hydration_logs.csv")
    args = parser.parse_args()

    rng = np.random.default_rng(args.seed)
    rows: list[dict] = []
    for user_id in range(args.users):
        rows.extend(simulate_user(rng, user_id, args.days))

    df = pd.DataFrame(rows)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(out, index=False)
    print(f"wrote {len(df)} rows ({args.users} users x {args.days} days) -> {out}")
    print(f"goal-met rate: {df['met_goal'].mean():.3f}")


if __name__ == "__main__":
    main()
