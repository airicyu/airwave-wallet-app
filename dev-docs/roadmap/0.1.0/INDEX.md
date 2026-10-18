# 0.1.0 — MVP

- **Status:** `shipped`
- **Parent:** (none)
- **Changelog:** [`changelog.md`](../../../changelog.md) under `0.1.0 (unreleased)`

## Product (agreed)

Only this scope is decided:

1. **Generate burner wallet** — after the app password exists, the user generates one new keypair. No import. The password is **app-level**, not entered on the generate screen.
2. **Wallet home** — includes a **wallet widget** on the home surface.
3. **Assets** and **Activity** — bottom (or equivalent) tabs exist; **no tab content yet** (empty shells).
4. **MWA wallet endpoint** — dApps associate, authorize (connect), and request sign / sign-and-send through the Solana Mobile spec. No custom deeplink protocol.

Not the Chrome extension inject / Wallet Standard stack.

## Read order

1. This file
2. [`docs/workflow.md`](./docs/workflow.md) — app paths (before visual design)
3. [`dev-docs/design-demos/wallet-010.html`](../../design-demos/wallet-010.html) — design demo
4. [`docs/reasoning.md`](./docs/reasoning.md) and [`docs/how.md`](./docs/how.md)
5. App code under `android/` (after design review, when scaffolded)

## Decided

| Topic | Decision |
|-------|----------|
| Custody | One **app password** (at least 8 characters) locks the vault. Creating a burner does not ask for another password |
| Application id | `com.airwave.wallet` |
| Store | Single durable blob `airwave.vault.v1` (see `how.md`). Session and pending MWA approvals stay in memory |
| Copy | English strings in `how.md` are normative for 0.1.0 |
| `sign_transactions` | Not implemented. That request fails immediately and is not listed in `get_capabilities` |
| Submit RPC | Public Solana cluster URLs in `how.md`. No RPC settings screen in 0.1.0 |
| Numbers | Lamports and fees are `BigInteger` from strings. No `double` |
| Platform | **Native Android only** for 0.1.0 acceptance and ship |
| UI | **Jetpack Compose** + Material 3 (details in `how.md`) |
| iOS / desktop / web | Out of scope for 0.1.0 |
| dApp protocol | **Mobile Wallet Adapter** wallet endpoint ([spec 2.0](https://solana-mobile.github.io/mobile-wallet-adapter/spec/spec.html)): local `solana-wallet:/v1/associate/local` + localhost WebSocket. **No** Phantom/Solflare-style custom scheme |
| MWA methods in workflow | `authorize`, `deauthorize`, `get_capabilities`, `sign_messages`, `sign_and_send_transactions`. SIWS and remote/QR association out of 0.1.0 |
| App workflow | [`docs/workflow.md`](./docs/workflow.md) |
| Design demo | [`dev-docs/design-demos/wallet-010.html`](../../design-demos/wallet-010.html) |
| Next steps | Tech design review → scaffold `android/` (do not scaffold before review passes) |

## Still to decide

*(empty)*

## Out of scope

Import of a seed or private key, seed backup UI, account name, balances, transfers from Home, activity history, multi-account, iOS, a proprietary deeplink, remote/QR/BLE/Nostr association, Sign In With Solana, `clone_authorization`, deprecated `sign_transactions`, an RPC settings screen.

## Acceptance

Device: one Android phone or emulator. Commands from `android/`:

- `./gradlew :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:lintDebug`

### Hand-test

**A. App password and burner** — Fresh install. Two different passwords stay on setup and show `Passwords do not match.` A password shorter than 8 characters shows `Password is too short.` Then set a valid password twice → `No wallet account yet.` → Create burner wallet. The preview shows a short address; Copy puts the full preview address on the clipboard. Roll again changes the preview. Generate → Home shows that address on Assets. Copy on Home puts the full address on the clipboard. Lock → Unlock with the same password shows the same address. Wrong password stays on Unlock with `Wrong password.` Kill the app → Unlock → same address.

**B. Empty tabs** — Assets shows `No assets yet.` Activity shows `No activity yet.` The wallet widget stays on both tabs.

**C. MWA** — Cold start with no vault, or an unlocked vault with no account: an associate request fails and is not completed after the user later creates an account. With an account: an MWA client associates with `solana-wallet`. Authorize → Approve → client receives this account. A later authorize → Reject fails only that request. `deauthorize` has no extra screen and that `auth_token` cannot skip approval afterward. Sign message shows the message; Approve returns a signature. Sign-and-send of one system transfer shows destination, amount, fee, and the instruction name; Approve submits via the public RPC for that chain. A sign-and-send whose chain is unknown fails before signing and does not submit. `sign_transactions` fails without a hang. Lock on an approval fails that request and the approval screen is gone. Kill the app during an approval; after relaunch that request is not waiting. A second request while one approval is open fails only the second.

**D. Privacy** — Logs and tracked docs contain no secret key and no password.

## Design review gate

Order: **workflow** → **design demo** ([`wallet-010.html`](../../design-demos/wallet-010.html)) → **tech design review** ([`docs/design-review.md`](./docs/design-review.md) per [`agent-workflow.md`](../agent-workflow.md)) → set **`in progress`** → **scaffold** `android/` and implement.

Stay **`draft`** until design review passes. Do not scaffold before that.
