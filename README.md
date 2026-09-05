# ULTRACAM

**Liquid optics. Raw hardware.**

ULTRACAM is a pro camera app for Android with a liquid-glass **prism UI** and
deep, direct access to the phone's camera hardware. It is built to squeeze
every drop of performance out of whatever sensors and lenses your device
actually has — and to tell you exactly what those are.

---

## What's inside

### Hardware controls (the "squeeze the sensor" part)
| Control | How |
|---|---|
| **Every lens** | Enumerates all cameras (ultrawide / wide / tele / front), labels them by 35mm-equivalent focal length, and lets you switch by ID |
| **Full sensor readout** | Picks from the device's real JPEG output sizes — capture at native 12/48/108/200 MP, 4:3, 16:9, whatever your sensor exposes |
| **Manual ISO** | Real `SENSOR_SENSITIVITY` override on the device's actual ISO ladder |
| **Manual shutter** | `SENSOR_EXPOSURE_TIME` from 1/8000s to the sensor's true max (up to 30s+ on supporting hardware) |
| **Manual white balance** | Kelvin dial converted to `COLOR_CORRECTION_GAINS` (RGGB) via blackbody math, 2500K–9500K |
| **Manual focus** | `LENS_FOCUS_DISTANCE` in diopters across the lens' real focus range |
| **Exposure compensation** | Hardware AE compensation index across the full supported range |
| **Zoom** | Pinch + presets, respecting the hardware zoom range (including sub-1x ultrawide) |
| **Tap-to-focus** | AF + AE + AWB metering at the tap point with hardware focus confirmation |
| **Torch & flash** | Hardware torch, flash off/auto/on for stills |
| **Frame-rate target** | Fixed fps ranges (30/60/…) read from `AE_AVAILABLE_TARGET_FPS_RANGES` |
| **Capture pipeline** | MAX QUALITY (full processing) or MIN LATENCY modes |

### Instrumentation
- **Live luminance histogram** with clipping warnings (computed from YUV analysis frames)
- **Focus peaking** — edge-detect overlay, cyan/magenta by strength
- **Horizon level + compass** — from the rotation-vector sensor (accelerometer/gyro/magnetometer fusion); the glass sheen also tilts with the horizon
- **Hardware dossier** — sensor size, pixel pitch, crop factor, ISO/shutter ranges, OIS, Camera2 hardware level, and more, per lens

### Capture
- Tap shutter to shoot, **press-and-hold for burst**
- Self-timer (3s / 10s) with cancel
- Full-screen capture flash, haptics, and system shutter sound (toggleable)
- Saves to `Pictures/ULTRACAM` (MediaStore on Android 10+, app dir + MediaScanner on 8.x)
- Last-shot chip: tap to open, long-press to share

### The liquid prism UI
- Translucent **liquid glass panels** with chromatic (prism) hairline borders
- Sheen that **flows with device tilt** — the UI responds to gravity
- Conic prism shutter ring, springy morphing mode pill, glass sliders with droplet thumbs
- Monospace instrument readouts (`ISO400 1/120 · AWB · AF · 30FPS · 2.0×`)
- Prism refraction fringes on the viewfinder edges

## Getting the app

### Option A — download the APK from GitHub Actions (no tooling needed)
> **One-time setup:** the agent's credential isn't allowed to create workflow
> files, so the CI config ships as [`ci/android-build.yml`](ci/android-build.yml).
> To enable it: open
> [this link](https://github.com/MAVERICKx1902/ULTRACAM-/new/arena/01a070aa-ultracam?filename=.github/workflows/android-build.yml),
> paste the contents of `ci/android-build.yml`, and commit — done.

1. Push (or merge) this branch — the **Build ULTRACAM APK** workflow then runs on every push.
2. Open the repo → **Actions** → latest **Build ULTRACAM APK** run → **Artifacts** → `ULTRACAM-debug-apk`.
3. Unzip and sideload the APK (`Settings → allow unknown apps` for your browser first).

### Option B — build it yourself
```bash
./gradlew assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```
Or open the repo in **Android Studio** (Ladybug or newer recommended), let it
sync, and press Run.

## Tech

- **Kotlin + Jetpack Compose** (Material 3), single-activity, dark-only instrument UI
- **CameraX 1.4** with **Camera2 interop** — manual controls are applied twice
  for reliability: live via `Camera2CameraControl` (preview updates instantly,
  no rebind) and baked into the still-capture request at bind time so JPEGs
  honor them
- **ResolutionSelector** strategies for exact-size capture + matching preview aspect
- `ImageAnalysis` (YUV) drives histogram + peaking on a background executor
- Rotation-vector sensor → attitude (pitch/roll/azimuth) with EMA smoothing
- Scoped-storage-aware capture saver (MediaStore `IS_PENDING` flow on 10+,
  FileProvider share on 8.x)
- Settings persisted via SharedPreferences

## Project layout
```
app/src/main/java/com/ultracam/app/
├── MainActivity.kt              # activity, edge-to-edge, permission gate
├── camera/
│   ├── CameraController.kt      # the hardware layer (CameraX + Camera2 interop)
│   ├── CameraMath.kt            # stops, kelvin→RGGB, formatting
│   ├── CaptureSaver.kt          # MediaStore / FileProvider save pipeline
│   ├── HistogramAnalyzer.kt     # YUV histogram + focus peaking
│   └── Model.kt                 # LensInfo, ResolutionOption, ManualSettings…
├── sensors/AttitudeSensor.kt    # rotation-vector → pitch/roll/azimuth
├── ui/
│   ├── CameraViewModel.kt       # single source of truth for the UI
│   ├── CameraUiState.kt         # state model
│   ├── theme/Theme.kt           # void palette, prism accents, mono type
│   ├── glass/                   # GlassPanel, prism art, aurora, reticle
│   ├── components/              # shutter, sliders, overlays, bars, sheets
│   └── screens/                 # CameraScreen, OnboardingScreen
└── util/Prefs.kt
```

## Requirements
- Android 8.0+ (API 26); some controls (pixel-pitch info, haptic constants)
  light up extra features on newer devices
- Camera permission — requested on first launch, nothing else
- Photos never leave the device

## Roadmap
- RAW/DNG capture
- Night mode (multi-frame stacking) & HDR fusion
- Zero-shutter-lag burst capture
- Video (manual controls over Media3/Camera2 recording)
- Grid/histogram/level refinements, tablet + landscape layouts

---
*ULTRACAM v0.1.0 — built to get the aesthetic shots the hardware deserves.*
