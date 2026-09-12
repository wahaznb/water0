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

**Use for us:** cross-checks our `35ml × kg × activity + climate` rule
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
2. ⬜ NHANES loader: parse `.XPT` recalls → per-person daily totals +
   demographics → calibration report vs simulator
3. ⬜ Kaggle sets as secondary validation
4. ⬜ Long-term: the app's **own on-device data** (opt-in, stays on phone;
   personal fine-tuning beats any population dataset)

## What NOT to use

- Household water-meter datasets (e.g. KIOS STREaM): utility tap-water
  flow, not drinking behavior. Wrong target variable.
- Any dataset claiming per-sip timestamps for thousands of people:
  almost certainly synthetic without disclosure. Ours is synthetic
  *with* disclosure (see `generate_data.py` header).
