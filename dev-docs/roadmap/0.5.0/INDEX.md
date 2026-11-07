# 0.5.0 — MWA connect page

- **狀態：** `shipped`（2026-11-07）
- **Changelog：** [`changelog.md`](../../../changelog.md) heading `0.5.0 — 2026-11-07`
- **上一版：** [0.4.0](../0.4.0/INDEX.md)
- **對齊現況：** `solana-wallet:` opens `MwaActivity`. A locked vault stays on `UnlockScreen` in that activity until unlock, then Connect shows the dApp name, URI, Connect, and Reject. The walletlib auth database is kept so a later `auth_token` can reauthorize after unlock. `sign_messages`, `sign_and_send_transactions`, and `sign_transactions` decline and are not in `get_capabilities`. Hand test is the official Solana Mobile playground. This repo does not ship a test dApp. The `connect-dapp` phone frame did not ship.
- **Design demo：** none for the screen that shipped. [`wallet-overall-v1.html`](../../design-demos/wallet-overall-v1.html) `connect-dapp` is not this version.

## Product

A dApp can **connect**. If the vault is locked, `MwaActivity` shows `UnlockScreen` and keeps the session until the user unlocks, then shows Connect. Signing a message is [0.6.0](../0.6.0/INDEX.md). Signing a transaction is not scheduled. The hand-test client is the official Solana Mobile playground (https://docs.solanamobile.com/cli/playground). Host: `bun run playground` at the repo root (`adb` must be on PATH). The CLI reverse-forwards; the emulator Chrome opens `http://localhost:4747`. Do not use `10.0.2.2`. The protocol stays the 0.1.0 MWA wallet endpoint, not the Chrome extension Wallet Standard.

## Read order

1. This file
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)
4. [0.1.0 workflow MWA section](../0.1.0/docs/workflow.md) (session lifecycle; the screen is this file)
5. Code: `MwaActivity.kt`, `mwa/MwaHostViewModel.kt`, `mwa/MwaSession.kt`, `mwa/MwaOffers.kt`

## Decided

| Topic | Decision |
|-------|----------|
| Protocol | Continue 0.1.0: `solana-wallet:` local associate plus localhost WebSocket. This version completes `authorize`, `deauthorize`, and `get_capabilities`. `sign_messages`, `sign_and_send_transactions`, and `sign_transactions` decline immediately and are not listed in `get_capabilities`. No SIWS, remote / QR, `clone_authorization`, or custom deeplink |
| Account | The address returned by connect is the signable account for `activeAccountId`. `watch`: fail before `offer`, string `mwa.watchAccount`, no approval (0.4.0) |
| Operation | One in-memory `WalletOperationRequest` on `WalletModel.walletOperationRequest`. Connect uses kind `Authorize`. A second request fails and does not replace the first. Lock and process death fail it. The result returns only to that request. `MwaActivity` draws `MwaSession.step` (`MwaHostViewModel.screen.step`): `UnlockConnect` or a locked `Waiting` shows `UnlockScreen`, then `ConnectApproval` |
| Hand-test client | Official playground, not in this repo. Host: `bun run playground`. Device browser `http://localhost:4747`. Connect opens Airwave. Sign message, Sign and send, and SIWS / Sign in fail and do not open an error approval. `http://10.0.2.2:…` is not a secure context. This is not the extension `test-web`. This version deletes `mwa-test-web/` |
| Connect screen | Full screen on `MwaActivity`, composable `ConnectApproval`. Locked vault: `UnlockScreen` until unlock. Then the dApp identity name, identity URI, Connect (`mwa.connect.action`), and Reject (`mwa.reject`). No back glyph and no lock glyph on this screen. Connect authorizes the active signable account. A watch account is `WatchBlocked` with `mwa.watchAccount` until teardown |
| Copy keys (en / zh-Hant / zh-Hans) | `mwa.connect.title` Connect / 連線 / 连线. `mwa.connect.action` Connect / 連線 / 连线. `mwa.reject` Reject / 拒絕 / 拒绝. `menu.back` / `menu.lock` stay as in 0.2.0 |
| Glyphs | New vector XML this version. `d` copied from the phone frame or web `StrokeIcon`. No Material Icons, no Compose `Canvas` invention, no PNG for these strokes: `ic_stroke_back`, `ic_stroke_lock`, `ic_stroke_key` |
| MWA Activity | Dedicated `MwaActivity`, `singleTask`, `taskAffinity=com.airwave.wallet.mwa`. `MwaHostViewModel` owns the `LocalScenario`. `Waiting` with an unlocked vault is the associate spinner (`AssociateWait`) on `AirwaveColors.Bg`. The starting-window background is the same dark color. After approve, chrome stays until teardown. `completeWith*` does not `finish()`. Serving complete closes the Scenario on the main thread. Teardown sets `MwaStep.Finished`, and the Activity `finish()`es. Reauthorize unlocks when needed, then completes with no approval page and does not `finish()`. Do not bring `MainActivity` forward. `WalletModel` is shared through `AirwaveApp` |
| File shape | `MwaActivity` stays in `com.airwave.wallet` and draws `ConnectApproval`. The session host is `com.airwave.wallet.mwa`: `MwaHostViewModel` owns the Scenario, `MwaSession` owns `MwaStep`, `MwaOffers` builds the authorize request. `WalletModel.kt` is not split. New `.kt` files stay at or under 400 code lines. `AppStrings.kt` may exceed that |
| 0.4.0 features | Multi-account, holdings, menu, and settings stay. This version only replaces the connect screen and the hand-test entry |

## Still to decide before implementation

(empty)

## Not this version

| Item | Where it goes |
|------|----------------|
| `connect-dapp` phone frame (letter tile, back, lock glyph, account row) | Not shipped |
| `sign_messages` | [0.6.0](../0.6.0/INDEX.md) |
| `sign_and_send_transactions` and `sign_transactions` | Unscheduled. Both decline |
| Live AI, report overlay, `approve-sign-tx` | Unscheduled |
| Connected sites, remote associate, SIWS | Menu row stays the 0.2.0 shell. SIWS is a different method from `sign_messages` |

## Acceptance

From `android/`, set `JAVA_HOME` to the Android Studio JBR (this machine: `C:\Program Files\Android\Android Studio\jbr`):

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

One Android emulator. At the repo root, `bun run playground` (adds `platform-tools` to PATH, then starts the official playground). Emulator opens `http://localhost:4747` (not `10.0.2.2`; host Windows Chrome cannot complete local associate).

1. Unlock a signable account (not watch), or unlock on the connect flow when the vault is locked. Connect shows the dApp name, URI, Connect, and Reject. After approve, the page log shows connected and the public key.
2. Playground Sign message and Sign and send fail that request and do not open an approval.
3. Active account is watch. Connect fails, no approval, `mwa.watchAccount` is visible.
4. Kill the app during connect. That request fails. Unlock again: no leftover approval. A later Connect that still has an `auth_token` waits on `UnlockScreen` when the vault is locked, then reauthorizes.

Unit tests: `offer` rejects a second pending; watch fails before pending. Do not write a mnemonic or a private key into a test.
