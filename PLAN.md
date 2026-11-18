# FrameLab Android — build plan

Spec: `design/HANDOFF.md` · rules: `CLAUDE.md` · screenshots: `design/screens/` · exact numbers: `design/design-source/`.

| Phase | Scope | Owner / files | Status |
|---|---|---|---|
| 0 | Gradle project (AGP 9.2.1, Kotlin 2.3.20, Compose BOM, Nav, Coil, DataStore, Serialization), `design/res` merged, strings.xml generated from ui-copy.json (821 strings, index in `design/strings-index.tsv`) | root | done |
| 1 | Theme (§3): `ui/theme/*` · shared components (§5): `ui/components/*` | core | done |
| 2 | Template model + DrawScope renderer (`data/model`, `render/`) · built-in Torn Diary · stores: drafts, saved, favourites, settings, profile (`data/AppData.kt`) | core | done |
| 3 | Navigation shell, routes, tab bar, global sheets/toasts (`nav/FrameLabRoot.kt`) | core | done |
| 4 | Built-in catalogue: all templates + frames + Home rows (`data/catalog/*`) | catalog agent | done |
| 5 | Home & Explore: H1–H7, N3–N7, N24 offline (`feature/home/*`) | home agent | done |
| 6 | My Designs & Profile: N8–N17 (`feature/library/*`, `feature/profile/*`) | library agent | done |
| 7 | Onboarding N1–N2, export light screens D2/D3, Exporter (MediaStore) + sharing (`feature/onboarding/*`, `feature/export/*`) | onboarding/export agent | done |
| 8 | Editor E1–E12, N19–N23, export sheets D1/E13/D4/D5–D7 (`feature/editor/*`) | editor agent | done |
| 9 | Integration, device run, screenshot comparison, fixes | core | done |

Debug: `adb shell am start -n com.framelab.app/.MainActivity --es route "<route>"` opens any route (see `nav/Routes`).
Placeholders in HANDOFF §10 (prices, links, company name, version) stay visible as placeholders.

## Simulated / to connect later
- Supabase: set `SUPABASE_URL` / `SUPABASE_ANON_KEY` in `app/build.gradle.kts` (blank = built-in catalogue only).
- Sign in with Apple/Google, Google Play Billing (Pro), feedback sending, In-App Review: local stand-ins, marked `TODO`.
- Legal/website/support links: blank constants in `LegalLinks` (feature/profile) — rows show a placeholder toast.
- Coach tips 2–3 copy and the Stickers/Slides buttons beside the page dots were not in the design — review.
