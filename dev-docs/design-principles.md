# Design principles

Screen rules for the mobile wallet app (native Android for 0.1.0+). A version **INDEX** overrides this file when it says so.

The Chrome extension popup rules (fixed 384px shell, CSS classes) live in that repo. Do not copy that chrome onto a phone layout.

## Visual contract (design demos)

For versions that link an HTML file under [`design-demos/`](design-demos/README.md):

- That file is the **Android visual contract** (layout, menu chrome, tabs, Back, theme tokens on phone).
- The version **INDEX must copy** the phone-frame structure into Decided (rows, glyphs, titles, subtitles). A link to the HTML without those sentences is not a spec. Function-only Tracks are not enough when the version ships a screen. See [roadmap/GUIDELINES.md](roadmap/GUIDELINES.md) Screen spec.
- Extension popup code supplies **IA, copy, and token values**—not popup pixel layout.
- Change the demo **before** (or in the same Track as) Compose when mobile chrome or spacing changes; see the workflow in [`design-demos/README.md`](design-demos/README.md).
- **Only content inside the phone frame** is shipped copy and controls. Designer notes beside the mockup are **not** user-visible text.
- **Never put instructional / designer / implementation notes inside the phone mock** (e.g. 「Receive 走 Home…」「與 web 一致」). Dev agents will treat them as on-screen requirements. Put those only in the right-hand Designer notes column or in `REVIEW-vs-web.md` / this file (Eric 2026-10-24).

**0.2.0:** [`wallet-020.html`](design-demos/wallet-020.html) — full-width **menu top sheet** under the status bar, Material-style **bottom tabs**, blue accent menu icon on Home; **Unlock** matches extension `.unlock-screen` (layered radials + brand mark).

## Back navigation

Every **secondary surface** must give an obvious way to go back. A secondary surface is any full-screen or modal layer that is not the primary **Home** hub (Assets / Activity with bottom tabs).

| Surface | Back control | Behavior |
|---------|----------------|----------|
| **Home** | None required | Menu opens from the top bar; tabs stay on Home. |
| **Hamburger menu** (modal sheet) | **Back** top-leading | Dismisses the menu and returns to whatever was underneath (Home or a pushed subpage). |
| **Pushed subpages** (Network, Settings hub, settings children, empty menu placeholders, etc.) | **Back** top-leading | Pops one level on the in-app stack (subpage → Home, or settings child → Settings hub). |
| **MWA approval** | Reject (and Lock) | Not a settings-style back stack; use explicit approve / reject actions. |

Conventions:

- Put **Back** at the **top-leading** corner (minimum 44×44 dp touch target). A chevron / arrow icon is preferred on phone; the accessible name is the localized **Back** string (`menu.back` in 0.2.0).
- Do not rely on the system gesture alone; always show in-app Back (or equivalent) on secondary surfaces.
- Opening the hamburger from a subpage is allowed; **Back** on the menu sheet closes only the menu, not the subpage underneath.

## System bars

App UI does not draw under the status bar. The clock, notification icons, and status icons stay in a system row. No wallet control, title, or account chip overlaps that row.

Apply this once at each activity root (`MainActivity` and `MwaActivity`) with the status-bar inset the system reports. Do not pad each screen separately. Do not use `windowOptOutEdgeToEdgeEnforcement`: Android 16 and later ignore it, and a theme that still names it fails to inflate on those versions.

The same root inset keeps menu sheets and subpage top bars below the status bar.

## Token trust shield placement
- Jupiter / verified **green shield is leftmost**, before the token icon (same as web).
- Mobile demo icons must reuse **web `StrokeIcon` paths** + real token PNGs (Jupiter / token-list); do not invent letter-circle placeholders.
- Do **not** place the shield next to the token name: scam tokens can embed a shield emoji in the symbol/name to spoof verification (Eric 2026-10-24).

## Android icons (web paths, not Material, not Canvas)

Chrome glyphs that exist in extension `wallet/src/popup/components/StrokeIcon.tsx` or `TokenTrust` in `HomeTokenList.tsx` are copied into `android/app/src/main/res/drawable/` as **vector XML** with the same SVG `d` values. Do not invent a Compose `Canvas` polygon. Do not substitute `Icons.Default.*` when a StrokeIcon exists. Do not export PNG for those glyphs.

- One-color strokes (`stroke="currentColor"`): vector uses `android:strokeColor="#FFFFFFFF"` and Compose `Icon` `tint` supplies the theme color.
- Two-color `TokenTrust`: drawable `ic_token_trust_verified.xml`. Shield fill is `AirwaveColors.Ok` (`#4ECB8D`). Check stroke is `#0F1419`. Draw with `tint = Color.Unspecified`.
- Token logos stay https bitmaps from Jupiter / the token list. They are not drawables and not letter circles.

A later version that adds refresh, recycle, copy, back, accounts, book, network, settings, globe, info, lock, key, eye, or external copies that StrokeIcon into a new vector in the same folder, then uses `painterResource`. Menu chrome still on Material icons until a version that touches that bar copies the matching StrokeIcon.
