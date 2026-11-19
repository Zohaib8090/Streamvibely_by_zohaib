# Design system

App name: **Streamvibely**. (The Figma desktop frame still shows the old working name "reson"; replace it in the design.)

Look: Spotify-style layout (dark-first, dense lists, card grids, persistent player) in the
Material 3 Expressive language. Do not copy Spotify's logo, green, icons or name.

## Source of truth

Figma: https://www.figma.com/design/NvHFIi2F6uBJpqpbpE8sZj/spotify-mockup (file key
`NvHFIi2F6uBJpqpbpE8sZj`, page "Page 1", read 2026-11-17).

The file defines **no Figma variables** (variable query returned empty). Tokens must be
extracted from node fills and text styles, not read from variables. Next step: run
`get_design_context` on one phone frame and the desktop frame and record exact values below.

## Screens in the mockup (13 frames)

Phone (412x892):
1. `3:2` Home — filter chips (All/Music/Podcasts), "Back in rotation" 2-column quick grid,
   "Made for your frequency" cards, "Your daily discovery" banner, mini-player, bottom nav
2. `3:6` Search — search field, source chips (All sources/YouTube/Spotify), "Browse your world"
   category tiles
3. `3:10` Your library — tabs (Playlists/Albums/Artists/Downloads), list rows
4. `3:14` Playlist — cover, title, actions, track list
5. `3:18` Now playing — art, title/artist, source/quality chips, progress, transport,
   bottom actions (This device / Equalizer / Queue)
6. `3:22` Play queue — now playing, next from playlist, drag handles
7. `3:26` Lyrics — synced lyrics card
8. `3:30` Equalizer — 10-band sliders, presets, bass/treble knobs, on/off
9. `3:34` Audio lab — output device, equalizer, loudness normalization, crossfade, gapless
10. `3:38` Music sources — YouTube, Spotify, local files, add source
11. `3:42` Settings — appearance, audio & equalizer, music sources, downloads, privacy, about

Wide:
12. `3:46` Tablet workspace (1024x1000) — icon rail, content, now-playing panel
13. `3:51` Desktop workspace (1440x1000) — sidebar with branding, collection and sources,
    content, right now-playing panel with mini equalizer and queue, bottom player bar

## Owner changes to the mockup (see decisions.md D6)

- Desktop right panel: queue replaces the mini equalizer; equalizer opens in that panel.
- Equalizer button in the bottom bar and in the side navigation.
- Mobile Search and Now playing screens to be optimized; the rest is near final.
- Thumbnail limits: 18 max (wide), 9 min (compact), 5 in the suggestions row (see D5).

## Observed style (from the screenshot; exact values still to extract)

- Dark surfaces with a green tint (cards and mini-player use a dark olive-green container).
- Single bright lime/mint-green accent for primary actions, active chips, sliders, play button.
- Large rounded corners on cards, chips (pill), and the main play button (circle/morph).
- Album artwork is photographic, rounded, used as hero content.
- Adaptive layouts already designed: bottom nav (phone), icon rail (tablet), sidebar + right
  panel (desktop). This maps directly to window-size classes.

## Tokens (extracted 2026-11-17)

Full data: `design-system/tokens/tokens.json`. Scanned 1,064 nodes across all 13 frames.

**Key finding:** the mockup is a flat drawing, not a design system. It has no Figma
variables, no paint or text styles, no effects, and no auto-layout. Every value below was read
from raw layers, and the M3 role names are my proposed mapping.

**Font:** Inter only (393 text nodes), weights Regular/Medium/SemiBold/Bold, auto line height,
no letter spacing.

**Core colors (dark theme only):**

| Role (proposed) | Value |
|---|---|
| background | `#0c0e0d` |
| surfaceContainer (chips, tiles) | `#1d2520` |
| mini-player / primaryContainer | `#354638` |
| primary (accent) | `#c1f479` |
| onPrimary | `#14200f` |
| onSurface | `#f5f8f2` |
| onSurfaceVariant | `#a5afa7` |
| outlineVariant (divider) | `#313c32` |

Six category tile colors for Search: `#435e35`, `#7444a0`, `#326e77`, `#b25135`, `#484da0`,
`#9a6034`. Album art is photographic; no gradients were found.

**Type scale in use:** 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 22, 23, 24, 25 px.
Screen titles are Bold 24, section headings Bold 25, track titles SemiBold 14, metadata
Regular 11.

**Radii in use:** 2, 7, 8, 9, 12, 14, 16, 17, 18, 20, 24 px (plus 32 on the phone frames).
Radius 7/8/9 appear to be one artwork radius drawn inconsistently. Pick one.

**Elevation:** none. Depth is tonal (surface color steps), which fits M3.

## Gaps and risks in the tokens

- **No light theme exists.** Decide whether v1 is dark-only.
- **Too many type sizes (14) and radii (11+).** The mockup was drawn by hand, so normalize
  them to an M3-style scale (about 8 type roles, 5 shapes) before building the theme.
- **No spacing data.** Derive a scale from layer positions or adopt a 4dp grid.
- **No motion data.** Define M3 Expressive springs separately; the mockup has none.
- **Fixed color scheme, not dynamic color.** Build the theme from these exact colors. An M3
  seed-color generator would not reproduce them.
- **Contrast (computed, WCAG 2.x):** most pairs pass AA (4.5:1): onSurface on background
  18.1, onSurfaceVariant on background 8.6 and on surfaceContainer 6.9, onPrimary on primary
  13.3, primary on background 15.2. Two fail or sit on the line:
  `#a5afa7` on the mini-player `#354638` is 4.46 (just under AA for small text), and
  lyrics `#8ea57b` on `#33452b` is 3.86 (fails AA for body text; large text needs 3.0).
  Lighten those two before shipping.
- **Fonts:** Inter must be bundled for Android, desktop and web, or text will differ per
  platform.

Flow: tokens JSON -> generated `StreamvibelyTheme` in Kotlin. No hand-copied values.

## Components to build once in commonMain

Filter chip, quick-pick tile, content card, track row, category tile, mini-player,
now-playing, seek slider, EQ vertical slider, EQ knob, toggle row, settings group,
source row, bottom nav, navigation rail, sidebar, now-playing side panel.

## Check before use

Compose Material 3 expressive APIs may be experimental; confirm the version and opt-ins.
