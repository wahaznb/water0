"""With-AI vs without-AI coaching simulation (Water0 v0.4 cycle).

Mirrors the app's real coaching logic, not a strawman:
- goal: RecommendationEngine.calculateDailyGoal (per-kg x activity + climate)
- nudge sizing: suggestSipMl (250 + (gap-1)*75, snapped to 50s, 150..500)
- nudge gating: recencySuggestion rules (behind prorated pace, gap>=45min,
  active window only) + morning start (250ml if <250 by wake+2)
- cadence: calculateNextReminderInterval (60min base; x0.9 if
  response>0.7; x1.5 if <0.3; x0.7 behind; x1.3 ahead; quiet hours skip)
- habit baseline: generate_data.py hourly probs, weekend dip, personal
  discipline (NHANES-calibrated), day noise

The ONE free parameter is responsiveness: P(act on a nudge). We sweep
it (0.2 / 0.5 / 0.8) and report the range, not a point estimate —
the effect size belongs to the assumption, stated up front.

Arms: CONTROL (no nudges) vs COACHED (app logic). Same users both arms
(paired), several seeds.

Usage:
    .venv/bin/python simulate_coach.py --users 300 --days 60 --seeds 3
"""

import argparse
import json
from pathlib import Path

import numpy as np

ACTIVITY_MULT = np.array([1.0, 1.1, 1.2, 1.3, 1.4])
CLIMATE_BONUS = np.array([0, 200, 500, 800])
HOURLY_P = np.array([
    0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.06,
    0.75, 0.90, 0.54, 0.45, 0.66, 0.90,
    0.84, 0.60, 0.48, 0.54, 0.72, 0.84,
    0.78, 0.60, 0.30, 0.12, 0.03, 0.00,
])


def daily_goal(weight, activity, climate, sex):
    per_kg = 33.0 if sex == 1 else 31.0
    return per_kg * weight * ACTIVITY_MULT[activity] + CLIMATE_BONUS[climate]


def suggest_sip_ml(gap_h):
    return int(np.clip(round((250 + (gap_h - 1) * 75) / 50) * 50, 150, 500))


def day_fraction(hour, wake, sleep):
    return float(np.clip((hour - wake + 1) / max(sleep - wake, 1), 0.0, 1.0))


def make_population(rng, n_users):
    pop = []
    for u in range(n_users):
        weight = float(np.clip(rng.normal(72, 14), 45, 120))
        activity = int(rng.choice(5, p=[0.25, 0.25, 0.25, 0.15, 0.10]))
        climate = int(rng.choice(4, p=[0.15, 0.45, 0.30, 0.10]))
        sex = int(rng.choice(2))
        wake = int(rng.integers(5, 9))
        sleep = int(rng.integers(21, 24))
        discipline = float(np.clip(rng.normal(0.80, 0.18), 0.40, 1.20))
        pop.append({
            "id": u, "weight": weight, "activity": activity,
            "climate": climate, "sex": sex, "wake": wake, "sleep": sleep,
            "discipline": discipline,
            "goal": daily_goal(weight, activity, climate, sex),
        })
    return pop


def simulate_day(rng, user, coached, responsiveness, interval_base=60):
    """Returns (total_ml, met, nudge_count, response_count)."""
    goal = user["goal"]
    wake, sleep = user["wake"], user["sleep"]
    span = max(sleep - wake, 1)
    total = 0.0
    nudges = 0
    responses = 0
    resp_rate = 0.5
    last_drink_h = None
    next_tick_h = wake + interval_base / 60.0
    interval = float(interval_base)

    def behind(h):
        return total < goal * day_fraction(int(h), wake, sleep)

    h = float(wake)
    while h < sleep:
        # Base habit sip in this 15-min slot.
        slot_p = HOURLY_P[int(h)] / 4.0 * user["discipline"]
        if rng.random() < slot_p:
            amt = float(np.clip(rng.normal(320, 90), 100, 800))
            amt *= (goal / 2500.0) ** 0.5
            total += amt
            last_drink_h = h

        if coached and h >= next_tick_h:
            # Adaptive cadence (mirrors calculateNextReminderInterval).
            interval = float(interval_base)
            if resp_rate > 0.7:
                interval *= 0.9
            elif resp_rate < 0.3:
                interval *= 1.5
            interval *= 0.7 if behind(h) else 1.3
            interval = float(np.clip(interval, 30, 240))
            next_tick_h = h + interval / 60.0

            gap = (h - wake + 1) if last_drink_h is None else (h - last_drink_h)
            morning = (h < wake + 2 and total < 250)
            if behind(h) and gap >= 0.75:
                nudge_ml = 250 if (morning and last_drink_h is None) \
                    else suggest_sip_ml(gap)
                nudges += 1
                if rng.random() < responsiveness:
                    total += nudge_ml * float(rng.normal(1.0, 0.15))
                    last_drink_h = h
                    responses += 1
                resp_rate = responses / max(nudges, 1)

        h += 0.25

    total *= float(rng.normal(1.0, 0.06))
    return total, total >= goal, nudges, responses


def run(users, days, responsiveness, seed):
    rng = np.random.default_rng(seed)
    pop = make_population(rng, users)
    out = {"control": [], "coached": []}
    for user in pop:
        for arm in ("control", "coached"):
            met_days = 0
            pcts = []
            nud = 0
            for _ in range(days):
                total, met, n, _ = simulate_day(
                    rng, user, coached=(arm == "coached"),
                    responsiveness=responsiveness)
                met_days += met
                pcts.append(100.0 * total / user["goal"])
                nud += n
            out[arm].append({
                "met_rate": met_days / days,
                "median_pct": float(np.median(pcts)),
                "nudges_per_day": nud / days,
            })
    return out


def summarize(runs):
    table = {}
    for arm in ("control", "coached"):
        met = [u["met_rate"] for r in runs for u in r[arm]]
        pct = [u["median_pct"] for r in runs for u in r[arm]]
        table[arm] = {
            "users": len(met),
            "goal_met_rate_mean": round(float(np.mean(met)), 3),
            "goal_met_rate_p10_p90": [round(float(np.percentile(met, 10)), 3),
                                      round(float(np.percentile(met, 90)), 3)],
            "median_pct_of_goal_mean": round(float(np.mean(pct)), 1),
        }
    nud = [u["nudges_per_day"] for r in runs for u in r["coached"]]
    table["coached"]["nudges_per_day_mean"] = round(float(np.mean(nud)), 1)
    table["lift_pp"] = round(
        100 * (table["coached"]["goal_met_rate_mean"] -
               table["control"]["goal_met_rate_mean"]), 1)
    return table


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--users", type=int, default=300)
    ap.add_argument("--days", type=int, default=60)
    ap.add_argument("--seeds", type=int, default=3)
    ap.add_argument("--responsiveness", type=float, nargs="+",
                    default=[0.2, 0.5, 0.8])
    ap.add_argument("--out", type=str, default="reports/coach_simulation.json")
    args = ap.parse_args()

    report = {"config": vars(args), "by_responsiveness": {}}
    for r in args.responsiveness:
        runs = [run(args.users, args.days, r, 1000 + s)
                for s in range(args.seeds)]
        report["by_responsiveness"][str(r)] = summarize(runs)
        s = report["by_responsiveness"][str(r)]
        print(f"responsiveness={r}: control met={s['control']['goal_met_rate_mean']:.3f} "
              f"coached met={s['coached']['goal_met_rate_mean']:.3f} "
              f"lift={s['lift_pp']:+.1f}pp "
              f"nudges/day={s['coached']['nudges_per_day_mean']:.1f}")
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(report, indent=2))
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
