# 0.9.0 — how

Session ownership stays as in [0.7.0 how](../../0.7.0/docs/how.md). This file adds `SignAndSend` and the submit step. It does not change `sign_transactions`.

## Track A — Offer and capability

`onSignAndSendTransactionsRequest` calls `MwaSession.offerSignAndSend`. Empty, missing, multiple, or undecodable payloads call `completeWithDecline()` and do not `offer`. Unknown chain, testnet, and a null `rpcUrlForSubmit` decline the same way.

A watch account does not `offer`, and `MwaActivity` shows `mwa.watchAccount` until teardown. This request has no `addresses` array. Do not add an address-list check. A locked vault uses step `UnlockSignAndSend` and keeps the protocol request on `MwaSession`.

The request stores the original `ByteArray`, the version tag, the submit URL, and the chain cluster. It may store a working copy and, after Approve, the signed bytes. None of those are written to disk or preferences.

Add `solana:signAndSendTransaction` to `MobileWalletAdapterConfig` optional features. Leave `solana:signTransactions` in place.

**Check:** unit tests for empty, multi-payload, unknown version, unknown chain, testnet, and mainnet with no RPC. A watch account does not offer.

## Track B — Sign and submit

Budget edit, simulation, and partial sign are the 0.7.0 rules. When the request cluster is not the selected cluster, show the mismatch screen before simulation. The body is `mwa.signSend.clusterMismatch`. Hide changes, fees, and instructions. Leave Approve disabled. The blockhash-only mismatch still uses `mwa.signTx.mismatchBody`.

Approve builds the signed bytes in memory, then disables Apply for the rest of this request. Refresh re-simulates those signed bytes and does not build a new working copy.

Extend `SolanaJsonRpc.sendTransaction`. Do not add an HTTP client. The config object sets `encoding` to base64 and `skipPreflight` to false. It does not copy `commitment`, the request's `skipPreflight`, `maxRetries`, or `waitForCommitmentToSendNextTransaction`. When `getMinContextSlot()` is non-null, put that integer on the config. Connect timeout and read timeout are 15 seconds. A JSON-RPC `error.message` is kept as the short reason on the in-memory request. A timeout or a body with no message still shows the failure block and no reason line.

Compare the RPC signature string with this account's 64-byte slot using the existing base58 decoder. A match calls `completeWithSignatures` with that one array. Anything else stores the send-failure on the in-memory request and leaves the protocol request open. A second Approve sends the same signed bytes and does not partial-sign again.

Reject, system back, and process death decline that request only. Teardown cancels an in-flight send and ignores a late result.

**Check:** unit tests that a matching 64-byte slot would complete and a different decoding would not. No live RPC. No secret in the test.

## Track C — Screen

`MwaActivity` composes the 0.7.0 approval when the step is `SignAndSendApproval`. The title key is `mwa.signSend.title`. Sending and send-failure copy are the INDEX copy row. Do not draw an AI card. Do not add a vector XML file.

**Check:** INDEX acceptance 1 through 4 on one emulator, using `bun run playground` and Sign and send on devnet.

## Commands

From `android/`, `JAVA_HOME` set to the Android Studio JBR:

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```
