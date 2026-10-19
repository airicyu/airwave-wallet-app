# 0.2.0 — reasoning

## What ships

Navigation chrome and settings foundation for the native Android wallet, plus the first pass of Airwave blue theme tokens aligned with the Chrome extension.

- Hamburger menu matching the extension order (Wallet accounts → Address book → Network → Settings → Connected sites → About this app → Lock wallet).
- **Network** screen: Devnet / Mainnet cluster pick (extension `NetworkScreen`).
- Empty destinations for accounts / address book / connected sites / about (IA reserved; products later).
- Working Settings hub: Language, RPC (per-cluster endpoints only), API keys / OpenRouter with `SecretKeyField` UX, AI Settings (Default = hardcoded chains / Custom = user slots; persist only), Default CU price, Wallet password.
- Lock from the menu (Home Lock text control removed).
- Design demo: [`dev-docs/design-demos/wallet-020.html`](../../../design-demos/wallet-020.html).

## Why

0.1.0 proved custody + MWA. The next product phase is **basic foundation pages**. Menu + Settings are the shared shell every later screen hangs from. Empty rows reserve places without scheduling full features.

The Android app is meant to **mirror the extension product** (`airicyu/airwave-wallet`): same menu order, same Settings field set, same blue accent family (`#3d9cf0` / `#2a6ea8`). Theme alignment starts now so later pages do not paint twice. Layout is phone Material, not a pixel clone of the popup.

**Reference the live popup / settings / CSS / ui-messages code.** That repo’s `dev-docs/design-demos/` are only occasional sketches; many are stale — do not treat them as current UI.

**MWA submit:** keep 0.1.0 “request chain selects the cluster”; settings only supply optional custom RPC URLs for that cluster. Network’s `cluster` is not a silent override of a mismatched dApp chain (design-review H1 → scheme A).

## What was dropped

| Dropped | Why |
|--------|-----|
| Filling empty menu destinations (accounts / address book / sites / about) | Separate foundation versions |
| Putting cluster pick on the RPC screen | Extension keeps Network vs RPC split; Android matches that |
| Running transaction AI assess | Settings persist only; AI stays read-only and never signs |
| Balances / Activity / transfer | Different axis; after shell |
| Full extension visual / popup chrome parity | Explicit later style pass |
| Copying extension TypeScript / CSS | Wrong platform; mirror product + tokens only |
| Extension-only shell / sidebar / rpcGuideDismissed | Not applicable on Android |

## Dependencies

Settings persistence uses EncryptedSharedPreferences (AndroidX Security family already used for the vault file wrap). No new network SDK beyond 0.1.0 RPC submit.

## Design demo

[`wallet-020.html`](../../../design-demos/wallet-020.html) (this repo) is the Android visual contract for hamburger, placeholders, Settings hub/subpages, and tokens. It was drawn from live extension popup/settings/CSS/messages — not from that repo’s design-demo folder. Compose adapts spacing to phone dp.
