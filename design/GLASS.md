# hy2ox1 Glass Design Language

Single source of truth for every glass surface, current and future.
Read this before adding any widget with transparency, blur, or shine.

## Rule (from Apple HIG via liquid_glass_widgets)

Glass lives on **navigation and control chrome** (top plates, dock,
buttons, chips, graphs-as-instruments). Content cards stay **frost**:
translucent fills, no refraction. Never nest refractive glass inside
refractive glass.

## Tokens (`ui.theme`)

| Token | Value | Notes |
|---|---|---|
| Card radius | `Radii.md` = 16dp | Every card, everywhere |
| Card fill | `primaryContainer` 0.16 | Cards == buttons transparency |
| Card border | white top 0.16 → outline 0.22 | `glassCardBorder()` |
| Dock corner | 42dp, max 45 | Glass Lab |
| Lens blur | 6dp default (0.25) | 24dp saturates; graphs 0.7 |
| Lens refraction | 1x chrome, 2x instruments | `LensCard(refraction = …)` |
| Press | 0.96 spring (ζ=0.5, k=300) + white Plus glow | `prismalPress()` |
| Dock pill | 56dp circle, primary 0.22 + primary shadow | glides, never morphs |
| Status ring | 13dp stroke, 300° arc, bottom gap | red / green / blue by status |

## Motion

- Springs over tweens for anything touch-driven (press, pill glide,
  page settle). Critically damped or bust — no bounce except delight
  moments.
- Background life runs on lifespans (spawn → drift → die → respawn),
  never loops. Wall-clock motion, 1s reaper.
- No full-page blur during gestures (stutter source, measured).

## Light

- One top light: white specular upper-left, brighter rims up top.
- Gradient end stops always fade the SAME hue (transparent black
  renders as mud through the lens — twice bitten).
- Dark: navy field, blue-forward glows. Light: blue field, white
  elements.

## Sources & attribution

- Lens container technique: Mortd3kay/liquid-glass-android (Apache-2.0).
- Optical recipe + press behavior: styropyr0/PrismalAGSL (MIT).
- Nav-bar language (pill indicator, glow, glass-for-chrome rule):
  sdegenaar/liquid_glass_widgets (MIT).
- Jellyfish reference studied, never shipped: namidaco/namida
  (proprietary EULA — our animal is original).
- Future widget ideas parked: AmirJabbari/flutter-glow (glow widgets),
  JumpingKeyCaps/DynamicVisualEffectsAGSL (touch ripples as background
  variant, holo foil).
