# 0.6.0 — MWA sign message

- **Status:** `shipped` (2026-11-08). Matches the roadmap table. Changelog: [`changelog.md`](../../../changelog.md) heading `0.6.0 — 2026-11-08`.
- **Previous:** [0.5.0](../0.5.0/INDEX.md) (connect page only)
- **Design demo:** no separate phone frame. The screen is the Decided sentences below. Do not reuse `approve-sign-tx`.

## Product

A connected dApp can ask the user to **sign one message** through Mobile Wallet Adapter `sign_messages`. The wallet shows the bytes and returns one ed25519 signature over those bytes. It does not send a transaction.

## Read order

1. This file
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)
4. [0.5.0 INDEX](../0.5.0/INDEX.md) (connect chrome, pending, Activity)
5. [0.1.0 workflow MWA section](../0.1.0/docs/workflow.md) (session lifecycle only)

## Decided

| Topic | Decision |
|-------|----------|
| Method | One method: `sign_messages` (`solana:signMessages`). This version adds that feature to `get_capabilities` by passing it in `MobileWalletAdapterConfig` optional features. `sign_and_send_transactions` and `sign_transactions` still decline immediately and stay out of `get_capabilities`. SIWS / `sign_in` stays out |
| Payloads | Sign the first payload only when `payloads` has exactly one element and that element is non-empty. Empty `payloads`, a missing first payload, an empty first payload, or more than one payload: do not `offer`; call `completeWithDecline()` immediately. Store the payload as `ByteArray`. Do not use `decodeToString()` as the only path. The signature is ed25519 over those original bytes, not over a re-encoded display string |
| Display | Use `MwaPayloads.display`: UTF-8 when a strict decoder accepts the bytes, otherwise `mwa.message.hex` plus lowercase hex. Do not detect a preamble, do not parse an off-chain header, and do not add a second screen |
| Screen | `MwaActivity` draws `SignMessageBody` when `MwaSession.step` is `SignMessageApproval`. It does not leave the user on `AssociateWait`, and it does not show this approval on `MainActivity`. Top row: back glyph (`ic_stroke_back`, content description `menu.back`) calls `reject()`, title `mwa.signMessage.title` centered between back and lock, lock glyph (`ic_stroke_lock`, content description `menu.lock`) calls `lock()`. No letter tile and no AI card. Scrollable middle, in order: one line that is the label `mwa.site` plus the identity URI, or the identity name when the URI is empty; the account short address; the message body (UTF-8 text, or the binary label plus hex). Sticky bottom: `mwa.reject` then `mwa.approve`. No new vector XML. Connect stays `ConnectApproval`: name, URI, Connect, and Reject, with no back or lock |
| Locked vault | Same as 0.5.0 connect: if the vault is locked, show `UnlockScreen` on `MwaActivity` first, then this screen. The request stays in memory. Process death fails it |
| Account | The signature and the displayed address are the signable account for `activeAccountId`. A `watch` account fails before `offer`, does not open this screen, and `MwaActivity` shows the one sentence `mwa.watchAccount` until teardown. A non-null, non-empty `addresses` list is `completeWithAuthorizationNotValid()` and does not `offer`, unless the list has exactly one element and that element equals the active public key bytes. When `addresses` is null or empty, sign with the active public key |
| Workflow | The business object is `WalletOperationRequest` (`WalletModel.walletOperationRequest`). Kinds are `Authorize`, `Reauthorize`, and `SignMessage`. `MwaActivity` reads `MwaHostViewModel.screen.step` (`MwaStep`): `UnlockSignMessage` then `SignMessageApproval`. `MwaOffers.signMessage` builds the request. `Reauthorize` unlocks when needed and then completes with no approval page. At most one operation. A second request fails and does not replace the first. Lock and process death fail it. The result returns only to that request. Approve and reject keep chrome until teardown. `completeWith*` does not `finish()` the Activity |
| Copy keys (en / zh-Hant / zh-Hans) | `mwa.signMessage.title` Sign message / 簽署訊息 / 签署讯息. `mwa.approve` Approve / 批准 / 批准. `mwa.site` Site / 站點 / 站点. `mwa.message.hex` Binary message / 二進位訊息 / 二进制讯息. `mwa.reject`, `menu.back`, and `menu.lock` stay as in 0.5.0 |
| Not this version | Solana off-chain message as its own path. `sign_and_send_transactions` and `sign_transactions` still decline and stay out of `get_capabilities`. No new Activity |
| File shape | `WalletModel.kt` stays one file. It is already over 400 code lines. This version adds the sign-message branch in offer, unlock, approve, and reject, and does not split that file. The host is `com.airwave.wallet.mwa`: `MwaHostViewModel` owns the Scenario, `MwaSession` owns `MwaStep`, `MwaOffers` builds each `WalletOperationRequest`, `MwaPayloads` decides display and decline-before-offer. `MwaActivity` stays in `com.airwave.wallet` and reads `screen.step` |

## Still to decide before implementation

(empty)

## Not this version

| Item | Where it goes |
|------|----------------|
| Off-chain message preamble or header parsing | Unscheduled |
| `sign_and_send_transactions` and `sign_transactions` | Unscheduled. Both decline |
| SIWS / `sign_in` | Unscheduled |
| `approve-sign-tx` phone frame and AI card | Unscheduled |
| A new Activity or a second pending store | Not this version |

## Acceptance

From `android/`, set `JAVA_HOME` to the Android Studio JBR (this machine: `C:\Program Files\Android\Android Studio\jbr`):

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

One Android emulator. At the repo root, `bun run playground`. Emulator opens `http://localhost:4747` after 0.5.0 Connect has succeeded for a signable account.

1. Playground Sign message opens this screen: back, title Sign message, lock, site row, account short address, and the message text the page sent. Approve: playground reports success for that request. Reject: only that request fails; the account and vault are unchanged. Sign and send still does not open an approval.
2. A non-UTF-8 payload shows the binary label and lowercase hex, and the signature is over the original bytes.
3. Lock on this screen fails the request. After unlock, no pending remains. Kill the app during this screen: that request fails and does not return after the next unlock.
4. A watch account, an empty payload, or more than one payload does not open this screen. For a watch account, `MwaActivity` shows `mwa.watchAccount` until teardown.

Unit tests cover `MwaPayloads.display` (UTF-8 versus hex), empty and multi-payload decline-before-offer, and address mismatch before offer. Do not write a mnemonic or a private key into a test.
