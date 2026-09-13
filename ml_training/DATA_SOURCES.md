# Real Hydration Data Sources

Honest status first: **there is no large public dataset of timestamped
personal drink logs** — that would be private health data. Our current
training data is synthetic (see `generate_data.py`). Below are the real
sources that ground it, what each is good for, and how to use them.

## 1. NHANES / WWEIA (CDC + USDA) — the gold standard ⭐

**What:** US National Health and Nutrition Examination Survey. 24-hour
dietary recalls for ~7,000–15,000 people per 2-year cycle, including
**plain water intake in grams**, plus age, gender, physical activity,
income, and day of week.

**Get it:**
- Survey data: <https://www.cdc.gov/nchs/nhanes/>
- Diet data docs + downloads: USDA ARS Food Surveys Research Group —
  <https://www.ars.usda.gov/northeast-area/beltsville-md-bhnrc/beltsville-human-nutrition-research-center/food-surveys-research-group/docs/wweia-documentation-and-data-sets>
- Catalog entry: <https://catalog.data.gov/dataset/what-we-eat-in-america-wweia-database>
- Reference stats: CDC "Fast Facts: Data on Water Consumption" —
  <https://www.cdc.gov/nutrition/php/data-research/fast-facts-water-consumption.html>

**Use for us:**
- *Calibrate* the simulator: match our synthetic daily-total distribution
  (by age/gender/activity) against NHANES means — e.g. adults ≈ 1,066 ml/d
  plain water, total dietary water ≈ 2,718 ml/d (NHANES 2011–2016); only
  ~40% meet intake recommendations.
- *Validate* the goal formula against population averages instead of
  trusting it blindly.

**Format note:** downloads are SAS transport (`.XPT`) files — read with
`pandas.read_sas()`. Needs a small loader script (not written yet).

## 2. Kaggle — Hydration Awareness Dataset

**What:** Daily water intake + lifestyle and environmental factors,
self-reported. Small, but directly on-topic (intake ↔ focus/lifestyle).

**Get it:** search "Hydration Awareness Dataset" on kaggle.com
(`prince7489/hydration-awareness-dataset`).

**Use for us:** feature validation — confirms which lifestyle features
correlate with intake; sanity-checks our feature set.

## 3. Kaggle — Daily Water Intake Recommended

**What:** Recommended daily intake by demographics (children/male/female,
litres).

**Use for us:** cross-checks our `31/33ml × kg × activity + climate` rule
against independent recommendations.

## 4. Published evidence our approach mirrors

- *Personalized Hydration Prediction* (IJIIS): Random Forest on
  activity + weather + demographics → R² 0.85 for daily intake. Same
  feature families we use — our lower R² is expected: they predict
  cross-sectional intake, we predict *tomorrow* for *new* users.
- CDC WWEIA briefs confirm our simulator's qualitative facts: active
  adults drink more; intake varies strongly by age/demographics.

## Roadmap to real data (honest order)

1. ✅ Synthetic simulator (done — behavior sequences we can't get elsewhere)
2. ✅ NHANES loader (done — `load_nhanes.py`, CDC 2017-2018, n=4,931 adults):
   `python load_nhanes.py --out data/nhanes_daily.csv` downloads the four
   `.XPT` files to `data/nhanes_raw/`, merges on SEQN, keeps reliable
   adult recalls, and writes per-person rows + `nhanes_calibration.json`.
   Fair comparator is **total water** (`DR1TMOIS`, all food/drink
   moisture), not plain water alone (understates intake ~40%).
   Headline 2017-2018 result: NHANES total-water mean 2,870 ml/d
   (F 2,626 / M 3,127) vs simulator 2,650 ml/d (F 2,550 / M 2,748) —
   within ~10%. Population total-water met-rate vs the app formula is
   0.30 vs the simulator's 0.35 (discipline mean tuned 0.90 → 0.80 to
   close the original 0.47 gap). Re-run the loader to
   reproduce; raw XPTs are gitignored, the JSON report is committed.
3. ⏭️ Kaggle sets — skipped: NHANES proved sufficient for grounding,
   and one-row-per-person sets can't train the sequence task anyway.
   (A `load_kaggle.py` adapter was prototyped and removed; ask to
   resurrect it if a longitudinal Kaggle set appears.)
4. ✅ Long-term: the app's **own on-device data** (shipped — Settings →
   Your data → Export training CSV; opt-in, 90 days, simulator schema,
   never uploaded). `train_model.py` detects single-user CSVs and holds
   out the most recent 20% of days instead of group-splitting. Personal
   fine-tuning loop is open; on-device retraining stays future work.

## Formula check (NHANES 2017-2018, n=4,813 adults)

Through-origin OLS of total water on weight: **32.3 ml/kg** overall
(F 31.6 / M 32.9). The app base has been narrowed accordingly: 31/35 →
31/33, putting the modeled gap (2) near the empirical gap (1.3) instead
of 3× above it. A with-intercept fit still dumps most intake into a
~2.3 L intercept (~6 ml/kg marginal) — weight alone predicts little,
consistent with the negative training result below. Goals remain above
population means by design (only ~30% meet them).

## Direct training on NHANES (tried — negative result, kept honest)

`train_nhanes.py` trains the cross-sectional task on `nhanes_daily.csv`
(one row per person: predict total water from weight/activity/sex/age/
weekday/goal). Result (n=4,931, seed 42): regression MAE 1,123 ml at
R² **−0.08** (worse than predicting the mean, MAE 1,090), classifier
accuracy 0.70 vs majority baseline 0.72. Only `age_yr` carries signal;
the coarse PAQ activity map carries none.

Verdict: single-recall demographics cannot train intake prediction —
one day's intake is dominated by day noise, and habit features
(`prev_day_total_ml`, `avg_7d_ml`, `streak_days`) don't exist in
single-recall data. The NHANES model is **not shipped**. The pipeline
stands: simulator trains (it has behavior sequences), NHANES calibrates
(distributions + goal formula). Real training data worth having is
longitudinal — i.e. the app's own opt-in logs (step 4).

## What NOT to use

- Household water-meter datasets (e.g. KIOS STREaM): utility tap-water
  flow, not drinking behavior. Wrong target variable.
- Any dataset claiming per-sip timestamps for thousands of people:
  almost certainly synthetic without disclosure. Ours is synthetic
  *with* disclosure (see `generate_data.py` header).
