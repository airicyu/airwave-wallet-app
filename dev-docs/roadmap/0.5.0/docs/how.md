# 0.5.0 — how

Connect only. Package `com.airwave.wallet` for the Activity. Session types live in `com.airwave.wallet.mwa`.

## Track A — Connect screen

`MwaActivity` reads `MwaHostViewModel.screen.step` and does not use `Route.Approval`.

| Step | What it draws |
|------|----------------|
| `Waiting`, vault locked | `UnlockScreen` |
| `Waiting`, vault unlocked | `AssociateWait` (spinner on `AirwaveColors.Bg`) |
| `UnlockConnect` | `UnlockScreen` |
| `ConnectApproval` | Identity name, identity URI, Connect, Reject |
| `WatchBlocked` | The one sentence `mwa.watchAccount` |
| `Finished` | `finish()` the Activity |

`ConnectApproval` calls `WalletModel.approve` and `reject`. It does not keep a second copy of the request.

**Check:** INDEX acceptance 1 and 3.

## Track B — Association

`solana-wallet:` is only `MwaActivity`. `MainActivity` does not register that intent. Both Activities read one `WalletModel` from `AirwaveApp`.

`MwaHostViewModel.processLaunch` parses a `LocalAssociationUri` and calls `createScenario(...).also { it.start() }`. The callbacks implement `LocalScenario.Callbacks`, including `onLowPowerAndNoConnection`. An invalid intent `finish()`es the Activity. Replacing a scenario calls `MwaSession.abandonOperation`, sets the teardown flag, and `close()`s the previous scenario so that teardown does not `finish()` the new one.

`onAuthorizeRequest` goes to `MwaSession.offerAuthorize`, which uses `MwaOffers.authorize` and `WalletModel.offer`. `onSignTransactionsRequest` and `onSignAndSendTransactionsRequest` call `completeWithDecline()` and do not `offer`. `sign_messages` is [0.6.0](../../0.6.0/INDEX.md).

`offer` keeps one `WalletOperationRequest`. A second offer or a watch account finishes `Rejected` and does not store it. A watch account sets `MwaStep.WatchBlocked`. No vault, or locked with no unlock path: decline. Locked with a vault file: `UnlockConnect`, then `ConnectApproval` after unlock.

Approve and reject set `approvalSettled` and leave the connect chrome until teardown. `completeWith*` does not `finish()`. Serving complete `close()`s the Scenario on the main thread. Teardown sets `MwaStep.Finished`. Low power does not `finish()` the Activity.

Reauthorize: `MwaOffers.reauthorize`. Locked: `UnlockReauthorize`. Unlocked, or after unlock: complete with reauthorize and return to `Waiting`. No approval page.

**Check:** INDEX acceptance 2 and 4.

## Track C — Playground

Root `package.json` script `playground` starts the official Solana Mobile playground and adds `platform-tools` to PATH when `adb` is missing. This repo does not ship a test dApp. `mwa-test-web/` is removed.

**Check:** INDEX device steps 1–3.

## File shape

`MwaActivity` does not hold walletlib callbacks. Callbacks stay on `MwaHostViewModel`. Request building stays on `MwaOffers`. Named types keep English declaration KDoc. New `.kt` files stay at or under 400 code lines. `AppStrings.kt` may exceed that.
