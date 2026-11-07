# 0.6.0 — how

`MwaActivity` stays in `com.airwave.wallet`. The session host lives in `com.airwave.wallet.mwa`: `MwaHostViewModel` owns the Scenario, `MwaSession` owns the screen step, and `MwaOffers` builds each `WalletOperationRequest`. This file turns on `sign_messages`.

## Track A — Offer the message

`onSignMessagesRequest` calls `MwaSession.offerSignMessage`. That uses `MwaOffers.signMessage` to build one in-memory `WalletOperationRequest` of kind `SignMessage` and passes it to `WalletModel.offer`. It does not write the payload to disk or to preferences. `MwaOffers` completes only that request.

Before `offer`:

- `MwaPayloads.declineBeforeOffer`: empty, missing, or more than one payload calls `completeWithDecline()` and does not build a request.
- Active account is watch: do not offer. The step is `WatchBlocked` with `mwa.watchAccount` until teardown.
- `addresses` is null or empty: sign with the active public key. Otherwise `MwaOffers.addressesAllow` calls `completeWithAuthorizationNotValid()` unless the list has exactly one element and that element equals the active public key bytes. When the vault is locked, this check runs after unlock, against `model.address`.
- Vault missing, or locked with no vault file: decline, same as connect. Locked with a vault file: step `UnlockSignMessage`, and the protocol request stays on `MwaSession` until unlock.

Display strings come from `MwaPayloads.display`. The request keeps the raw `ByteArray` inside `approve`, not only `messageUtf8` / `messageHex`.

`approve` signs that array with the unlock seed on `Dispatchers.IO` and returns `ApprovalResult.Bytes`. The host calls the walletlib signed-messages completion with that one signature. It does not log the seed. Reject, lock, and process death call decline on that request only.

`onSignAndSendTransactionsRequest` and `onSignTransactionsRequest` stay `completeWithDecline()`.

**Check:** unit tests for display, empty, multi-payload, and address mismatch. INDEX acceptance 2 and 4.

## Track B — Capability and screen

`MobileWalletAdapterConfig` optional features include `solana:signMessages` and no sign-and-send or sign-transactions feature.

`MwaActivity` composes `SignMessageBody` when the step is `SignMessageApproval`, and `UnlockScreen` when the step is `UnlockSignMessage` or the vault is locked on `Waiting`. Do not leave the user on `AssociateWait` for this request, and do not show this approval on `MainActivity`. Center `mwa.signMessage.title` between back and lock. The site row is one line: the `mwa.site` label plus the URI, or the name when the URI is empty. Do not add a letter tile, an AI card, or a second scroll section. Copy keys in the INDEX go into `AppStrings` if a locale string is still missing.

Approve and reject keep that activity’s chrome until teardown. Do not end this flow by setting `route = Home`. `completeWith*` does not `finish()` the Activity. Teardown still closes the session.

**Check:** INDEX acceptance 1 and 3 on one emulator with `bun run playground`.

## Commands

From `android/`, `JAVA_HOME` set to the Android Studio JBR:

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## File shape

Do not add a new Activity. Do not put walletlib callbacks into `ui/MwaScreens.kt`. New or edited `.kt` files stay at or under 400 code lines except `AppStrings.kt`. Named types keep English declaration KDoc.
