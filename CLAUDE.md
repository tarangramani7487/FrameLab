# FrameLab Android — instructions for Claude Code

Put this file in the repo root and the whole handoff package in `design/`.

## Source of truth
- Spec: `design/HANDOFF.md`. Read §0 first, then the section for the screen you are building.
- Look: `design/screens/<page>/<screen>.png`, 2× screenshots of 390 × 844 dp phones.
- Exact numbers: `design/design-source/<screen>.dc.html`. Inline styles are the spec: px = dp, font px = sp.
- Text: `design/tokens/ui-copy.json`. Use the exact wording, and put every string in `strings.xml`.
- Tokens: `design/tokens/design-tokens.json` and `design/res/values/colors.xml`.

## Stack
Kotlin, Jetpack Compose with Material 3 (fully custom FrameLab colours and type, not the dynamic colour scheme), and Navigation Compose. Use no XML layouts. minSdk 26. Draw edge-to-edge.

## Rules
- **Colours:** only the named colours (`fl_*`, `fl_ed_*`, `tpl_*`, `swatch_*`, `pastel_*`). Don't invent hex values.
- **Icons:** only `R.drawable.ic_*` from `design/res/drawable`, tinted where you use them. No emoji, Unicode glyphs or icon libraries.
- **Images:** only the `img_*` PNGs listed in HANDOFF §4.2–4.4, at the dp sizes and positions given there. Template slides, card thumbnails and previews are drawn from template data. The photos in the screenshots are placeholders.
- **Fonts:** `res/font` (Bricolage Grotesque for display, Inter for UI, DM Mono for labels), mapped to the type styles in HANDOFF §3.3.
- **Contrast (§3.5):**
  - Ink text on the accent colour, never white.
  - Accent-coloured text on light backgrounds uses `fl_accent_ink`.
- **Touch targets:** at least 48 dp. Every icon-only button gets the `contentDescription` from `ui-copy.json`.
- **Bars stay above the navigation-bar inset:** the tab bar, editor dock, toasts and bottom buttons. Keep the status-bar area empty.
- **Editor:** always dark; it hides the tab bar. Android back closes a sheet, then deselects, then asks "Save as a draft?" if there are unsaved changes.
- **Placeholders** in HANDOFF §10 stay as visible placeholders. Don't invent prices, links or legal text.

## Working loop for each screen
1. Open the screenshot and the design file.
2. Build the screen from the shared components (HANDOFF §5). Add a new component only when the spec has one.
3. Add a `@Preview` at 390 × 844 dp with sample data, compare it with the screenshot, and fix spacing, sizes and colours until they match.
4. Build every state listed for the screen: loading, empty, error, selected and so on. Each state has its own screenshot.
