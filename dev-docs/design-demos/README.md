# Design demos (HTML)

Static **phone sketches** for the native Android wallet. They are the **visual and interaction contract** for layout, chrome, and theme on each scheduled version that links a file here (for example [`wallet-020.html`](./wallet-020.html) for 0.2.0).

## What these files are

| Source | Use in this repo |
|--------|------------------|
| **This folder (`wallet-*.html`)** | Ship target for **Compose UI**: screens, navigation chrome, spacing, tokens, empty states. Best-effort HTML inside a ~390×780 phone frame. |
| **Chrome extension** (`airicyu/airwave-wallet` popup / settings **code**) | **Product IA** only: menu order, Settings rows, field names, persistence rules, color **token values** (`style.css` `:root`). |
| **Extension `dev-docs/design-demos/`** | Not authoritative. Do not copy layout from there. |

**Theme:** dark surfaces and blue accent (`#3d9cf0` / `#2a6ea8`) match [`AirwaveTheme.kt`](../../android/app/src/main/kotlin/com/airwave/wallet/theme/AirwaveTheme.kt) and extension `style.css`. **Layout** is **mobile-native** (bottom tabs, top sheet menu, safe areas, Back on subpages)—not a 384px popup clone.

## Workflow (design ↔ implementation)

Follow this order whenever UI behavior or chrome changes:

1. **Product** — Decide IA and fields in the version `INDEX.md` and `docs/how.md` (extension code may inform labels and order).
2. **Mobile adaptation** — If phone layout differs from extension popup, record it in `docs/reasoning.md` or design-review under **Mobile adaptation** (one short paragraph per decision).
3. **Design demo first** — Update the version’s `wallet-*.html` so every affected screen in the left nav matches the intended phone UI. Open the HTML in a browser and sanity-check.
4. **Human sign-off** — Implementer states what changed in the demo and waits for explicit approval (or a waived design-review note) before Kotlin changes—except tiny fixes that only close a gap between app and an already-approved demo.
5. **Implement** — Android Compose follows the demo + `how.md` + [`design-principles.md`](../design-principles.md). Extension TypeScript/CSS layout is not ported.
6. **Close the loop** — `implementation-review.md` lists each demo screen id and whether the app matches (or documents an accepted delta).

**Rule:** Do not ship a materially different mobile chrome (menu pattern, bottom bar, Back placement) than the linked HTML without updating the demo first.

## Phone frame vs designer notes

| Region | Meaning for implementers |
|--------|-------------------------|
| **Inside the phone mockup** | Shipped UI: labels, empty states, controls, and layout the user sees. If it is not meant to ship, it must **not** appear here. |
| **Right-hand “Designer notes” panel** (`wallet-020.html` and later demos) | Contract commentary only: scope boundaries, “do not ship” footnotes, links to `INDEX` / `how.md`. **Never** implement this text in Compose. |
| **Page header** (above the layout) | About the demo file itself, not product copy. |

When adding a new screen to a demo, put **Ship:** / **Do not ship:** bullets in the notes panel; keep the phone frame limited to real strings (usually English `ui-messages` / `AppStrings`).

Shared brand art for unlock (and future About): copy from extension `wallet/public/icon128.png` into `design-demos/assets/brand-icon-128.png` and Android `res/drawable/brand_mark.png`. Unlock background is **CSS/Compose radials** from extension `style.css`, not a separate wallpaper image.

## File naming

| File | Version |
|------|---------|
| `wallet-010.html` | 0.1.0 baseline |
| `wallet-0XY.html` | `0.X.Y` when INDEX links it |

## Related docs

- Screen rules: [`design-principles.md`](../design-principles.md)
- Review gates: [`roadmap/agent-workflow.md`](../roadmap/agent-workflow.md)
- Per-version contract: `dev-docs/roadmap/X.Y.Z/INDEX.md`
