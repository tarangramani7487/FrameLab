# FrameLab — Android handoff (Kotlin · Jetpack Compose)

> **What this is:** the complete design handoff to build FrameLab as a native Android app.
> It covers tokens, components, navigation, every screen and state, the editor, and every exported asset.
> **Source of truth:** the FrameLab redesign canvas (85 artboards). Everything in this package was exported from it.
> **Units:** 1 px in the designs = **1 dp** on Android. Text px = **sp**. All screens are drawn at 390 × 844 dp (portrait phone).
> **Date:** 2026-11-17.

---

## 0. How to use this package (read first)

```
framelab-android-handoff/
├── HANDOFF.md                ← this file (spec)
├── CLAUDE.md                 ← short rules for Claude Code; copy to your repo root
├── screens/<page>/*.png      ← 2× screenshot of every screen and state (85)
├── design-source/*.dc.html   ← the original design files (HTML/CSS) — exact numbers live here
├── res/                      ← drop-in Android resources → copy into app/src/main/res/
│   ├── drawable/ic_*.xml          76 vector icons (VectorDrawable)
│   ├── drawable-xxhdpi/*.png      illustrations + stickers @3x
│   ├── drawable-xxxhdpi/*.png     same @4x
│   ├── mipmap-*/ , mipmap-anydpi-v26/   adaptive launcher icon
│   ├── font/*.ttf                 Bricolage Grotesque, Inter, DM Mono (static weights)
│   └── values/colors.xml          every colour token
├── icons/svg/*.svg + icons/icons_sheet.png   same icons as SVG + a contact sheet
├── tokens/design-tokens.json  colours, type scale, spacing, radius, elevation, motion
├── tokens/ui-copy.json       every on-screen string + accessibility label, per screen (exact wording)
├── store/play_store_icon_512.png
└── fonts-licences/            OFL licences (ship them in the app's licences screen)
```

**Workflow for Claude Code**

1. Copy `res/` into `app/src/main/res/` (merge). Copy `CLAUDE.md` to the repo root and keep this package in `/design`.
2. Build the theme from §3 and the shared components from §5 first, then screens page by page (§7), then the editor (§8).
3. For every screen, open its screenshot in `screens/` and its `design-source/*.dc.html` for exact sizes. Inline `style="…"` values are the spec: `left/top/width/height` are dp, `font: 700 15px/20px Inter` = weight 700, 15 sp, 20 sp line height.
4. Take all text from `tokens/ui-copy.json` (strings in reading order, plus `contentDescriptions` for icon buttons) and put it in `strings.xml`. Sample data (names, counts, dates) is marked in §10.
5. Use only the icons in `res/drawable/ic_*.xml`, tinted at the use site. Never use emoji or Unicode glyphs as icons, and don't add an icon library.
6. Template slides and card thumbnails are **drawn by the template renderer** from layout data. The photos in the screenshots are placeholders — never ship them as images. The images to ship are listed in §4.2–4.4.

**Suggested prompts for Claude Code** (one per session, review between steps):

1. “Read CLAUDE.md and design/HANDOFF.md. Create the Android project (Kotlin, Compose, Material 3), copy design/res into the app, and build the FrameLab theme from §3. Show me a preview screen with every colour and text style.”
2. “Build the shared components in §5 with previews that match screens/foundations and screens/home/H7-Cards.png.”
3. “Build the bottom tabs and Home (H1–H7, N24 offline) from §7.1 with fake data. Compare each preview with its screenshot and fix differences.”
4. Repeat for Explore and Create (rest of §7.1) → My Designs and Profile (§7.4) → Onboarding (§7.5) → Editor (§7.2 + §8) → Export (§7.3).
5. “Connect Supabase read-only (§8.3) with the built-in fallback, then drafts (Room) and settings (DataStore).”

**Suggested stack (recommendations, not requirements):** Kotlin, Jetpack Compose + Material 3 with a fully custom colour/typography theme, Navigation Compose, Coil, the Android Photo Picker (`PickMultipleVisualMedia`), MediaStore for saving images, Supabase Kotlin client (read-only templates), Room for drafts, DataStore for settings, edge-to-edge with `WindowInsets` padding. `minSdk 26`.

---

## 1. Product in one paragraph

FrameLab makes **Instagram carousel posts**. You pick a **template** (several 4:5 slides with photo spots, paper, frames, stickers and text) or a **frame** (one decorative frame), fill the photo spots from your gallery, adjust each photo, and **save every slide as a full-size image** to the gallery, numbered and in order. Signature feature: **seamless carousels** — photos and frames cross from one slide into the next, so the post reads as one long picture when swiped.

Look: **"Darkroom & Paper"** — warm paper for browsing (Home, Explore, My Designs, Profile), a dark photo-first **Editor**, one coral accent.

---

## 2. App structure

- **Bottom tab bar (5):** Home · Explore · **Create** (opens a sheet) · My Designs · Profile. Hidden in the Editor, onboarding and full-screen flows.
- **Editor** opens full-screen over the tabs (slide in from the right 260 ms; back slides out 220 ms).
- **Sheets** (bottom sheets) and **dialogs** sit over a scrim. Light screens use light sheets; the Editor uses dark sheets.
- **Android back:** closes the open sheet/dialog → deselects the selected photo → leaves the Editor (asks "Save as a draft?" if there are unsaved changes).
- **Insets:** draw edge-to-edge. Top 48 dp in the mockups is the status-bar area — keep it empty. Bottom bars (tab bar, editor dock, primary buttons) always sit **above the navigation bar inset** (gesture bar or 3-button bar).

---

## 3. Design tokens

All values are in `tokens/design-tokens.json`; colours are also in `res/values/colors.xml` with the same names.

### 3.1 Colour — light side

| Token | Hex | Use |
|---|---|---|
| `fl_ink` | #16130F | Primary text, dark cards, dark buttons |
| `fl_paper` | #FAF7F2 | App background |
| `fl_card` | #FFFFFF | Cards, fields, tab bar, light sheets |
| `fl_muted` | #6F685E | Secondary text (5.1:1 on paper). Replaces the old #8A8378 (3.5:1, failed) |
| `fl_line` | #ECE6DC | Borders, dividers |
| `fl_line_strong` | #D9D2C7 | Sheet handles, outlined buttons on white |
| `fl_sunk` | #F1ECE4 | Segmented tracks, skeletons, secondary buttons |
| `fl_track` | #E4DED4 | Progress tracks |
| `fl_accent` | #FF5A36 | Brand accent — **fills only**, with **ink text on top** (5.97:1). White on accent fails (3.1:1) |
| `fl_accent_ink` | #B33A17 | Accent-coloured text and active tab label on light (5.6:1) |
| `fl_accent_soft` | #FFE6DE | Badges, icon wells (text on it: `fl_accent_soft_text` #A8361A or `fl_accent_ink`) |
| `fl_danger` | #C8372D | Destructive (white text on it 5.2:1) |
| `fl_control_off` | #8F877B | Switch off state, inactive page dots on light |
| `fl_disabled_text` | #A39C91 | Disabled labels on light |

### 3.2 Colour — Editor (dark)

| Token | Hex | Use |
|---|---|---|
| `fl_ed_bg` | #0B0B0C | Editor background |
| `fl_ed_surface` | #1C1C1E | Dock, dark sheets, dialogs |
| `fl_ed_raised` | #2C2C2E | Chips, tiles |
| `fl_ed_text` | #FFFFFF | Text |
| `fl_ed_muted` | #8E8E93 | Secondary text on bg/surface (**not** on raised — use `fl_ed_muted_raised` #A1A1A6) |
| `fl_ed_disabled` | #5A5A5E | Disabled tools |
| `fl_ed_dots` | #6E6E73 | Inactive page dots, dashed outlines |
| `fl_ed_handle` | #48484A | Sheet handles, input borders |
| `fl_ed_danger` | #FF6961 | Destructive text on dark |
| `fl_ed_active_tint` | #FF5A36 @ 10% | Background of the active dock tool (accent icon + label on it = 4.9:1) |

Scrims: `fl_scrim_light` ink 45% (Create sheet), `fl_scrim` ink 50% (light dialogs/sheets), `fl_scrim_editor` black 60%. Editor neighbour-slide peek: `fl_peek_dim` #0B0B0C 60%.
Editor background swatches, in order: `swatch_white #FFFFFF · paper #FAF7F2 · cream #F3EDE3 · blush #FFE8EC · mist #EAF1F4 · sage #E7EEDF · ink #16130F · navy #0F1E2E`.
Template palette (renderer; `tpl_*` in colors.xml): kraft #EFE6D6, paper #FBF6EC, lab print #F4ECDC, ink #2B2621, caption #4A4238, mono label #7A6E60, date stamp #FF8A3D (+ 6 dp glow), tape #ECD6A3 @ 90%, ruled lines #786046 @ 15% every 18 dp, margin line #D65C50 @ 35%. Pastel placeholders for empty spots in thumbnails: #F3CFC3 #CDE7EA #E3E0F3 #FDE7C4 #D7EBDD #F6D3DC.

### 3.3 Typography

Fonts (in `res/font/`): **Bricolage Grotesque** (display: bold 700, extrabold 800), **Inter** (UI: 400/500/600/700), **DM Mono** (labels: 400/500). Template text uses the system serif italic (Noto Serif on Android).
The bundled TTFs are Latin-subset static builds. For other scripts, download the full families from Google Fonts (all three are OFL).

| Style | Font | Size/line (sp) | Tracking (sp) | Used for |
|---|---|---|---|---|
| wordmark | Bricolage 800 | 28/30 (splash 46/50) | −0.8 (−1.6) | “FrameLab.” — the dot is `fl_accent` |
| display | Bricolage 800 | 36/40 | −1.1 | Onboarding titles |
| screen_title | Bricolage 800 | 30/34 | −0.8 | Explore, My Designs, Profile, Success, Sign in |
| sheet_title | Bricolage 800 | 24/28 (preview 26/30) | −0.5 | Light sheets |
| title | Bricolage 700 | 22/28 | −0.3 | Empty states, dark sheet titles |
| dialog_title | Bricolage 700 | 20/26 | 0 | Dialogs |
| section | Bricolage 700 | 19/24 | −0.3 | Home row titles |
| nav_title | Inter 700 | 17/22 | 0 | Pushed-screen top bars |
| body_l | Inter 400 | 16/24 | 0 | Onboarding body |
| body | Inter 400 | 15/22 | 0 | Body, list rows |
| body_s | Inter 400 | 14/20 | 0 | Dialog and sheet body |
| secondary | Inter 400 | 13/18 | 0 | Meta text, hints |
| caption | Inter 400 | 12/16 | 0 | Card info line, helper text |
| label | Inter 600 | 11/14 | 0 | Tab bar, dock, tiles |
| button_l | Inter 700 | 16 | 0 | 52 dp primary buttons |
| button | Inter 600 (700 on accent) | 15 | 0 | 48 dp buttons |
| chip | Inter 600 | 13 (Home tabs 14) | 0 | Chips, segmented controls |
| tag | Inter 700 caps | 10/18 | 0.6 | NEW tag |
| mono_label | DM Mono 500 caps | 11/16 | 1.0 | Section labels (“COMMON QUESTIONS”), meta |
| mono_small | DM Mono 500 caps | 10/14 | 1.0 | Kickers (“NEW · FRAMELAB ORIGINALS”), step labels |

### 3.4 Spacing, shape, elevation, motion

- **Spacing:** 4, 6, 8, 10, 12, 14, 16, 20, 24, 32. Screen side margin **20 dp** (light screens), **24 dp** (onboarding, sign-in, forms), **12–16 dp** in the editor.
- **Radius:** thumbnails 4/10, tags 6, cards and fields **14**, list cards 16, grouped settings cards **18**, hero card and dock **22**, dialogs and dark sheets **24**, light sheets **28** (top corners), pills fully rounded.
- **Elevation (shadows):** card `0 1 2` ink 10% · popover `0 12 32` ink 22% + hairline · dialog `0 20 50` ink 30% · dock `0 8 24` black 35% · action pill `0 8 20` black 40% · toast `0 10 28` ink 30% (light) / `0 12 30` black 50% (editor).
- **Motion:** Editor push 260 ms / pop 220 ms; sheets ≈ 280 ms ease-out; toast stays 4 s; card press = scale 0.97 + alpha 0.82 (≈120 ms).
- **Sizes:** min touch target **48 dp** (handles and small dots get invisible 48 dp hit areas); primary button height 52; standard button 48; chips 40 (small 36); icons 24 (20–22 inside buttons, 16–18 inside chips); tab bar 64 + inset; editor top bar 56; dock 64.

### 3.5 Contrast rules (do not break)

Ink text on `fl_accent` — never white. Accent-coloured *text* on light uses `fl_accent_ink`. On the dark editor, accent text only on `fl_ed_bg`/`fl_ed_surface`/active tint, never on `fl_ed_raised`. Toasts use an inverse surface: ink toast with accent action on light screens; white toast with `fl_accent_ink` action in the editor.

---

## 4. Assets

### 4.1 Icons — `res/drawable/ic_*.xml` (also `icons/svg/`, preview `icons/icons_sheet.png`)

One stroke set: 24 dp grid, 1.75 stroke, round caps and joins, black — **tint at the use site** (`Icon(painterResource(R.drawable.ic_back), contentDescription, tint = …)`). `ic_style` and `ic_favourite_filled` contain filled shapes.

| Area | Icons |
|---|---|
| Navigation & common | `ic_back` `ic_chevron_right` `ic_chevron_down` `ic_chevron_up` `ic_arrow_right` `ic_arrow_left` `ic_close` `ic_check` `ic_add` `ic_search` `ic_filters` `ic_sort` `ic_more` `ic_external` |
| Tab bar | `ic_home` `ic_explore` `ic_add` (Create, inside the accent pill) `ic_designs` `ic_profile` |
| Library, sharing, account | `ic_favourite` `ic_favourite_filled` `ic_share` `ic_send` (Share to Instagram) `ic_download` (save) `ic_link` `ic_edit` `ic_select` `ic_templates` `ic_image` `ic_photos_add` `ic_camera` `ic_email` `ic_settings` `ic_help` `ic_info` `ic_alert` `ic_error` `ic_offline` `ic_pro` `ic_star` |
| Editor top bar | `ic_back` `ic_undo` `ic_redo` `ic_preview` |
| Dock — main | `ic_add` (Photos, in accent pill) `ic_shuffle` `ic_color` `ic_order` `ic_clear` |
| Dock — photo selected | `ic_frame` `ic_position` `ic_style` `ic_duplicate` `ic_delete` + `ic_close` (deselect) |
| Photo action pill / rotate | `ic_delete` `ic_replace` `ic_lock` `ic_unlock` `ic_rotate` |
| Frame panel | `ic_shape_original` `ic_shape_square` `ic_shape_rounded` `ic_shape_circle` `ic_shape_arch` |
| Position panel | `ic_adjust_inside` `ic_reset_size` `ic_zoom_out` `ic_zoom_in` `ic_rotate` (Turn photo) `ic_center` |
| Style panel | `ic_fill` `ic_fit` `ic_flip` |
| Colour panel | `ic_reset_colour` (template colour swatch) |
| Text sheet | `ic_text` `ic_align_left` `ic_align_center` `ic_align_right` |
| Stickers & slides | `ic_sticker` `ic_slides` `ic_doodle_arrow` |

### 4.2 Illustrations — `res/drawable-xxhdpi` (@3x) and `res/drawable-xxxhdpi` (@4x), transparent PNG

Show them at the dp size below with `contentScale = Fit`. They contain sample photos, which is intended.

| File | Size (dp) | Placed at (x, y dp) | Screen (screenshot) |
|---|---|---|---|
| `img_onboarding_flow` | 390 × 270 | 0, 160 | Onboarding 1 “Posts that flow” (`onboarding/N2-Flow`) |
| `img_onboarding_add` | 300 × 370 | 45, 80 | Onboarding 2 “Tap + to add photos” (`onboarding/N2-Add`) |
| `img_onboarding_save` | 350 × 300 | 20, 100 | Onboarding 3 “Save & post in order” (`onboarding/N2-Save`) |
| `img_photo_access` | 250 × 250 | 70, 88 | Photo-access primer (`onboarding/N2-Primer`) |
| `img_photo_access_off` | 270 × 264 | 70, 96 | “Photo access is off” (`export/D2-Permission`) |
| `img_empty_drafts` | 240 × 178 | 75, 214 | My Designs › Drafts empty (`library/N8-DraftsEmpty`) |
| `img_empty_saved` | 240 × 178 | 75, 214 | My Designs › Saved empty (`library/N9-SavedEmpty`) |
| `img_empty_favourites` | 240 × 178 | 75, 214 | My Designs › Favourites empty (`library/N10-FavEmpty`) |
| `img_empty_search` | 180 × 174 | 105, 250 | Search › no results (`home/H6-NoResults`) |
| `img_email_sent` | 220 × 208 | 85, 130 | Sign in › Check your inbox (`library/N15-Inbox`) |
| `img_pro_hero` | 330 × 190 | 30, 72 | Pro paywall header (`library/N17-Paywall`) |
| `img_pro_welcome_hero` | 330 × 204 | 30, 96 | “You're Pro” on ink (`library/N17-Welcome`) |
| `img_home_hero_film_roll` | 164 × 132 | 206, 266 (inside the hero card: right edge flush, top 44) | Home hero card (`home/Main`); ink background baked in |
| `img_wordmark` / `img_wordmark_light` | 236 × 60 | — | Optional — prefer live text in Bricolage 800 |

Positions are screen coordinates in the 390 × 844 dp mockups (status area included). On devices, keep the same offsets from the top of the content area and centre horizontally on wider screens.

### 4.3 Stickers — `res/drawable-xx(x)hdpi/img_sticker_*.png`, 80 × 80 dp tiles (transparent)

`tape_masking` · `tape_pink` · `tape_gingham` · `date_stamp` · `postmark` · `postage_stamp` · `star` · `arrow` · `swipe_chip` · `heart` · `label` · `ticket`. They are the Stickers-panel previews and the default sticker art. Star, heart and arrow also exist as tintable vectors (`ic_star`, `ic_favourite`, `ic_doodle_arrow`).

### 4.4 Launcher icon

`res/mipmap-anydpi-v26/ic_launcher.xml` (+ `_round`) = background `@color/ic_launcher_background` (#16130F) + foreground `ic_launcher_foreground` (white **F** with accent dot, inside the 72 dp safe zone, mdpi → xxxhdpi) + `ic_launcher_monochrome` for themed icons. Play Store icon: `store/play_store_icon_512.png`. This icon is a proposal — swap it if you have a brand mark.

### 4.5 Not images

Template thumbnails, editor slides, the template preview, saved-post viewers and the contact sheet are **rendered from template data** (§8). Avatars use initials (“SR”) on `fl_accent_soft`. Skeletons are shapes.

---

## 5. Components

Each spec gives the default size in dp. Screens reuse these; build them once. Reference boards: `screens/foundations/` (Foundations = tokens and type, TabBar, Card, Dock, EditorTop, Ed = editor layout, Diary = the Torn Diary template in every fill state).

**5.1 Tab bar** (`design-source/TabBar.dc.html`) — white, 1 dp top border `fl_line`, height 64 + navigation inset. Five equal items: 24 dp icon over a label (`label` style). Active: icon `fl_accent`, label `fl_accent_ink` 700. Inactive: `fl_muted` 500. **Create** = 52 × 32 accent pill with ink `ic_add` (stroke 2.3), label ink 600; it opens the Create sheet and is never "active".

**5.2 Top bars (light)** — height 48 at y 52 (below the status area). Back = 44 dp round button with 26 dp `ic_back`. Title `nav_title` centred. Text actions (“Cancel”, “Save”, “Select all”) are 44 dp tall; “Save” uses `fl_accent_ink` 700, disabled `fl_disabled_text`. Big titled screens (Explore, My Designs, Profile) use `screen_title` at x 20, y 54 instead.

**5.3 Search field** — 350 × 48, radius 14, white, 1 dp `fl_line` border, 20 dp `ic_search`, placeholder `fl_muted` 15 sp. Focused: 2 dp ink border, value 15/500, round 36 dp clear button (`fl_sunk`, `ic_close` 16).

**5.4 Segmented control** — track `fl_sunk`, radius 13 (12 small), padding 4, height 42 (40 for sort). Selected segment white, radius 10, shadow `0 1 3` ink 12%, ink text 600; others `fl_muted`. In search, segment labels carry counts (“Templates · 18”).

**5.5 Chips** — height 40 (36 small, 32 suggestions), radius = height/2, 13 sp 600.
Default: white + 1 dp `fl_line`. Selected/active filter: ink fill, white text (“Slides · 5–6” + `ic_chevron_down`). Filters button: `ic_filters` + count badge (18 dp ink circle, white 11 sp). Removable chip: label + `ic_close` 16. Dark chips (editor panels): `fl_ed_raised` + white; selected white + ink; disabled `fl_ed_surface` + `fl_ed_disabled`.

**5.6 Buttons** — Primary: `fl_accent` fill, ink text, 52 dp (`button_l`) or 48 dp. Dark: ink fill, white. Secondary: `fl_sunk` (light) / `fl_ed_raised` (dark). Outlined: white + 1 dp `fl_line`. Text button: no fill, underline offset 3 for “Back to editor”, “Continue as guest”. Danger: `fl_danger` fill + white (light), or `fl_ed_danger` text on dark. Disabled: `fl_sunk` + `fl_disabled_text`.

**5.7 Switch** — 48 × 28. On: `fl_accent` track, 22 dp white knob on the right. Off: `fl_sunk` track with 1.5 dp `fl_control_off` border and 19 dp `fl_control_off` knob on the left. Use `role = Switch` semantics.

**5.8 Template card** (`design-source/Card.dc.html`, variants in `screens/home/H7-Cards.png`) — thumbnail 128 × 160 (rows) or 169 × 211 (2-column grids), radius 14, `fl_sunk` while loading, shadow `card`. Under it: title 14/18 600 (1 line, ellipsis) + info line 12/16 `fl_muted` (“6 slides · 12 photos”). Badges sit outside the scaled thumbnail art:
slide count top-right (22 dp pill, `fl_badge_scrim` = ink 74%, white 11 sp + 13 dp `ic_slides`) when slides > 1 · **NEW** bottom-left (accent, ink `tag` text) for FrameLab-made · Pro top-left (26 dp ink circle, accent `ic_pro`) · favourite top-left (28 dp white circle, `ic_favourite_filled` accent) · pressed = scale 0.97 + alpha 0.82. Bare variant (thumbnail only) is used for drafts, saved posts and the paywall.

**5.9 Row header (Home)** — 24 dp tall at x 20: title `section`, optional NEW tag (accentSoft), then right-aligned link “See all” + count (`fl_muted` 500) + 16 dp `ic_chevron_right`. Card rows scroll horizontally: padding 12/20, gap 12.

**5.10 Grouped list (settings style)** — `mono_label` caption above (x 4), white card radius 18, 1 dp `fl_line`, rows 52–64 dp with 1 dp `fl_sunk` dividers, 20 dp leading icon, 18 dp `ic_chevron_right` in `fl_muted`. Destructive rows use `fl_danger` text.

**5.11 Empty state** — centred column 326 wide: illustration (§4.2), title `title` (22/28), body `body_s` `fl_muted`, primary button (“Browse templates”, 48 dp, accent). Position: illustration at y 214, centred, on tab screens.

**5.12 Sheets** — Light: white, top radius 28, 36 × 5 handle `fl_line_strong` 10 dp from the top, side padding 20–24, scrim `fl_scrim`. Dark (editor): `fl_ed_surface`, top radius 24, handle `fl_ed_handle`, scrim `fl_scrim_editor`. Content stays above the navigation inset (≥ 32 dp bottom space in the mockups).

**5.13 Dialogs** — 318–334 wide, radius 24, padding 22–24 top and 20 on the sides and bottom, centred text, `dialog_title` + `body_s`, two 48 dp buttons side by side (Cancel = secondary, confirm = primary/danger). Light dialogs are white; editor dialogs `fl_ed_surface`.

**5.14 Popover menu** — 184 wide, radius 16, white, padding 6, items 44 dp with 18 dp icon + 15 sp 500, divider before destructive items, destructive in `fl_danger`. Anchored under the ⋯ button, which shows a `fl_track` circle while open.

**5.15 Toasts and banners** — 52–56 dp, radius 16, padding 16 left / 6 right. Light screens: x 12, 366 wide, bottom edge 12 dp above the tab bar (y 688). Editor: x 16, 358 wide, y 626 — above the hint line and panels, never over the dock. Icon 20, text 13–14 sp (bold lead-in + plain), optional text action 44 dp tall. Light screens: ink toast, accent action. Editor: white toast, `fl_accent_ink` action. Offline banner = persistent ink toast with **Retry**. Catalogue: `screens/export/N24-Toasts.png`.

**5.16 Page dots** — 6 dp dots, active = 18 × 6 pill. Light: active ink, inactive `fl_control_off`. Dark: active white, inactive `fl_ed_dots`. Each dot is tappable (hit area ≥ 28 × 28 inside a 48 dp row).

**5.17 Progress & skeletons** — Progress bar 6 dp (4 dp in lists), radius 3, track `fl_sunk`/`fl_ed_raised`/`fl_track`, fill accent (ink when complete). Skeletons: `fl_sunk` blocks with a soft diagonal shimmer (#F8F4EE highlight), same geometry as the real content.

**5.18 Editor top bar** (`EditorTop.dc.html`) — 56 dp on `fl_ed_bg`: back (26 dp) · flex · undo · redo (disabled `fl_ed_disabled`) · preview (`ic_preview`) · **Save** pill (36 dp, accent, ink 15/700). While saving: 16 dp spinner + “Saving”. Loading/error variant shows back only.

**5.19 Dock** (`Dock.dc.html`) — floating `fl_ed_surface` bar, 366 × 64, radius 22, 1 dp white 7% border, shadow `dock`, at x 12, y 750–814 (30 dp above the screen bottom; on devices, keep it 30 dp above the navigation-bar inset). Five equal 56 dp buttons: 22 dp icon over `label`. States: normal white · active `fl_accent` icon + label on `fl_ed_active_tint` · disabled `fl_ed_disabled`.
Main mode: **Photos** (accent 32 × 24 pill with ink `ic_add`) · Shuffle (disabled with < 2 photos) · Color · **Order** · Clear (disabled with 0 photos).
Selected-photo mode: Frame · Position (disabled when locked) · Style · Duplicate · Delete (disabled when locked), plus a 36 dp round deselect button (`fl_ed_raised`, `ic_close`) overlapping the dock's top-right corner.

**5.20 Editor panels** — replace the hint line, in the band between the page dots and the dock bar (y 750):
chip row y 688–728 (40 dp chips, gap 8, horizontal scroll, 12 dp side padding) · tile row x 19, y 670–728 (5 × 64 × 58 tiles, gap 8, radius 14, icon over label; selected = white tile + ink) · swatch row: `mono_small` caption at x 16, y 660, then 36 dp circles in a 44 dp row at y 690 (padding 16, gap 8; selected = 2 dp `fl_ed_bg` gap + 2 dp accent ring; dark swatches get a 1 dp white 30% border) · photo-order strip (§8.6).

**5.21 Selection overlay** — follows the photo's rotation. Frame 1.5 dp white + 1 dp black 25% outer line. Corner handles 14 dp white dots; side handles 22 × 6 white pills, shown only when that side ≥ 110 dp. Hit areas 48 dp. **Action pill** 150 × 48 white, radius 24, three 44 dp icon buttons (delete · replace · lock), centred 10 dp above the photo's bounding box. **Rotate button** 40 dp white circle, `ic_rotate`, 12 dp below the bounding box. Neither may cover the photo.

**5.22 Number badge** — 20 dp min, radius 6, ink, white 11/700. Used for photo order and export order (DM Mono “01” variant).

**5.23 Coach mark** — scrim black 74% with a 72 dp circular cut-out ringed by 2 dp accent; white card (radius 20, padding 18) with a 14 dp arrow pointing at the target, `mono_small` “TIP 1 OF 3”, title 19/24, body, “Skip tips” text button + “Next” ink button.

---

## 6. Navigation map & flows

```
Splash ─(first launch)→ Onboarding 1 → 2 → 3 → Photo-access primer → OS prompt → Home
       └(returning)───→ Home

Tabs: Home · Explore · [Create sheet] · My Designs · Profile
Home ─ card / hero CTA ─→ Template preview sheet ─ Use template → Editor
     ─ See all ─→ Category grid ─ card → Template preview
     ─ search field → Search results / No results
Explore ─ chips → filter sheets (per group) ─ sort → Trending | New | Most used ─ card → Template preview
Create sheet → Continue last draft (Editor) | Start from a template (Explore) | Blank carousel (soon) | Add a frame (Home › Frames)

Editor ─ preview (eye) → Preview mode
       ─ tap text → Edit text sheet │ Stickers sheet │ Slides sheet
       ─ Save → [empty spots?] Pre-check sheet → Progress sheet → Success screen | Error sheet
                [no photo access] → Photo access is off
       ─ back with unsaved changes → “Save as a draft?”
Success → Share to Instagram → “Post in this order” sheet → Instagram │ Share… (system) │ Back to editor │ Done

My Designs: Drafts ─ row → Editor ─ ⋯ → Rename | Duplicate | Select | Delete
            Saved ─ card → Saved post ─ Share to Instagram → Post in order │ Delete → confirm
            Favourites ─ card → Template preview
Profile ─ Edit profile → photo sheet / handle error
        ─ Settings → Sign out (dialog) · Delete account · Licences · Terms · Privacy
        ─ Help & feedback → Send feedback │ Rate FrameLab (sheet) │ Share the app (sheet) │ About
        ─ FrameLab Pro → Paywall → Welcome to Pro
        ─ Guest: Sign in → Apple | Google | Email → Check your inbox
```

---

## 7. Screens

Each entry: **ID · title** — screenshot, then what matters for building it. Open the design file for exact numbers. Appendix A lists every file.

### 7.1 Home & Explore (`screens/home/`)

- **H1 · Home — Templates** (`Main.png`). Header at y 48–98: wordmark + subtitle (12/17 `fl_muted`, one line) left; count pill right (104 × 40, white, “126 designs”, number in Bricolage 700 17). Search field (y 108). Segmented Templates | Frames (y 166, 42 tall). **Hero card** y 222, 350 × 216, ink, radius 22: kicker `mono_small` accent “NEW · FRAMELAB ORIGINALS”, title 24/26 Bricolage 700 white “One photo, three slides.”, body 13/18 white 74%, CTA 44 dp accent pill “Try Film Roll →”, art `img_home_hero_film_roll` on the right. Then rows (§5.9): “Instagram Trending” (NEW) and “Torn Paper Stories”… in Supabase `sort_order`. Tab bar, Home active.
- **H2 · Loading** — header, search, tabs and hero show immediately (built-in). Count pill and rows are skeletons (row title 150 × 16, cards 128 × 160 + two caption bars).
- **H3 · Couldn't load** — rows from built-in templates (“FrameLab Originals · BUILT-IN”), then an error card: 40 dp `fl_accent_soft` well with `ic_offline`, title 16/700 “Couldn't load more templates”, body, `mono` error line, dark “Try again” button with `ic_rotate`.
- **H4 · Frames tab** — same header (“Search frames”), Frames selected, no hero. Rows of frame cards (single frame, info “Frame · 1 photo”). Frames come from the frame API; thumbnails are remote images.
- **H5 · Search — results** — focused field with clear button; segmented shows counts; “ALSO TRY” suggestion chips; rows filtered live (a row stays whole if its title matches — highlight the match with `fl_accent_soft`; otherwise only matching cards stay, with “1 match” on the right). Hero hidden. Keyboard closes on scroll.
- **H6 · Search — no results** — `img_empty_search`, title “Nothing matches “bowling night””, body, suggestion chips, dark “Clear search”.
- **N24 · Offline banner** — Home plus the persistent ink banner above the tab bar: `ic_offline`, “**You're offline.** Showing built-in templates.”, accent “Retry”.
- **H7 · Card variants** — reference sheet for §5.8.
- **N3 · Explore** — title “Explore”, search (“Search templates, styles, topics”), filter chip row (Filters + count, active groups as ink chips with values, inactive groups outlined: Slides · Photos · Ratio · Style · Topic), meta row “24 templates” + sort button (`ic_sort` “Trending”), 2-column grid of 169 dp cards (gap 16 × 12). Filter groups: Slides 1 / 3 / 5–6 · Photos ≤5 / 6–10 / 10+ · Ratio 1:1 / 4:5 / 9:16 · Style Torn paper / Polaroid / Film / Stamp / Minimal / Dark · Topic Travel / Love / Birthday / Food / Pets / Fitness. Sort: Trending · New · Most used.
- **N4 · Explore — no results** — 76 dp `fl_sunk` circle with `ic_filters`, “No templates match all 4 filters”, removable chips for each active filter, dark “Clear all filters”.
- **N4 · Explore — loading** — skeleton grid.
- **N5 · Category — See all** — top bar with back, title + “40 templates · …” subtitle, filter button; Trending | New | Most used segmented; 2-column grid; tab bar keeps the origin tab active.
- **N6 · Create sheet** — over Home with `fl_scrim_light`; light sheet from y 404: title “Create” + close; “Continue last draft” card (draft thumbnail 48 × 60, “Torn Diary · 5 of 12 photos · 2 h ago”); rows with 44 dp icon wells: Start from a template (`ic_templates`), Blank carousel (disabled, **SOON** tag), Add a frame (`ic_frame`).
- **N7 · Template preview** — sheet from y 52 (almost full height): title 26/30, `mono_label` “6 SLIDES · 12 PHOTOS · 4:5”, category + style chips, swipeable slide preview (280 dp wide, next slide peeking and washed white 55%), dots, “ALL 6 SLIDES, JOINED” seamless strip (58 dp slides with dashed slide edges), actions: favourite (52 dp outlined circle), share link, **Use template** (accent, fills the rest). “Similar templates” row below (108 dp cards).

### 7.2 Editor (`screens/editor/`) — dark

Shared layout (`Ed.dc.html`): top bar y 48 · slide area: current slide 342 × 428 at x 24, y 168, with **24 dp of the next slide peeking** on the right under `fl_peek_dim` · page dots row y 608 (28 tall) · hint line y 704 (13/18 `fl_ed_muted`, counts in white 600) · panels between dots and dock (§5.20) · dock bar y 750.

- **E1 · Opening** — back only; slide placeholder with shimmer; accent ring spinner; “Opening Torn Diary” / “Getting 6 slides ready”; inactive dots.
- **E2 · Couldn't open** — 64 dp `fl_ed_surface` circle with accent `ic_alert`, title, body, `mono` error, **Try again** (accent) and **Go back** (raised).
- **E3 · Empty template (0/12)** — empty spots: diagonal hatch (ink 5% / 12%, 7 dp stripes) + 1.5 dp dashed ink 38% outline + 30–32 dp ink “+” button. **A spot that spans two slides gets its own + on each slide**, and on the first slide a 44 dp + with a white label pill “Background · slides 1–2”. Undo/redo disabled, Shuffle and Clear disabled. Hint “0/12 photos · tap + to add photos”.
- **E4 · Partly filled** — hint “5/12 photos · tap a photo to edit it”.
- **E5 · Background colour** — dock Color active; caption `mono_small` “BACKGROUND · SHOWS AROUND PAPER AND PHOTOS”; swatch row (§5.20): template colour first (with `ic_reset_colour`), then the 8 swatches; the slide background updates live (screenshot shows Ink).
- **E6 · Photo order** (was “Guides”) — dock Order active; number badges on every spot; strip of 24 dp tiles grouped by slide (S1…S6 under each group), filled tiles show the photo, empty tiles are dashed with a grey number; the current slide's group has a 1.5 dp white outline. Caption “PHOTO ORDER · SLIDE BY SLIDE, TOP TO BOTTOM”. Tapping a tile jumps to that spot.
- **E7 · Photo selected** — selection overlay (§5.21); paging locked; dock in selected mode; hint “Drag to move · corners resize · sides stretch”.
- **E8 · Frame shape** — tile row: Original · Square · Rounded · Circle · Arch (selected tile white). Screenshot shows Arch applied to the photo.
- **E9 · Position** — chip row: Adjust inside · Reset size (disabled until resized) · Zoom out · Zoom in · Turn photo (90°) · Center photo.
- **E10 · Adjust inside (crop)** — handles hidden; the full picture shows at 42% outside the box with a dashed white outline; inside the box a rule-of-thirds grid; zoom badge (“1.5×”, DM Mono) above; floating hint pill at the top of the canvas “Adjusting inside · drag to move, pinch to zoom”; “Adjust inside” chip selected with `ic_check`. Zoom range 1×–6×.
- **E11 · Style** — Fill (cover, default) · Fit (contain) · Flip (mirror toggle), three equal chips.
- **E12 · Locked photo** — frame at 70% white, 26 dp ink lock badge on the photo's top-right corner, delete disabled, lock button shows filled ink circle (“Unlock”), rotate button 40% opacity and disabled, Position + Delete disabled in the dock, hint with `ic_lock` “Locked — unlock to move or delete”.
- **N19 · Preview mode** — no handles or +: close (x), title “Preview”, “1 / 6” counter; slide full width (390 dp) at y 140; dots; strip scrubber (40 dp slides, current outlined white, others dimmed); **Keep editing** (raised) + **Save 6 images** (accent, `ic_download`).
- **N20 · Edit text** — slide scaled to 274 dp at the top; text selection frame with corner dots; dark sheet from y 470: “Edit text” + accent **Done**; multiline text field (Serif italic preview, `fl_ed_raised`, 1.5 dp `fl_ed_handle` border); font segmented **Aa** Sans / Serif / Mono; alignment (left/centre/right); Size slider (12–96, value in DM Mono); Colour swatches. The sheet rides above the keyboard.
- **N21 · Stickers** — dark sheet from y 440: “Stickers” + close; category tabs All · Tape · Stamps · Dates · Doodles · Chips; 4-column grid of 80 dp tiles (`fl_ed_raised`, radius 16) showing `img_sticker_*`. Tap adds the sticker to the current slide, selected.
- **N22 · Slides panel** — slide 2 at 274 dp on top; dark sheet: “Slides · 6 · 4:5” + Done; strip of 44 dp thumbnails (gap 8) with numbers, selected = 2 dp accent ring, dashed “Add slide” tile; **link badges** (16 dp accent circle with `ic_link`) between slides that share a spanning photo; note “Slides 1–2 and 5–6 share a photo, so they move as a pair. Long-press a slide to drag it.”; actions Duplicate · Move left · Move right · Delete (danger text).
- **N23 · Save as a draft?** — dark sheet: “You've placed 5 of 12 photos. Save now and pick up where you left off in My Designs.” **Save draft** (accent) · Discard (`fl_ed_danger` text) · Cancel (raised).
- **N23 · First-visit coach mark** — §5.23 on the first + of an empty template: “Tap + to add photos” — 3 tips total: add photos, drag/pinch, Save.

### 7.3 Export & dialogs (`screens/export/`)

- **D1 · Empty-spots check** (`N18-Precheck`) — dark sheet: “7 spots are still empty”, body “Empty spots save as plain paper…”, strip of 50 dp slides with accent count badges per slide (white check badge when full), **Keep editing** / **Save anyway**.
- **E13 · Saving** (`N18-Progress`) — top-bar Save shows spinner + “Saving” (not dimmed by the scrim); sheet “Saving slide 3 of 6”, progress bar, thumbnails: done = accent check badge, current = spinner overlay, pending = dimmed; Cancel.
- **D3 · Saved** (`N18-Success`) — light full screen: `mono_label` “TORN DIARY · 1080 × 1350” + Done; white contact-sheet card (x 16, y 104, 358 × 300, radius 20) with the 6 slides in two rows of three (100 dp wide, gap 12) and DM Mono badges 01–06; “6 images saved, in order”; body names the “FrameLab” album; **Share to Instagram** (accent, `ic_send`) · **Share…** (outlined) · Back to editor (text).
- **D4 · Export failed** (`N18-Error`) — dark sheet: 48 dp accent-tint circle with `ic_alert`, “Export failed”, “Slide 4 couldn't be saved. Slides 1–3 are already in your gallery…”, `mono` error, Close / **Try again**.
- **D5 · Remove all photos?** (`D5-Clear`) — centred dark dialog: “Locked photos stay. You can undo this.” Cancel / **Remove** (`fl_danger`).
- **D6 · Photo is locked** (`D6-Locked`) and **D7 · No empty spot** (`D7-NoSlot`) — editor toasts (white): “**Photo is locked.** Unlock it to delete.” + Unlock; “**No empty spot.** Every spot has a photo.” + Add slide.
- **D2 · Photo access is off** (`D2-Permission`) — light screen: `img_photo_access_off`, title, body, numbered steps card (Open Settings for FrameLab · Tap Photos · Allow access to all photos), **Open settings** (accent), Not now.
- **N24 · Toasts & banner** — catalogue for §5.15 (Draft saved, Added to favourites + Undo, Link copied, Offline + Retry; editor: locked, no empty spot, “5 photos removed” + Undo, Draft saved).

### 7.4 My Designs & Profile (`screens/library/`)

- **N8 · Drafts** — title, segmented Drafts · 3 | Saved · 4 | Favourites · 6; list rows 114 dp: 72 × 90 thumbnail (radius 10), title 16/700, “Edited 2 h ago”, 120 dp progress bar + `mono` “5/12 photos” (complete drafts: ink bar + “Ready to save”), 44 dp ⋯ button.
- **N8 · Draft options menu** — popover (§5.14): Rename · Duplicate · Select · — · Delete.
- **N8 · Rename draft** — white dialog at y 190 (keyboard below): focused field “Torn Diary · Goa trip” with clear, helper “Only you see this name. The template stays Torn Diary.”, Cancel / **Save**.
- **N8 · Delete draft?** — dialog with the draft thumbnail, “The original photos stay in your gallery.”, Cancel / **Delete** (danger).
- **N8 · Select drafts** — top bar Cancel · “2 selected” · Select all; rows with 26 dp check circles (selected = accent + ink check, row tinted #FFEDE7); bottom action bar replaces the tab bar: Duplicate (sunk) · **Delete 2** (danger).
- **N8 · Drafts — empty** — `img_empty_drafts`, “No drafts yet”, “Leave a design with “Save draft” and it waits here…”, **Browse templates**.
- **N9 · Saved** — 2-column grid of saved posts (bare card art with the user's photos), info “6 images · Nov 14”. **N9 empty** — `img_empty_saved`, “Nothing saved yet”.
- **N10 · Favourites** — 2-column grid of hearted templates (heart badge). **N10 empty** — `img_empty_favourites`, “No favourites yet”, “Tap the heart on any template preview…”.
- **N11 · Saved post** — back · title · ⋯; slide viewer 342 × 428 (radius 4, shadow), dots, `mono_label` “6 IMAGES · 1080 × 1350 · SAVED TODAY”, **Share to Instagram**, three 64 dp tiles (Share… · Save again · Edit a copy), “Delete from My Designs” (danger text).
- **N9 · Post in this order** (`N9-PostInOrder`, opened by Share to Instagram on a saved post or the success screen) — light sheet: “Post in this order”, numbered strip 01–06, three numbered steps, **Open Instagram** (`ic_send`), Not now. Hand all images to Instagram in order (multi-image share intent).
- **N11 · Delete saved post?** — dialog: “The 6 images stay in your gallery. Only FrameLab's copy is removed…”.
- **N12 · Profile** — title + settings button; avatar 64 (initials on accentSoft) + name 19/700 + handle + “Edit profile” outlined pill; three stat tiles (number Bricolage 800 24) that are shortcuts (Designs made → My Designs, Favourites → Favourites tab); ink Pro banner (accent crown circle, “FrameLab Pro”, “See plans” accent pill); grouped list: Settings · Help & feedback · Rate FrameLab · Share the app · About.
- **N13 · Profile — guest** — white card: user-icon avatar, “Guest”, “Sign in to keep drafts and favourites on all your devices.”, **Sign in**. Pro banner and list stay.
- **N14 · Edit profile** — Cancel · “Edit profile” · Save; 96 dp avatar with ink camera badge; “Change photo” text button; fields Name and Handle (“@” prefix, helper text). **Change photo sheet**: Take photo · Choose from library · Remove photo (danger) · Cancel. **Handle taken**: 2 dp danger border + `ic_error`, “That handle is taken. Try one of these:” + three DM Mono suggestion chips, Save disabled.
- **N15 · Sign in** — close; three joined slides as the visual; “Keep your designs everywhere”; Continue with Apple (ink) · Continue with Google (outlined) · Continue with email (outlined, `ic_email`) · Continue as guest; legal line with Terms and Privacy links. **Use Google's and Apple's official sign-in buttons** (Credential Manager for Google). **Sign in with email** → field + **Send link**. **Check your inbox** → `img_email_sent`, email in bold, **Open email app**, “Resend link in 0:24” (disabled countdown), “Use a different email”.
- **N16 · Settings** (390 × 1560, scrolls) — groups: EXPORT (Quality High | Max · File format JPG | PNG · Number slides in file names ✓ “torn-diary-01.jpg, -02…” · Save to an album called “FrameLab” ✓) · EDITOR (Always show photo-order numbers · Snap rotation to 90° ✓ · Haptics ✓) · APPEARANCE (Theme Light | Dark | System; “The editor always stays dark”) · PERMISSIONS (Photos “Full access” · Open system settings `ic_external`) · STORAGE (Cache “84 MB · template images” + Clear · Delete all drafts) · ACCOUNT (Signed in as · Sign out · Delete account) · ABOUT (Version · Terms · Privacy · Licences). **Sign out?** dialog (ink confirm). **Delete account** screen: what gets deleted / what stays, Pro reminder, “Type DELETE to confirm” field, **Delete account** (danger). **Licences** page: SOFTWARE (React Native, Expo, Supabase JS — replace with your real dependency list for the Kotlin app) and FONTS (Bricolage Grotesque, Inter, DM Mono — OFL 1.1) with DM Mono licence tags.
- **N12 · Help & FAQ** — search; COMMON QUESTIONS accordion (first expanded); ink “Still stuck?” card with **Send feedback** (accent) + **Email support** (outlined white). **Send feedback** — topic chips (Something's broken · Idea · Template request · Other), message field, screenshots (64 × 80 tiles + dashed add tile), “Include device and app info” switch, **Send feedback**. **Rate FrameLab** — sheet “Enjoying FrameLab?”: **Yes, rate FrameLab** → Play In-App Review API; “Not really, send feedback” → feedback form; Maybe later. **About** — wordmark, tagline, `mono` version, joined-slides strip, links (Website, Follow on Instagram, Terms, Privacy, Licences). **Share the app** — sheet with an ink invite card (wordmark, line, slide strip, link) + Copy link · **Share…**.
- **N17 · FrameLab Pro** — close; `img_pro_hero`; “FrameLab Pro” 34/38; four benefits with accentSoft check circles; plan cards (selected = 2 dp ink border + accent “BEST VALUE” tag); **Start free trial**; Restore purchases; legal line. Locked cards show the Pro badge (§5.8). **Welcome to Pro** — ink screen, `img_pro_welcome_hero`, “You're Pro.”, unlocked list, `mono` trial line, **Start creating**. Use Google Play Billing.

### 7.5 Onboarding (`screens/onboarding/`)

- **N1 · Splash** — wordmark 46/50 centred at y ≈ 372, three 22 × 6 progress bars (first accent), `mono_small` “SEAMLESS CAROUSELS” at the bottom. Use the Android 12 SplashScreen API for the system splash (launcher icon on #FAF7F2), then this screen while templates load (≤ 1 s).
- **N2 · Onboarding 1–3** — Skip (top right); illustration (`img_onboarding_flow` / `_add` / `_save`); step dots at y 556 (x 24, left-aligned); title `display`; body `body_l`; button at y 748: **Next** (ink) ×2, then **Start creating** (accent). Copy: “Posts that flow” · “Tap + to add photos” · “Save & post in order”.
- **N2 · Photo-access primer** — `img_photo_access`, “Let FrameLab use your photos”, two reasons with 44 dp accentSoft icon wells, **Allow photo access** → system prompt; Not now.

---

## 8. Editor & template rendering

**8.1 Slide sizes (export px):** square 1080 × 1080 · 4:5 1080 × 1350 (default) · 3:4 1080 × 1440 · 9:16 1080 × 1920. A carousel is N slides side by side on one long canvas (6 × 1080 = 6480 px wide); elements may cross slide edges.

**8.2 Building blocks the renderer draws:** photo spot (rect, any corner radius incl. circle/arch, rotation, border, optional mask) · box (fill, radius, solid/dashed/dotted border, shadow, opacity, rotation, children) · text (sans/serif/mono, size, weight, italic, align, tracking, line height, colour) · image (remote PNG/WebP for stickers, textures, frames). Layers: background → per-slide elements → top layer. Torn edges are polygons; paper is fill + ruled lines.

**8.3 Data:** Supabase (read-only): `templates` (`preview` = layout used for live thumbnails), `template_categories` (Home rows, `sort_order`), `category_templates`. Frames come from the frame API. If the backend is unreachable, use the built-in copies and show the offline banner.

**8.4 Canvas view:** current slide scaled to the width minus 24 dp margins; the next slide peeks 24 dp on the right under `fl_peek_dim`. Horizontal paging, one slide per page; paging is locked while a photo is selected.

**8.5 Filling photos:** Photos button opens the system photo picker limited to the number of empty spots and fills empty spots in fill order (slide → top → left). Tapping an empty spot opens the picker starting at that spot. When everything is full, picking refills all spots. Shuffle swaps photos between unlocked spots and resets their position. Clear removes all unlocked photos (confirm D5; undoable). Undo/redo history: 50 steps.

**8.6 Photo order view:** numbers on spots + the strip described in E6. Off by default; Settings › Editor can keep numbers on.

**8.7 Gestures on a selected photo:** drag body = move box · two fingers = resize + rotate · corner handles = proportional resize · side handles = stretch (only when that side ≥ 110 dp) · rotate button: drag = free rotation snapping to 0/90/180/270 within 4°, tap = +90°. Tap another photo (even through handles) selects it; tap empty canvas deselects. Adjust inside: drag pans the image inside its box; pinch zooms 1×–6×. Locked photos ignore move/resize/rotate/delete.

**8.8 Panels:** Frame (Original, Square, Rounded, Circle, Arch) · Position (Adjust inside, Reset size, Zoom out, Zoom in, Turn photo 90°, Center photo) · Style (Fill/cover, Fit/contain, Flip) · Color (template colour + 8 swatches; changes the slide background).

**8.9 Saving:** pre-check for empty spots → save each slide at full size (JPG quality 95 by default; Settings: High | Max quality, JPG | PNG) into `Pictures/FrameLab` via MediaStore, file names `<template>-01.jpg …` when numbering is on → progress sheet → success screen. Ask for photo permission the first time (primer first). On failure, show the error sheet; slides already saved stay.

**8.10 Drafts:** leaving with unsaved changes asks “Save as a draft?”. A draft stores the template id, every spot's photo URI and transform, text edits, stickers, background colour and slide order. Show it in My Designs › Drafts with progress (“5/12 photos”).

**8.11 Preview mode:** clean viewer — no + buttons, handles or numbers; swipe between slides; strip scrubber shows the joined post.

---

## 9. Accessibility

- Touch targets ≥ 48 dp everywhere (visual handles and dots may be smaller; their hit areas are not).
- Every icon-only button needs a `contentDescription` (per screen in `tokens/ui-copy.json` → `contentDescriptions`, taken from the `aria-label`s in the design files: “Back”, “Undo”, “Preview”, “Add photo”, “Add background photo”, “Delete photo”, “Replace photo”, “Lock photo”, “Rotate photo”, “Deselect photo”, “More options for <name>”, “Clear search”…).
- Switches use `Role.Switch`; segmented controls and chips expose selected state; page dots announce “Slide 2 of 6”.
- Keep the contrast rules in §3.5. Text scales with the system font size; let rows grow instead of truncating where possible.
- Respect “Remove animations”: skip slide/sheet animations and the shimmer.

---

## 10. Placeholders and decisions to confirm

- Prices and trial: `[YEARLY PRICE]`, `[MONTHLY PRICE]`, `[Trial length and renewal terms]`, `[DATE]`.
- Links and names: `[website]`, `@[handle]`, `[APP LINK]`, `[Company name]`, version `[x.y.z]`.
- Sample data in screenshots (Sam Rivera, counts like “126 designs”, “84 MB”) is illustrative.
- Fonts are a proposal (the old app used the system font). The launcher icon is a proposal.
- “Blank carousel” is marked SOON in the Create sheet.

---

## Appendix A — Screen index

Every artboard, in canvas order. Screenshots are 2× (1 px in the design = 1 dp). The design file has exact measurements.

| Page | Screen | Design file | Screenshot | Size (dp) |
|---|---|---|---|---|
| Home & Explore | H1 · Home — Templates | `design-source/Main.dc.html` | `screens/home/Main.png` | 390×844 |
| Home & Explore | H2 · Home — loading | `design-source/H2-Loading.dc.html` | `screens/home/H2-Loading.png` | 390×844 |
| Home & Explore | H3 · Home — couldn't load | `design-source/H3-Error.dc.html` | `screens/home/H3-Error.png` | 390×844 |
| Home & Explore | H4 · Home — Frames tab | `design-source/H4-Frames.dc.html` | `screens/home/H4-Frames.png` | 390×844 |
| Home & Explore | H5 · Search — results | `design-source/H5-Search.dc.html` | `screens/home/H5-Search.png` | 390×844 |
| Home & Explore | H6 · Search — no results | `design-source/H6-NoResults.dc.html` | `screens/home/H6-NoResults.png` | 390×844 |
| Home & Explore | N24 · Offline banner | `design-source/N24-Offline.dc.html` | `screens/home/N24-Offline.png` | 390×844 |
| Home & Explore | H7 · Card variants | `design-source/H7-Cards.dc.html` | `screens/home/H7-Cards.png` | 760×660 |
| Home & Explore | N3 · Explore — filters & sort | `design-source/N3-Explore.dc.html` | `screens/home/N3-Explore.png` | 390×844 |
| Home & Explore | N4 · Explore — no results | `design-source/N4-NoResults.dc.html` | `screens/home/N4-NoResults.png` | 390×844 |
| Home & Explore | N4 · Explore — loading | `design-source/N4-Loading.dc.html` | `screens/home/N4-Loading.png` | 390×844 |
| Home & Explore | N5 · Category — See all | `design-source/N5-SeeAll.dc.html` | `screens/home/N5-SeeAll.png` | 390×844 |
| Home & Explore | N6 · Create sheet | `design-source/N6-Create.dc.html` | `screens/home/N6-Create.png` | 390×844 |
| Home & Explore | N7 · Template preview | `design-source/N7-Preview.dc.html` | `screens/home/N7-Preview.png` | 390×844 |
| Editor | E1 · Opening a design | `design-source/E1-Loading.dc.html` | `screens/editor/E1-Loading.png` | 390×844 |
| Editor | E2 · Couldn't open | `design-source/E2-Error.dc.html` | `screens/editor/E2-Error.png` | 390×844 |
| Editor | E3 · Empty template · 0/12 | `design-source/E3-Empty.dc.html` | `screens/editor/E3-Empty.png` | 390×844 |
| Editor | E4 · Partly filled | `design-source/E4-Filled.dc.html` | `screens/editor/E4-Filled.png` | 390×844 |
| Editor | E5 · Background colour | `design-source/E5-Color.dc.html` | `screens/editor/E5-Color.png` | 390×844 |
| Editor | E6 · Photo order (was Guides) | `design-source/E6-Order.dc.html` | `screens/editor/E6-Order.png` | 390×844 |
| Editor | E7 · Photo selected | `design-source/E7-Selected.dc.html` | `screens/editor/E7-Selected.png` | 390×844 |
| Editor | E8 · Frame shape | `design-source/E8-Frame.dc.html` | `screens/editor/E8-Frame.png` | 390×844 |
| Editor | E9 · Position | `design-source/E9-Position.dc.html` | `screens/editor/E9-Position.png` | 390×844 |
| Editor | E10 · Adjust inside | `design-source/E10-Crop.dc.html` | `screens/editor/E10-Crop.png` | 390×844 |
| Editor | E11 · Style | `design-source/E11-Style.dc.html` | `screens/editor/E11-Style.png` | 390×844 |
| Editor | E12 · Locked photo | `design-source/E12-Locked.dc.html` | `screens/editor/E12-Locked.png` | 390×844 |
| Editor | N19 · Preview mode | `design-source/N19-PreviewMode.dc.html` | `screens/editor/N19-PreviewMode.png` | 390×844 |
| Editor | N20 · Edit text | `design-source/N20-Text.dc.html` | `screens/editor/N20-Text.png` | 390×844 |
| Editor | N21 · Stickers | `design-source/N21-Stickers.dc.html` | `screens/editor/N21-Stickers.png` | 390×844 |
| Editor | N22 · Slides panel | `design-source/N22-Slides.dc.html` | `screens/editor/N22-Slides.png` | 390×844 |
| Editor | N23 · Save as draft? | `design-source/N23-Draft.dc.html` | `screens/editor/N23-Draft.png` | 390×844 |
| Editor | N23 · First-visit coach mark | `design-source/N23-Coach.dc.html` | `screens/editor/N23-Coach.png` | 390×844 |
| Export & dialogs | N18 · D1 Empty spots check | `design-source/N18-Precheck.dc.html` | `screens/export/N18-Precheck.png` | 390×844 |
| Export & dialogs | N18 · E13 Saving | `design-source/N18-Progress.dc.html` | `screens/export/N18-Progress.png` | 390×844 |
| Export & dialogs | N18 · D3 Saved | `design-source/N18-Success.dc.html` | `screens/export/N18-Success.png` | 390×844 |
| Export & dialogs | N18 · D4 Export failed | `design-source/N18-Error.dc.html` | `screens/export/N18-Error.png` | 390×844 |
| Export & dialogs | D5 · Remove all photos? | `design-source/D5-Clear.dc.html` | `screens/export/D5-Clear.png` | 390×844 |
| Export & dialogs | D6 · Photo is locked | `design-source/D6-Locked.dc.html` | `screens/export/D6-Locked.png` | 390×844 |
| Export & dialogs | D7 · No empty spot | `design-source/D7-NoSlot.dc.html` | `screens/export/D7-NoSlot.png` | 390×844 |
| Export & dialogs | D2 · N24 Photo access off | `design-source/D2-Permission.dc.html` | `screens/export/D2-Permission.png` | 390×844 |
| Export & dialogs | N24 · Toasts & banner | `design-source/N24-Toasts.dc.html` | `screens/export/N24-Toasts.png` | 760×560 |
| My Designs & Profile | N8 · Drafts | `design-source/N8-Drafts.dc.html` | `screens/library/N8-Drafts.png` | 390×844 |
| My Designs & Profile | N8 · Drafts — empty | `design-source/N8-DraftsEmpty.dc.html` | `screens/library/N8-DraftsEmpty.png` | 390×844 |
| My Designs & Profile | N9 · Saved | `design-source/N9-Saved.dc.html` | `screens/library/N9-Saved.png` | 390×844 |
| My Designs & Profile | N9 · Saved — empty | `design-source/N9-SavedEmpty.dc.html` | `screens/library/N9-SavedEmpty.png` | 390×844 |
| My Designs & Profile | N10 · Favourites | `design-source/N10-Favourites.dc.html` | `screens/library/N10-Favourites.png` | 390×844 |
| My Designs & Profile | N10 · Favourites — empty | `design-source/N10-FavEmpty.dc.html` | `screens/library/N10-FavEmpty.png` | 390×844 |
| My Designs & Profile | N11 · Saved post | `design-source/N11-PostDetail.dc.html` | `screens/library/N11-PostDetail.png` | 390×844 |
| My Designs & Profile | N12 · Profile | `design-source/N12-Profile.dc.html` | `screens/library/N12-Profile.png` | 390×844 |
| My Designs & Profile | N13 · Profile — guest | `design-source/N13-Guest.dc.html` | `screens/library/N13-Guest.png` | 390×844 |
| My Designs & Profile | N14 · Edit profile | `design-source/N14-EditProfile.dc.html` | `screens/library/N14-EditProfile.png` | 390×844 |
| My Designs & Profile | N15 · Sign in | `design-source/N15-SignIn.dc.html` | `screens/library/N15-SignIn.png` | 390×844 |
| My Designs & Profile | N16 · Settings | `design-source/N16-Settings.dc.html` | `screens/library/N16-Settings.png` | 390×1560 |
| My Designs & Profile | N17 · FrameLab Pro | `design-source/N17-Paywall.dc.html` | `screens/library/N17-Paywall.png` | 390×844 |
| My Designs & Profile | N8 · Draft options menu | `design-source/N8-Menu.dc.html` | `screens/library/N8-Menu.png` | 390×844 |
| My Designs & Profile | N8 · Rename draft | `design-source/N8-Rename.dc.html` | `screens/library/N8-Rename.png` | 390×844 |
| My Designs & Profile | N8 · Delete draft? | `design-source/N8-DeleteDraft.dc.html` | `screens/library/N8-DeleteDraft.png` | 390×844 |
| My Designs & Profile | N8 · Select drafts | `design-source/N8-Select.dc.html` | `screens/library/N8-Select.png` | 390×844 |
| My Designs & Profile | N11 · Share to Instagram — post in order | `design-source/N9-PostInOrder.dc.html` | `screens/library/N9-PostInOrder.png` | 390×844 |
| My Designs & Profile | N11 · Delete saved post? | `design-source/N11-DeletePost.dc.html` | `screens/library/N11-DeletePost.png` | 390×844 |
| My Designs & Profile | N14 · Change profile photo | `design-source/N14-PhotoSheet.dc.html` | `screens/library/N14-PhotoSheet.png` | 390×844 |
| My Designs & Profile | N14 · Handle taken | `design-source/N14-HandleError.dc.html` | `screens/library/N14-HandleError.png` | 390×844 |
| My Designs & Profile | N15 · Sign in with email | `design-source/N15-Email.dc.html` | `screens/library/N15-Email.png` | 390×844 |
| My Designs & Profile | N15 · Check your inbox | `design-source/N15-Inbox.dc.html` | `screens/library/N15-Inbox.png` | 390×844 |
| My Designs & Profile | N16 · Sign out? | `design-source/N16-SignOut.dc.html` | `screens/library/N16-SignOut.png` | 390×844 |
| My Designs & Profile | N16 · Delete account | `design-source/N16-DeleteAccount.dc.html` | `screens/library/N16-DeleteAccount.png` | 390×844 |
| My Designs & Profile | N12 · Help & FAQ | `design-source/N12-Help.dc.html` | `screens/library/N12-Help.png` | 390×844 |
| My Designs & Profile | N12 · Send feedback | `design-source/N12-Feedback.dc.html` | `screens/library/N12-Feedback.png` | 390×844 |
| My Designs & Profile | N12 · Rate FrameLab | `design-source/N12-Rate.dc.html` | `screens/library/N12-Rate.png` | 390×844 |
| My Designs & Profile | N12 · About | `design-source/N12-About.dc.html` | `screens/library/N12-About.png` | 390×844 |
| My Designs & Profile | N12 · Share the app | `design-source/N12-Share.dc.html` | `screens/library/N12-Share.png` | 390×844 |
| My Designs & Profile | N16 · Licences (legal page) | `design-source/N16-Licences.dc.html` | `screens/library/N16-Licences.png` | 390×844 |
| My Designs & Profile | N17 · Welcome to Pro | `design-source/N17-Welcome.dc.html` | `screens/library/N17-Welcome.png` | 390×844 |
| Onboarding | N1 · Splash | `design-source/N1-Splash.dc.html` | `screens/onboarding/N1-Splash.png` | 390×844 |
| Onboarding | N2 · Onboarding 1 — Posts that flow | `design-source/N2-Flow.dc.html` | `screens/onboarding/N2-Flow.png` | 390×844 |
| Onboarding | N2 · Onboarding 2 — Tap + to add | `design-source/N2-Add.dc.html` | `screens/onboarding/N2-Add.png` | 390×844 |
| Onboarding | N2 · Onboarding 3 — Save & post | `design-source/N2-Save.dc.html` | `screens/onboarding/N2-Save.png` | 390×844 |
| Onboarding | N2 · Photo access primer | `design-source/N2-Primer.dc.html` | `screens/onboarding/N2-Primer.png` | 390×844 |
| Foundations | Foundations | `design-source/Foundations.dc.html` | `screens/foundations/Foundations.png` | 1280×1240 |
| Foundations | Component · TabBar | `design-source/TabBar.dc.html` | `screens/foundations/TabBar.png` | 390×88 |
| Foundations | Component · EditorTop | `design-source/EditorTop.dc.html` | `screens/foundations/EditorTop.png` | 390×56 |
| Foundations | Component · Dock | `design-source/Dock.dc.html` | `screens/foundations/Dock.png` | 390×80 |
| Foundations | Component · Card | `design-source/Card.dc.html` | `screens/foundations/Card.png` | 128×204 |
| Foundations | Component · Diary (Torn Diary template) | `design-source/Diary.dc.html` | `screens/foundations/Diary.png` | 684×143 |
| Foundations | Component · Ed (editor screen, any state) | `design-source/Ed.dc.html` | `screens/foundations/Ed.png` | 390×844 |

## Appendix B — File list

407 files. Every file name is listed in the section shown.

| Folder | Files | Contents |
|---|---|---|
| (root) | 2 | `HANDOFF.md` (this spec), `CLAUDE.md` (rules for Claude Code) |
| `screens/<page>/` | 85 | 2× screenshots, pages: editor, export, foundations, home, library, onboarding — see Appendix A |
| `design-source/` | 86 | 85 design files (`.dc.html`) + `canvas.json` (board layout) |
| `res/drawable/` | 76 | `ic_*.xml` vector icons — §4.1 |
| `res/drawable-xxhdpi/` | 27 | `img_*.png` at 3× — illustrations §4.2, stickers §4.3, wordmarks |
| `res/drawable-xxxhdpi/` | 27 | same files at 4× |
| `res/mipmap-{mdpi…xxxhdpi}/` | 10 | `ic_launcher_foreground.png`, `ic_launcher_monochrome.png` in 5 densities — §4.4 |
| `res/mipmap-anydpi-v26/` | 2 | `ic_launcher.xml`, `ic_launcher_round.xml` (adaptive icon) |
| `res/font/` | 8 | bricolage_grotesque_bold.ttf, bricolage_grotesque_extrabold.ttf, dm_mono_medium.ttf, dm_mono_regular.ttf, inter_bold.ttf, inter_medium.ttf, inter_regular.ttf, inter_semibold.ttf |
| `res/values/` | 1 | `colors.xml` — §3.1–3.2 |
| `icons/` | 77 | `icons_sheet.png` (contact sheet) + `svg/ic_*.svg` (same icons as SVG) |
| `tokens/` | 2 | `design-tokens.json`, `ui-copy.json` |
| `store/` | 1 | `play_store_icon_512.png` |
| `fonts-licences/` | 3 | BricolageGrotesque-OFL.txt, DMMono-OFL.txt, Inter-OFL.txt |
