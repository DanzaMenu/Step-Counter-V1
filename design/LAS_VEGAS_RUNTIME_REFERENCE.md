# Las Vegas Strip Runtime Reference

## Source of Truth

`Las_Vegas_Strip_Mockup.png` is the approved visual target. Its polished 2D mobile-game rendering, saturated neon palette, dense resort detail, night lighting, boulevard perspective, reflections, palms, traffic, fountains, and cyan/magenta searchlights define the production style. The former geometric vector reconstruction is not an acceptable visual substitute.

## Production Assets

| Asset | Dimensions | Purpose |
|---|---:|---|
| `Las_Vegas_Strip_Mockup.png` | 941 × 1672 px | Original approved composition and UI reference |
| `Las_Vegas_Strip_Clean_Background.png` | 941 × 1672 px | Dashboard background with the sample UI removed and scenery reconstructed |
| `Las_Vegas_Strip_Widget_Panorama.png` | 1912 × 823 px | Wide widget composition derived from the approved style |
| `app/src/main/res/drawable-nodpi/vegas_strip_background.png` | 941 × 1672 px | Runtime dashboard asset |
| `app/src/main/res/drawable-nodpi/vegas_strip_widget.png` | 1912 × 823 px | Runtime widget asset |

## Widget Rendering

- The home-screen widget is hosted at approximately 428.67 × 147.33 dp on the verified Samsung launcher, a displayed aspect ratio of approximately 2.91:1.
- `StepCounterWidget` renders a high-resolution bitmap and the launcher scales it into the bound widget surface.
- `VegasScene` uses the wide panorama for landscape/widget surfaces and the clean portrait artwork for dashboard surfaces.
- The widget panorama is sampled at a 2.9:1 source ratio before being drawn into the provider bitmap. This compensates for the launcher's final widget scaling and preserves the intended landmark proportions on-device.
- Rounded clipping remains 28 dp so the scenery and glass overlay share one clean silhouette.

## Live Overlay

- The overlay is real UI, not baked into either runtime background.
- Vegas glass uses a translucent violet gradient from ARGB `(148, 35, 13, 76)` to `(128, 81, 25, 103)`.
- Border color is lavender ARGB `(225, 226, 147, 255)` at 1.2 dp.
- Shoe and active progress colors are cyan `#42E9F5`.
- Inactive progress color is lavender ARGB `(105, 205, 174, 255)`.
- Live steps, goal, miles, walker, calories, and estimated pounds remain supplied by the existing `Steps` and `StepCounterView` logic.

## Dynamic Layers

- The approved artwork remains the base layer without geometric substitutions.
- `VegasScene` adds a low-alpha animated cyan/magenta ambient pulse.
- Existing live rain, storm, snow, fog, and lightning effects render over the artwork.
- The widget foreground service continues refreshing the RemoteViews bitmap at the existing animation cadence.

## Verification Baseline

- Clean build command: `$env:JAVA_HOME = "C:\Android\tools\jdk-17.0.20.1+1"; C:\Android\tools\gradle-8.11.1\bin\gradle.bat clean assembleDebug`
- Verified APK: `app/build/outputs/apk/debug/app-debug.apk`
- Verified device binding: 7 columns × 2 rows.
- Verified result: the real home-screen widget displays the pyramid, castle, Eiffel-style tower, neon balloon, fountain resort, observation wheel, palms, boulevard, and traffic together inside the widget bounds.
