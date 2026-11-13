# Las Vegas Strip Scene Mockup Specification

## Purpose

This mockup defines the target look for the animated Las Vegas Strip scenery: a recognizable, colorful 2D mobile-game boulevard with casino landmarks, traffic, neon signage, and the step-counter widget integrated into the scene.

Reference image: `Las_Vegas_Strip_Mockup.png`

## Target canvas

- Design size: 1080 × 1920 px.
- Aspect ratio: 9:16 portrait.
- Android implementation: render to the available widget/dashboard canvas and scale by `canvasWidth / 1080f`.
- Keep the widget's safe content inset at 48 px left/right and 36 px top/bottom at design size.
- Do not use a photographic ocean, generic skyline, or empty night background.

## Scene composition

### Background sky: y=0–470

- Deep navy-to-violet vertical gradient: `#080D32` at the top to `#24124A` near the horizon.
- Small sparse stars with slow opacity pulsing.
- Four animated light beams behind the buildings: cyan, violet, blue, and magenta.
- Beam opacity: 0.20–0.45; animate over 4–8 seconds with independent phases.

### Landmark skyline: y=230–820

Use fictional landmark-inspired resorts so the scene is recognizable without copying real logos:

- Left pyramid resort: x=0–310, y=275–690; dark glass pyramid with cyan edge lights and a gold marquee reading `DESERT PYRAMID RESORT`.
- Left foreground castle resort: x=0–260, y=480–900; white towers, red/blue roofs, warm windows, marquee reading `ROYAL CASTLE RESORT`.
- Center tower resort: x=315–570, y=190–690; gold lattice observation tower with a purple sign reading `RIVIERA TOWER RESORT`.
- Center-right fountain resort: x=650–1080, y=350–760; cream/gold facade with a large animated fountain and sign `BELLA VISTA FOUNTAINS`.
- Right observation wheel: x=760–1080, y=150–550; neon magenta/cyan wheel with slow rotation.
- Additional layered hotel blocks: y=420–780, warm windows and colored roof lighting.

### Boulevard and Strip activity: y=650–1920

- Main boulevard vanishing point: approximately `(540, 730)`.
- Road width at bottom: x=70–1010.
- Six traffic lanes with white lane markers.
- Sidewalks: x=0–170 and x=910–1080 near the bottom.
- Palm trees every 90–140 px along both sidewalks; trunks wrapped in warm bulbs.
- Animated pedestrians as small silhouettes on sidewalks.
- Cars and taxis moving toward and away from the vanishing point.
- Road reflections beneath headlights and neon signs.
- Side-street signs and marquee panels in cyan, magenta, red, gold, and electric blue.

## Widget panel

### Placement

- Panel bounds: x=50, y=1015, width=980, height=345 at 1080 × 1920.
- Corner radius: 38 px.
- It should sit above the road while leaving enough Strip visible above and below it.
- Panel is translucent so the boulevard lights remain visible through it.

### Panel styling

- Fill: `#24143F` at approximately 88% opacity.
- Outer border: `#E6B7FF` at 70% opacity, 3 px.
- Inner glow: violet/pink, 18 px blur equivalent.
- Optional subtle glass highlight along the top edge: `#FFFFFF` at 15% opacity.

### Widget content coordinates

- Shoe icon: x=105, y=1075, width=92, height=62; cyan-white glow.
- Step text: x=300, baseline y=1130; large bold white text, target `171 / 10,000 (0.1 mi)`.
- Progress bar: x=100, y=1215, width=880, height=28; rounded radius 14.
- Bar unfilled: `#D5B9FF` at 42% opacity.
- Bar filled: `#1EE7FF`; add a soft cyan glow.
- Walker anchor: progress position on the bar, with feet touching the bar's top edge.
- Walker design size: 64 px tall at design size; scale proportionally with the scene.
- Footer text: centered, baseline y=1300; target `6 kcal · 0.00 lbs est.` in white at 36 px.

## Walker animation

The walker is a clear two-legged stick character, not an abstract marker.

- Head: filled circle, 10 px radius.
- Body: 8 px wide, 28 px tall.
- Two arms: mirrored diagonals, 5 px stroke.
- Two legs: mirrored diagonals, feet terminate exactly at the bar's top edge.
- Never draw the legs through the bar.
- Never let the walker overlap the shoe or step-count row.
- Position: `x = barLeft + progress * barWidth`, clamped to the bar endpoints.
- At 171 steps of a 10,000 goal, the walker should be very close to the left edge.
- Walking loop: 2-frame or 4-frame leg swing, 350–500 ms per frame.
- Move the walker smoothly toward its new progress position over 500 ms after a step update.
- Add a small two-frame arm/leg alternation; keep the head and torso stable.

## Animated scene layers

Render in this order:

1. Gradient sky and stars.
2. Distant mountains and low skyline.
3. Landmark buildings and windows.
4. Searchlights and rotating observation wheel.
5. Palm trees and sidewalks.
6. Boulevard, lane markers, traffic, and reflections.
7. Foreground signs, pedestrians, and marquee bulbs.
8. Translucent step-counter widget panel.
9. Widget text, bar, shoe, and walker.

Recommended update cadence:

- Static background: cache and redraw only on theme/size change.
- Marquee/window blinking: 200–500 ms.
- Traffic: 16–30 FPS while active; reduce to 5 FPS when not visible.
- Observation wheel: 30 FPS while visible.
- Walker: 30 FPS during movement, static otherwise.

## Responsive sizing

- Preserve the 1080×1920 coordinate system with a uniform scale.
- Scale all scene elements from the same factor; do not independently stretch the widget panel.
- For short widget heights, reduce decorative scene layers first.
- Keep the widget panel content in a minimum safe height of 345 design px.
- If the widget is shorter than the safe height, reduce text and walker scale together while preserving the bar's bottom relationship.
- The walker feet must always remain on the bar's top surface.

## Implementation notes

- Use a custom Android Canvas renderer for deterministic alignment and animation.
- Keep landmark signs as app-owned fictional text; do not embed real casino logos.
- Separate scene state from step state: `LasVegasSceneState`, `TrafficState`, `MarqueeState`, and `WalkerState`.
- Persist only user settings and selected scene; animation phase can remain in memory.
- Recompute miles, calories, and estimated weight loss from the existing step/persistence state.
- The scene must render identically in the dashboard and widget renderer, with widget-specific clipping only at the final stage.

## Acceptance checklist

- The scene is immediately identifiable as the Las Vegas Strip.
- Multiple casino landmark silhouettes span the horizon.
- Traffic, neon, windows, fountains, palms, and searchlights visibly animate.
- The step panel remains readable over the animated background.
- The walker is large enough to read and has two distinct mirrored legs.
- The walker follows the true progress fraction.
- The walker stands on the bar rather than inside it.
- At very low progress, the walker remains near the left edge and does not overlap the shoe or step text.
