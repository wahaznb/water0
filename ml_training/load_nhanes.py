"""Download NHANES 2017-2018 files and calibrate the Water0 simulator.

What this does (roadmap step 2 in DATA_SOURCES.md):
  1. Downloads DEMO_J, DR1TOT_J, BMX_J (+ optional PAQ_J) .XPT files
     from the CDC into data/nhanes_raw/ (stdlib only, resumable-ish).
  2. Parses them with pandas.read_sas() and merges on SEQN.
  3. Keeps adults (18+) with a reliable Day-1 recall (DR1DRSTZ == 1).
  4. Maps NHANES columns onto app-domain concepts and writes one row
     per person to data/nhanes_daily.csv.
  5. Compares NHANES plain-water totals against the synthetic simulator
     (data/hydration_logs.csv) and writes nhanes_calibration.json.

What this does NOT do: NHANES has one 24h recall per person — no drink
sequences, no streaks, no wake/sleep hours. So it cannot feed
train_model.py directly (which needs prev_day_total_ml, streak_days,
...). Its honest job is grounding: check the simulator's means and the
goal formula against population data, then tune the simulator.

Verified variable names (DR1TOT_J codebook, CDC, June 2020):
  SEQN      respondent id (merge key)
  DR1DRSTZ  recall status (1 = reliable)
  DR1DAY    intake weekday, 1=Sunday..7=Saturday
  DR1_320Z  total plain water drank yesterday (gm ~= ml)
  DR1_330Z  tap water (gm), DR1BWATZ bottled water (gm)
  DEMO_J: RIAGENDR (1=male, 2=female), RIDAGEYR (age years)
  BMX_J:  BMXWT (weight, kg)
  PAQ_J:  activity questionnaire (coding varies; see map_activity())

Column assumptions that are documented, not silent:
  - climate, wake_hour, sleep_hour do not exist in NHANES -> filled with
    app defaults (TEMPERATE=1, 7, 23) and flagged imputed_* = 1.
  - activity is a coarse map from vigorous/moderate day counts; the
    report prints what share fell back to SEDENTARY.

Usage:
    python load_nhanes.py --out data/nhanes_daily.csv
    python load_nhanes.py --no-download   # reuse data/nhanes_raw/*.XPT
"""

import argparse
import json
import urllib.request
from pathlib import Path

import numpy as np
import pandas as pd

BASE = "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2017/DataFiles"
FILES = {
    "demo": "DEMO_J.XPT",
    "diet": "DR1TOT_J.XPT",
    "bmx": "BMX_J.XPT",
    "paq": "PAQ_J.XPT",  # optional: missing file -> all SEDENTARY + note
}

ACTIVITY_MULT = np.array([1.0, 1.1, 1.2, 1.3, 1.4])
CLIMATE_BONUS = np.array([0, 200, 500, 800])


def download(raw_dir: Path) -> None:
    raw_dir.mkdir(parents=True, exist_ok=True)
    for key, name in FILES.items():
        dest = raw_dir / name
        if dest.exists() and dest.stat().st_size > 0:
            print(f"[skip] {name} already cached")
            continue
        url = f"{BASE}/{name}"
        print(f"[get] {url} ...")
        try:
            urllib.request.urlretrieve(url, dest)
        except Exception as exc:
            if key == "paq":
                print(f"[warn] PAQ download failed ({exc}); activity falls back to SEDENTARY")
                continue
            raise
        # CDC serves a "Page Not Found" HTML page with HTTP 200 for bad
        # paths — reject anything that is not an XPORT file.
        with open(dest, "rb") as fh:
            magic = fh.read(32)
        if not magic.startswith(b"HEADER RECORD"):
            dest.unlink()
            raise RuntimeError(
                f"{url} did not return an XPORT file "
                f"(got {magic[:40]!r}); check BASE in this script"
            )
        print(f"[ok] {name} ({dest.stat().st_size / 1e6:.1f} MB)")


def read_xpt(path: Path) -> pd.DataFrame:
    df = pd.read_sas(path)
    # read_sas may return bytes for strings and categoricals; normalize.
    for col in df.columns:
        if df[col].dtype == object:
            df[col] = df[col].apply(
                lambda v: v.decode().strip() if isinstance(v, bytes) else v
            )
    return df


def map_activity(paq: pd.DataFrame | None) -> pd.Series:
    """Coarse 0..4 activity from PAQ day counts.

    Tries known vigorous/moderate day-count columns first
    (PAQ610/PAQ650 vigorous days, PAQ625/PAQ665 moderate days;
    77/99 = refused/don't-know -> ignored). Falls back to any
    yes(1) on work/recreation activity items -> LIGHT, else SEDENTARY.
    Returns a Series aligned to paq's index (or all zeros if no PAQ).
    """
    if paq is None or paq.empty:
        return pd.Series(dtype=int)
    idx = paq.index
    out = pd.Series(0, index=idx, dtype=int)
    used = []

    def days(cols: list[str]) -> pd.Series:
        present = [c for c in cols if c in paq.columns]
        if not present:
            return pd.Series(np.nan, index=idx)
        vals = paq[present].apply(pd.to_numeric, errors="coerce")
        vals = vals.mask(vals.isin([77, 99, 777, 999]))
        used.extend(present)
        return vals.max(axis=1)

    vig = days(["PAQ610", "PAQ650"])
    mod = days(["PAQ625", "PAQ665"])
    out[(mod >= 1) | (vig >= 1)] = 2  # MODERATE
    out[(vig >= 3) | (mod >= 5)] = 3  # ACTIVE
    if not used:
        # No day counts in this cycle: any "yes" on activity items.
        yes_cols = [c for c in paq.columns if c in
                    ("PAQ605", "PAQ620", "PAQ640", "PAQ655", "PAQ670Q", "PAQ675Q")]
        if yes_cols:
            vals = paq[yes_cols].apply(pd.to_numeric, errors="coerce")
            out[(vals == 1).any(axis=1)] = 1  # LIGHT
    return out


def app_goal(weight_kg: float, activity: int, climate: int, sex: int) -> float:
    per_kg = 35.0 if sex == 1 else 31.0
    return per_kg * weight_kg * ACTIVITY_MULT[activity] + CLIMATE_BONUS[climate]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--raw-dir", default="data/nhanes_raw")
    parser.add_argument("--out", default="data/nhanes_daily.csv")
    parser.add_argument("--report", default="nhanes_calibration.json")
    parser.add_argument("--sim", default="data/hydration_logs.csv")
    parser.add_argument("--no-download", action="store_true")
    args = parser.parse_args()

    raw_dir = Path(args.raw_dir)
    if not args.no_download:
        download(raw_dir)

    demo = read_xpt(raw_dir / FILES["demo"])
    diet = read_xpt(raw_dir / FILES["diet"])
    bmx = read_xpt(raw_dir / FILES["bmx"])
    paq_path = raw_dir / FILES["paq"]
    paq = read_xpt(paq_path) if paq_path.exists() else None
    print(f"rows: demo={len(demo)} diet={len(diet)} bmx={len(bmx)} "
          f"paq={len(paq) if paq is not None else 'MISSING'}")

    df = diet.merge(demo, on="SEQN", how="inner", suffixes=("", "_demo"))
    df = df.merge(bmx[["SEQN", "BMXWT"]], on="SEQN", how="left")
    activity = map_activity(
        paq.set_index("SEQN") if paq is not None and "SEQN" in paq.columns else None
    )

    n_raw = len(df)
    df = df[(df["DR1DRSTZ"] == 1)]
    df = df[(df["RIDAGEYR"] >= 18)]
    df = df[df["BMXWT"].apply(pd.to_numeric, errors="coerce").between(30, 300)]
    df = df[df["DR1_320Z"].apply(pd.to_numeric, errors="coerce") >= 0]
    print(f"kept {len(df)}/{n_raw} rows (reliable recall, adult, valid weight/water)")

    out = pd.DataFrame({
        "user_id": df["SEQN"].astype(int),
        "age_yr": pd.to_numeric(df["RIDAGEYR"], errors="coerce").astype(int),
        "sex": df["RIAGENDR"].map({1: 1, 2: 0}),
        "weight_kg": pd.to_numeric(df["BMXWT"], errors="coerce").round(1),
        "day_of_week": ((pd.to_numeric(df["DR1DAY"], errors="coerce") + 5) % 7).astype(int),
        "plain_water_ml": pd.to_numeric(df["DR1_320Z"], errors="coerce").round(0),
        "tap_water_ml": pd.to_numeric(df["DR1_330Z"], errors="coerce").round(0),
        "bottled_water_ml": pd.to_numeric(df["DR1BWATZ"], errors="coerce").round(0),
        # DR1TMOIS = moisture (gm) from ALL foods, beverages and water:
        # the fair comparator for the simulator's total_day_ml.
        "total_water_ml": pd.to_numeric(df["DR1TMOIS"], errors="coerce").round(0),
    }).dropna(subset=["sex", "weight_kg", "plain_water_ml"])
    out["sex"] = out["sex"].astype(int)
    out["activity"] = out["user_id"].map(
        activity.reindex(out["user_id"]).fillna(0).astype(int)
    ).fillna(0).astype(int).clip(0, 4)
    out["climate"] = 1  # not in NHANES; app default TEMPERATE
    out["imputed_climate_wake_sleep"] = 1
    out["wake_hour"] = 7
    out["sleep_hour"] = 23
    out["is_weekend"] = (out["day_of_week"] >= 5).astype(int)
    out["goal_ml"] = [
        round(app_goal(w, a, c, s)) for w, a, c, s in
        zip(out["weight_kg"], out["activity"], out["climate"], out["sex"])
    ]
    out["met_goal"] = (out["plain_water_ml"] >= out["goal_ml"]).astype(int)
    out["met_goal_total_water"] = (out["total_water_ml"] >= out["goal_ml"]).astype(int)

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out.to_csv(out_path, index=False)
    print(f"wrote {len(out)} rows -> {out_path}")

    # ---- calibration vs the synthetic simulator ----
    report: dict = {
        "n_nhanes": int(len(out)),
        "nhanes_plain_water_mean_ml": round(float(out["plain_water_ml"].mean()), 1),
        "nhanes_plain_water_median_ml": round(float(out["plain_water_ml"].median()), 1),
        "nhanes_plain_water_p90_ml": round(float(out["plain_water_ml"].quantile(0.9)), 1),
        "nhanes_total_water_mean_ml": round(float(out["total_water_ml"].mean()), 1),
        "nhanes_total_water_median_ml": round(float(out["total_water_ml"].median()), 1),
        "nhanes_total_met_rate": round(float(out["met_goal_total_water"].mean()), 3),
        "nhanes_goal_mean_ml": round(float(out["goal_ml"].mean()), 1),
        "nhanes_met_rate": round(float(out["met_goal"].mean()), 3),
        "nhanes_activity_fallback_share": round(
            float((out["activity"] == 0).mean()), 3),
    }
    for sex, label in ((0, "female"), (1, "male")):
        sub = out[out["sex"] == sex]["plain_water_ml"]
        report[f"nhanes_plain_water_mean_ml_{label}"] = round(float(sub.mean()), 1)
        report[f"n_{label}"] = int(len(sub))
        sub_tot = out[out["sex"] == sex]["total_water_ml"]
        report[f"nhanes_total_water_mean_ml_{label}"] = round(float(sub_tot.mean()), 1)

    sim_path = Path(args.sim)
    if sim_path.exists():
        sim = pd.read_csv(sim_path)
        report["sim_total_day_mean_ml"] = round(float(sim["total_day_ml"].mean()), 1)
        report["sim_goal_mean_ml"] = round(float(sim["goal_ml"].mean()), 1)
        report["sim_met_rate"] = round(float(sim["met_goal"].mean()), 3)
        for sex, label in ((0, "female"), (1, "male")):
            sub = sim[sim["sex"] == sex]["total_day_ml"]
            if len(sub):
                report[f"sim_total_day_mean_ml_{label}"] = round(float(sub.mean()), 1)
    else:
        report["sim_note"] = f"{args.sim} not found; run generate_data.py first"

    rep_path = Path(args.report)
    rep_path.write_text(json.dumps(report, indent=2))
    print(json.dumps(report, indent=2))
    print(f"report -> {rep_path}")
    print("\nFair comparator is total water (DR1TMOIS: all food/ drink moisture) "
          "vs the simulator's total_day_ml; plain-water-only comparisons "
          "understate intake by ~40%.")


if __name__ == "__main__":
    main()
