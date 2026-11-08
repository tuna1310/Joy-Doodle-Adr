# PROMPT — Rebuild DoodleStudio into a Neon "Glow Drawing" app (UI + features like the attached video)

## 0. Context
I'm attaching: (1) `DoodleStudio.zip` – my existing Android project, (2) a screen-recording video + 3 screenshots of the reference app ("Paint Joy"-style neon drawing app).
Goal: **redesign the UI and re-implement the core functionality so the app looks and behaves like the video**, reusing as much of my current architecture as makes sense.
Watch the video frame by frame before coding. When the video and this prompt disagree, the video wins.

## 1. Existing project (keep, don't rewrite from scratch)
- Kotlin, Jetpack Compose + Material3, Navigation-Compose, Koin DI, Room, DataStore, Media3, kotlinx.serialization. minSdk 26 / target 35, package `com.doodlestudio.app`.
- Current screens: Splash, Onboarding, Home, Gallery (template picker), Editor, Library, Settings, Result.
- Reusable domain code: `ui/components/FreeDrawCanvas.kt`, `domain/canvas/{FreeDrawManager, StrokeInterpolator, HistoryManager, ActionLogPlayer, ActionLogSerializer}`, `domain/model/{Artwork, ActionLog, EditAction}`, `domain/export/*` (timelapse video export), Room `ArtworkEntity/Dao`, `SoundEffectManager`.
- `BrushTool` already has PEN / MARKER / NEON / ERASER. `ActionLog` already records timestamped strokes → perfect for the "replay" button.
- The old template-coloring flow (RegionFiller, LabelMapBuilder, 40 template PNGs) is **not part of the new design**. Don't delete it yet; just remove it from navigation (keep behind a flag so I can restore it).

## 2. Target screens (from the video/screenshots)

### 2.1 Home screen (pure black background)
- Full black `#000000`, content vertically centered, 2 big items stacked with generous spacing:
  1. **NEW** – ~140dp rounded-square icon (palette + brush on a rainbow watercolor background) with a **neon glowing border** (blue/teal glow), label "New" below written in a **handwritten neon-tube font with rainbow gradient letters + soft glow**.
  2. **GALLERY** – same size icon (spiral notebook with a flower), **magenta/pink neon border**, label "GALLERY" in neon rainbow tube lettering.
- Bottom-right: small circular **"Ad Free"** badge with a neon pink ring (opens a "remove ads" dialog – stub is fine).
- Bottom: horizontal row of 6 small rounded "More apps" tiles. Make these generic placeholders from a config list (don't copy other apps' branding/icons).
- Tap animation: slight scale-down + glow pulse. Optional soft click sound via `SoundEffectManager`.

### 2.2 Drawing screen (New)
- Top: reserved **banner area** (~15% height, denim-blue textured background `#1E5068`) → leave as `BannerAdSlot()` composable, empty by default.
- Canvas: fills the rest, **pure black** by default. Draws with **neon glow strokes** (see §3).
- Top-left: coral circular **Back** button (`#EE6A6A`, white chevron). Top-right: coral circular **Clear/Trash** button → confirm dialog → clears canvas (undoable).
- Bottom toolbar, 5 round 52dp buttons evenly spaced, soft pastel backgrounds:
  1. **Brush** (light-blue bg) – opens a bottom sheet: color palette (rainbow + custom picker), brush size slider, 4 presets (Thin/Medium/Thick/Extra), and brush style (Neon glow / Rainbow auto-cycle / Sparkle).
  2. **Eraser** (light-blue bg) – toggles eraser; size slider.
  3. **Undo** (mint/teal bg, curved arrow) – undo last stroke; long-press = redo (or add a Redo icon if the video shows one).
  4. **Play** (bright blue bg, ▶) – **replays the drawing stroke-by-stroke** as an animation on the canvas using `ActionLogPlayer` (tap again to stop; speed ×1/×2/×4).
  5. **Share/Send** (red bg, paper-plane) – exports the drawing as PNG (and optional timelapse MP4 via existing `domain/export`) and opens the Android share sheet; also saves to the Gallery.
- Auto-save the artwork (thumbnail + ActionLog JSON) on back / app pause.

### 2.3 Gallery screen (light background `#FAFAFA`)
- Top-left coral Back button.
- **3-column grid** of white cards with rounded corners, subtle elevation and an inner thin gray frame, aspect ≈ 3:4.
- First cards = **user's saved artworks** (black thumbnail with the glowing drawing). Tap → reopens in the drawing screen with all strokes restored (continue editing). Long-press → delete / share.
- Following cards = "More apps" promo tiles (icon + colorful title) from the same config list as the Home bottom row.
- Footer text centered at bottom: **"N Artworks"** (live count from Room).
- Empty state: friendly neon "Draw something!" hint.

## 3. Neon brush rendering (the key feature)
Implement in `FreeDrawCanvas` / a new `NeonBrushRenderer`:
- For each stroke, draw 3 passes on the Compose `Canvas` / native `Paint`:
  1. **Outer glow**: strokeWidth × 2.5, same color alpha ≈ 0.25, `BlurMaskFilter(radius = width*1.2, NORMAL)`.
  2. **Inner glow**: strokeWidth × 1.5, alpha ≈ 0.55, smaller blur.
  3. **Core**: strokeWidth × 0.6, color blended 70% toward white, `StrokeCap.Round`, `StrokeJoin.Round`.
- Use `Paint.setLayerType(SOFTWARE)` or draw into an offscreen `Bitmap` per committed stroke (cache) so glow doesn't re-blur every frame; only the in-progress stroke is redrawn live. Must stay ≥ 55 fps on a mid-range phone.
- Smooth paths with quadratic Bézier through midpoints (reuse `StrokeInterpolator`).
- Optional "Rainbow" brush: hue shifts along stroke length.
- Eraser = `BlendMode.Clear` on an offscreen layer (canvas background stays black).
- Everything must be serialised to `EditAction.Stroke` (add `toolType = "NEON" | "RAINBOW" | "ERASER"`) so undo, resume and replay keep working.

## 4. Visual design tokens
| Token | Value |
|---|---|
| Background (Home/Draw) | `#000000` |
| Background (Gallery) | `#FAFAFA` |
| Coral buttons | `#EE6A6A` |
| Banner denim | `#1E5068` |
| Undo button | `#6FE0D0` |
| Play button | `#3DB8F5` |
| Share button | `#EE5060` |
| Brush/Eraser bg | `#DDF1FA` |
| Neon palette | `#FF2D95, #FF9F1C, #FFEE32, #39FF14, #00E5FF, #3D5AFE, #B517FF, #FFFFFF` |
- Font: a handwritten/neon-tube display font (e.g. Google Font **"Pacifico"**, **"Sniglet"** or **"Neonderthaw"**; bundle it in `res/font`). Rainbow gradient via `Brush.linearGradient` + `drawWithCache`, glow via blurred duplicate text behind it.
- Neon border composable: `NeonGlowBox(color, cornerRadius)` using `drawBehind` + `BlurMaskFilter`/`Modifier.blur` fallback on API < 31.
- Portrait only, edge-to-edge, dark status bar on Home/Draw.

## 5. Navigation (update `Screen.kt` / `NavGraph.kt`)
`Splash (short, black + glowing logo, 1s)` → `Home` → `Draw?artworkId=` and `Gallery`.
Remove Onboarding/Library/Settings/Result from the main flow (keep files, unlinked). Gallery item → `Draw?artworkId=<id>`.

## 6. Data
- Keep Room `ArtworkEntity` (id, title, createdAt, updatedAt, thumbnailPath, statePath). `templateId` becomes null / `isBlankCanvas = true` always.
- `GalleryViewModel` exposes `Flow<List<Artwork>>` + count.
- Thumbnail: render canvas at 512px with black background after each save.

## 7. Extra requirements
- Keep Vietnamese + English strings (`values`, `values-vi`).
- Haptic tick on color change, soft pop sound on Undo/Clear (reuse `SoundEffectManager`).
- Ads: only placeholder slots (`BannerAdSlot`, "Ad Free" dialog) – do **not** integrate a real ad SDK.
- Do not copy third-party artwork/brand icons; generate Home icons as vector drawables or Compose drawings.

## 8. Deliverables / how to work
1. First, read the zip and the video, then reply with a short plan (files to change/add/remove).
2. Implement in small commits: (a) theme + NeonGlow components, (b) Home, (c) neon canvas + toolbar, (d) replay + share, (e) Gallery, (f) polish.
3. Update unit tests that break (`FreeDrawManagerTest`, `ActionLogPlayerTest`, …) and add tests for the new `toolType` serialization.
4. Make sure `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass.
5. At the end, list what's finished, what's stubbed, and any difference from the video.
