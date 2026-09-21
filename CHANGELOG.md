# Changelog

All notable changes to Water0 are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.1.1] - 2026-09-19

### Added
- Profile sex (female 31ml/kg / male 33ml/kg, narrowed from 35 on NHANES
  evidence) with Room v1→v2 migration,
  settings radio, and goal breakdown by sex
- Swipeable pager shell (hold-and-slide across tabs) with
  glass bottom bar and motion-blur transition
- Glass Lab in Settings: blur/tint/bevel sliders (defaults 23dp / 31% / 5%),
  applied live, no restart — kept after audit (restart flow deleted)
- Edge-to-edge display with full-bleed living background (kills grey
  system bars); Tetris line-clear delete animation in the Log tab
- Compact centered dock (no footer): fixed-width tabs, gliding selection
  badge that follows the finger, lens height measured on the lens itself
- Material icons throughout (tabs, recommendation reasons, drink entries,
  delete, navigation); glass toast snackbars
- Four-tab navigation (Home / Update / Logs / Settings): hero water glass
  back on Home with transparent unmixed drink layers, Update tab with drink
  types and working delete, long-press-drag across tabs to navigate,
  droplet selection indicator on the glass bar
- Translucent liquid-glass cards everywhere (settings, recommendations,
  logs) over a dimmed mesh; header removed for a seamless top; toast
  lifted above the glass bar
- Real liquid-glass bottom bar: ported backdrop-lens technique (refraction +
  blur + rim highlight sampled from content, AGSL on API 33+, gradient
  fallback below), driven live by the Glass Lab settings
- Lens title plates on Update/Logs/Settings; touch-blob tab indicator that
  follows the finger and morphs while held; tab-change zoom-blur pulse
  only on home transitions; drink colors fitted to the theme palette
- Plates removed everywhere (no overlap, no Home ghost): Update/Logs/
  Settings own plain in-content headers, Logs range back in content;
  tab selector is a layout-centered liquid-glass circle (no drifting dot);
  status + recs aligned to the right info zone, tank shifted left and
  dimmed off-Home; background dark pass (~40% dimmer washes/blooms)
- Top glass tab bar restored (titles + Logs range in the lens, reserved
  inset so records never slide under, pager-driven so Home never ghosts);
  recommendations breathing room (full-width, roomier cards, Canvas droplet
  buddy empty state); dock is a rounded rectangle (28dp) with a clean
  glass selection circle (inner bar removed)
- Pitch-black background pass (washes/bubbles/twinkles halved again, white
  grain cut, calm ~34s swirl); blooms breathe on a 7s clock with a small
  bright star core + sparkle in the middle of each light; dock bubble is
  one floating lens that trails the finger instead of popping tab-to-tab
- Weight is typed (kg/lb numeric field with range check), not a slider;
  notification permission is opt-in on the Reminders toggle, never at
  install; pure-black field (washes deleted, 14 bubbles / 48 stars /
  3 blooms with hot cores for contrast); full-width dock (measured tabs,
  30dp icons, 56dp bubble); recs back in the right info zone; floating
  top lens on every tab ("Water0" included) with scroll-under content
- Top lens: Water0 big + centered on Home, left-aligned elsewhere; Logs
  7d/14d/30d chips sit beside the title as glass chips; dock icons 34dp,
  blob morphs while scrubbing, edge tabs recentered, all tap animations
  off (no magnify, no ripple — the blob is the only motion); bloom stars
  smaller
- TV Girl lenses (Who Really Cares palette): top bar vivid blue #0351A3,
  dock hot pink #EE2689; pour-out tips RIGHT with spill stream + falling
  drops off the lip (mirrors the add-side pour); bloom stars wander on
  slow drift and burn back in new spots, warm yellowish starlight #FFF1BE
  that reads through the glass
- Light mode gets its own luminous blue field (dark stays pixel-identical
  pure black — every particle color branches on theme luminance); Home
  info cards canted −2° to echo the tumbler's tapered wall
- Plate titles TV Girl blue #0351A3 in dark, black in light; light theme
  goes white background + black text; light glows run dark (navy #1B2268
  halos, deeper-blue particles); all cards carry a slight black tint in
  light (dark untouched); tumbler mouth square-cut with a flat rim
- Card blur reverted (Haze removed entirely): home/logs/settings cards
  back to plain translucent tints; Home info cards keep the −2° cant but
  pivot on their left edge now
- Home cards carry the angle in their silhouette now (no rotation): only
  the left edge slants at the tumbler's ~2° wall lean; dock blob is true
  jelly — four corners on four phases plus volume-preserving squash,
  never a rounded rectangle
- Panel sits tighter under the top lens; stats + recommendations swipe
  sideways (snap carousels with dots); backdated logging (Now/1h/3h/pick
  time, same-day); quick-add drops scale with amount; Logs range is a
  sliding glass segmented bar; training augments noisy human estimates
  (150ml jitter, 50ml grid) and reports clean+noisy metrics
- Nudges suggest ranges ("around 500ml", ±20% to 50s) with the midpoint
  prefilled; goal judged by time of day (full day chugged early warns
  instead of celebrating); tapping Add opens Update with the amount
  prefilled — confirm logs it, walking away starves the pace tracker
- Overflow stream deleted (foam cap stays); tank takes a pose per tab
  (Update right, Logs left, Settings small-center); sips live in the
  stats panel grouped by type (WATER · 1100ml ×3, no time order), recs
  plain list again; range pill jellies like the dock;
  bubbles ×1.8 size/brightness, up to 36; dock glide + page slide slowed
- Stats panel quips: rotating playful lines per status, sometimes >.<
- Bubbles fully independent (own rise curve, sway rhythm, peak glow;
  24 max, 7–18dp), sips dotted >.< faces
- Bubbles go lifespan-driven like the glows (own 20–45s risetime each,
  respawned at random x — no shared loop); sips capped so the panel
  stays balanced; info zone reordered (recs up, stats down, one shared
  left edge equidistant from the tank)
- Logs range fixed (segments are fixed dp — self-measuring collapsed to
  zero and hid 7d/14d/30d); quick-add stacked vertical so amounts never
  clip; tank sunk lower on Home; Today's sips card fills the lower info
  zone; background bubbles big (5–14dp), slow (30s+ crossings), frequent
- Glow orbs go lifespan-driven: each spawns at a random spot, drifts while
  it burns exactly 10s, dies, and a reaper respawns it somewhere new —
  no loops, no sync, nothing repeats
- New launcher icon from IconKitchen (>_< bubble face): all densities +
  monochrome + store listing refreshed
- About section in Settings (version, GitHub link, license)
- Real launcher icon (IconKitchen adaptive set + monochrome + store listing)
- iPhone-style tab bar: hold to magnify nearby tabs with haptic scrub,
  drag-select with springy droplet, release to navigate
- Motion pass: press bounce on quick-add, cascading card entrances,
  gliding status tint, animated glass numerals
- Top-layer-only waves; droplet pour with splash rings (replaces the bar
  stream); glass badge on the active tab (no dot); footer chrome removed
- Living background: swirling washes + rising bubbles reacting to progress,
  twinkle specks, film grain against banding; washes turned up loud
- Background bubbles: truly random seeded layout, larger, slower (11s loop),
  sine-fade wrap so the loop never pops; fresh sky minted every cold start,
  stable across rotations
- Black-water background with random bioluminescent glow pulses (no fish,
  just lights); full glass gently overflows instead of clipping
- Tank animations play only on Home: other tabs accumulate the net ml and
  replay it as one pour/slosh on return
- Ambient mega-tank: one persistent tumbler behind every tab (cropped hero
  on Home, dim refractive backdrop elsewhere); tumbler silhouette with
  thick base + rim; icon-only dock tabs
- Home zones per layout: tank left, frosted info panel right with red
  divider, greeting box above stats; headers removed everywhere (no ghost
  plates); tab transition shrinks instead of blur-grow
- Touch circle tab indicator (measured centers, swells on press); 72dp
  tabs; 20dp radii on all text cards; drink colors fitted to the theme
- Blue-tinted lens plates; quick-add buttons with distinct icons + names
  on one glass tint; darker black water; 3x bigger/brighter/fewer/slower
  glow blooms
- Home zones per layout: tank left, frosted info panel right with red
  divider, greeting box above stats; lens plates only off-Home (pager
  driven, no ghosts); tab transition shrinks instead of blur-grow
- Touch-blob tab indicator (measured centers, morphs on touch); 20dp
  radii on all text cards; wider 72dp dock tabs
- Home is tank + recommendations only (logging lives strictly in Update);
  tall side tank with stats column; chronological unmixed drink layers
- Opt-in training-data export (Settings → Your data, 90-day simulator-schema
  CSV, fully offline) + single-user time split in `train_model.py`
- Personal-pace nudge: recommendations adapt to the user's own 7-day rhythm
  (prorated by time of day, 3-day cold-start guard, suppressed at goal)
- Custom water amounts (preset chips + slider + exact ml field)
- Liquid-glass water fill replacing the progress ring (animated level + wave)
- True drink colors (alcohol red, soda black, juice sand) rendered as
  unmixed layers; realistic liquid (depth gradient, meniscus, bubbles,
  shimmer, overfill foam); pour-out delete (lip-pinned tilt + spill arc),
  static glass otherwise; standout mesh background
- `ml_training/LEARNING_GUIDE.md`: hands-on training walkthrough incl. the
  NHANES negative result; direct NHANES training retired as a direction

### Fixed
- Evening "small sip before bed" no longer shows after goal met (gated to
  90–99%); goal-met state shows a celebration instead of drink nudges
- Tab bar rendered at the top over the status bar (dock wrapper didn't
  span the overlay); dock now pinned to the bottom
- Content cut off abruptly above the dock; lists now flow full-bleed
  underneath the floating dock instead
- Bare Text()/Icon() rendered black on dark theme (Material3 doesn't set
  LocalContentColor); scheme color provided once at the theme root
- Background gradient banding: removed large-area blur blobs, subtle
  ≤10% hydration overlay only
- Time-flaky quiet-hours unit test (injected clock hour)

### Added
- History screen: per-day totals with progress, 7/14/30-day ranges,
  expandable day cards, working delete
- Settings screen: profile, live goal breakdown, reminders, quiet hours,
  sleep window, metric/imperial units
- Adaptive reminder notifications (WorkManager chain, quiet hours, boot
  reschedule, runtime permission flow)
- Offline Python ML pipeline (`ml_training/`): synthetic data generator,
  sklearn baselines, optional TFLite export, real-data source docs
- Unit tests (14) + Compose UI tests + GitHub Actions CI
- First-run seeding of default profile and behavior

### Fixed
- `activityExtra` in daily-goal math always computed as zero
  (operator-precedence bug, caught by tests)
- Fresh installs stuck on Loading (empty Room tables emit nothing)

## [0.1.0] - 2026-09-13

First tagged release (published as v0.1 — same precedence).
