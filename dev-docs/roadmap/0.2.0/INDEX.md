# 0.2.0 — Hamburger menu, Settings, theme align

- **Status:** `shipped`
- **Parent:** [0.1.0](../0.1.0/INDEX.md)
- **Changelog:** [`changelog.md`](../../../changelog.md) under `0.2.0 — 2026-10-20`
- **Design demo (Android visual contract):** [`dev-docs/design-demos/wallet-020.html`](../../design-demos/wallet-020.html) — mobile layout; extension code is IA/tokens only ([`design-demos/README.md`](../../design-demos/README.md))

## Product

Build on the shipped 0.1.0 Android wallet an **Android counterpart** of the Chrome extension wallet shell:

1. **Hamburger menu** on Home (same order as the live extension popup).
2. Several menu rows are **empty placeholders** only (not Network — Network is real).
3. **Network** selects active cluster (Devnet / Mainnet). **Settings → RPC** only configures per-cluster RPC endpoints (does not pick which network is in use).
4. **Settings** is a real hub with working subpages (persist settings). API key rows: idle eye+pencil; edit confirm+discard only (no eye).
5. **Lock wallet** from the menu works.
6. **UI theme alignment** with the extension: same dark surfaces + blue Airwave accent family from live `style.css` (`#3d9cf0` / `#2a6ea8`). Phone layout — not a pixel clone of the 384px popup.
7. **MWA sign-and-send submit** still targets the **request’s chain** (0.1.0 rule). Settings RPC endpoints are looked up for that chain’s cluster; Network’s `cluster` does not override a mismatched request (see `how.md`).

**UI source of truth for the extension side:** live code under `airicyu/airwave-wallet` — especially `wallet/src/popup/PopupMarkup.tsx` (menu), `wallet/src/popup/settings/*`, `wallet/src/popup/style.css` (`:root` tokens), and `wallet/src/shared/ui-messages.ts` (English labels). Do **not** treat that repo’s `dev-docs/design-demos/` as authoritative (many files there are stale).

Develop and hand-test on **Windows + Android Studio emulator** (local checkout).

## Read order

1. This file
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)
4. [`dev-docs/design-demos/wallet-020.html`](../../design-demos/wallet-020.html)
5. Code under `android/`

Do not start from [`HANDOFF.md`](./HANDOFF.md).

## Decided

| Topic | Decision |
|-------|----------|
| Platform | Native Android only (Jetpack Compose + Material 3). No Flutter |
| Dev machine | Windows local repo + Android Studio emulator for iteration |
| Extension UI reference | Live popup / settings / CSS / ui-messages code only — not extension design-demo HTML |
| Hamburger order | 1 Wallet accounts · 2 Address book · 3 Network · 4 Settings · 5 Connected sites · 6 About this app · 7 Lock wallet |
| Empty menu rows | Wallet accounts, Address book, Connected sites, About this app → open a placeholder screen with a one-line empty state (no lists, no CRUD) |
| Network | **Real** screen (extension `NetworkScreen` pattern): radio **Devnet** / **Mainnet** only; writes `cluster`. **No RPC URL or summary** on this screen |
| Settings | Real hub. Rows: Language, RPC, API keys, AI Settings, Default CU price, **Wallet lock settings** |
| Settings hub RHS | Language → endonym; **RPC → omit**; **API keys → omit**; AI Settings → **On/Off only**; Default CU price → current integer; Wallet lock settings → **omit** (navigate only) |
| Wallet lock settings | One screen: **Idle auto-lock** card (enable switch, default **on**; lock after **minutes**, default **10**); **Wallet lock password** card with **Change** → same change-password flow as before (re-box vault). Persist idle fields in settings JSON; idle timer locks session when enabled (same as menu Lock) |
| Settings behavior | Persist into durable settings (see `how.md`). Mirror extension product fields where they apply on Android; do not copy extension inject / popup layout code |
| Network vs RPC | Same as extension: **Network** = which cluster is active (in-app default / summaries); **Settings → RPC** = per-cluster endpoint lists / active URL only (badge may show which cluster is current; no cluster radio on RPC) |
| MWA submit RPC | **Request chain wins** (continues 0.1.0). Map request chain → cluster; use that cluster’s allowed active custom RPC from settings if set, else that cluster’s public default. Mainnet with empty active → fail before signing. `solana:testnet` / `testnet` → public testnet only (no Network/RPC UI for testnet). Unknown chain → fail before signing. Network `cluster` must **not** redirect submit when it disagrees with the request chain |
| API key fields | Same layout family as extension `SecretKeyField` (standalone bordered input + ghost icons outside). **Idle:** mask `••••••` or empty; eye reveals; pencil edits. **Edit:** check (confirm) + X (discard) **only** — no eye (extension backlog: eye-in-edit is a bug; Android ships the wanted UX). Only one row edits at a time. Never log plaintext |
| AI Settings models | Mirror extension: Default \| Custom per Report / Grading chain. **Default** shows source-hardcoded model list (read-only). **Custom** lets the user edit up to 5 OpenRouter model-id slots. Lists named in `how.md`. Persist only; no assess in 0.2.0 |
| Home chrome | Replace 0.1.0 Home **Lock** text button with icon-only **Menu**; Lock only via hamburger |
| Lock | Menu → Lock wallet clears the in-memory session; Unlock uses extension unlock-screen visual (demo **Unlock**) |
| Theme | Shared tokens from extension `style.css` `:root` (see `how.md`). Apply to top bar, hamburger, Settings, primary buttons. Full visual parity with extension is later |
| Locales | `en` (default), `zh-Hant`, `zh-Hans` — endonyms on the Language screen |
| 0.1.0 features | Burner, vault, empty Assets/Activity tabs, MWA must keep working |
| Copy | English UI strings for 0.2.0 (listed in `how.md`); zh catalogs for the same keys |
| Numbers | Still no `double` for amounts; CU price as integer types named in `how.md` |
| Design demo | [`wallet-020.html`](../../design-demos/wallet-020.html) in **this** repo is the Android visual contract for 0.2.0 |

## Still to decide before implementation

*(empty)*

## Out of scope

| Item | Where |
|------|--------|
| Real Wallet accounts / Address book / Connected sites / About content | Later foundation versions |
| Network beyond cluster pick (custom chains, testnet UI, etc.) | Later |
| Assets balances, in-app transfer, Activity history | Later |
| Multi-account, import seed, seed backup | Later |
| Pixel-perfect extension popup chrome | Later style-align pass |
| Transaction AI assess / signing assist at runtime | Later; 0.2.0 only persists AI settings |
| iOS | Later |
| Tech-debt-only cleanup without the above UI | Later (after basics + style pass) |

## Acceptance

From `android/`:

- `gradlew.bat :app:assembleDebug`
- `gradlew.bat :app:testDebugUnitTest`
- `gradlew.bat :app:lintDebug`

Device: Android Studio emulator on the Windows machine.

### Hand-test

**A. Hamburger**

1. Unlock with an existing 0.1.0 vault + burner.
2. Open the hamburger from Home (Menu icon; no Lock text on the top bar).
3. Order matches the Decided table.
4. Wallet accounts / Address book / Connected sites / About this app each open an empty placeholder (short empty copy, back returns cleanly).
5. Network opens the cluster picker (Devnet / Mainnet) **without RPC text**; changing cluster persists and survives kill + unlock; does not edit RPC URLs.
6. Settings opens the Settings hub with the six rows; RPC and API keys rows have **no RHS summary**; AI row shows **On/Off** only.
7. Lock wallet → Unlock screen; unlock again → same address.

**B. Settings**

8. Language: change locale among `en` / `zh-Hant` / `zh-Hans` → UI strings update; survives process kill + unlock.
9. RPC: set / select / clear per-cluster endpoints as `how.md` (no cluster radio) → survives kill + unlock. With Network on Devnet and a custom Devnet RPC selected, a Devnet sign-and-send uses that custom URL. Mainnet with no active RPC: mainnet sign-and-send fails before signing. Reject disallowed custom URLs (non-https except localhost http). Destination / amount / fee / instruction name still shown as in 0.1.0.
10. API keys: Helius / Jupiter — idle masked + eye + pencil; edit shows confirm + discard only (no eye); no plaintext in logs.
11. AI Settings: toggle + OpenRouter key; Report / Grading chains — Default shows hardcoded list (not editable), Custom allows editing slots; survives kill + unlock; AI never signs; no assess required in 0.2.0.
12. Default CU price: save a non-negative integer; survives kill + unlock.
13. Wallet lock settings: idle auto-lock defaults on / 10 minutes; changing minutes persists; **Wallet lock password → Change** re-boxes vault (min 8); wrong current fails; new password unlocks after kill + unlock.

**C. Theme**

14. Primary actions and accents use `#3d9cf0` family; screens are dark-themed (`#0f1419` / elevated surfaces), not default Material purple-only.

**D. Regression**

15. Assets still `No assets yet.` Activity still `No activity yet.`
16. Generate burner path and MWA authorize still work (smoke). One Devnet sign-and-send smoke after RPC hand-test 9 still completes (or fails cleanly on mainnet-unset when that path is used).

**E. Privacy**

17. No password, seed, or private key in logs or tracked docs.

## Design review gate

Status stays `draft` until [`docs/design-review.md`](./docs/design-review.md) passes per [`agent-workflow.md`](../agent-workflow.md). Do not implement from chat alone.
