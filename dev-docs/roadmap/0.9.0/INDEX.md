# 0.9.0 — MWA sign and send

- **Status:** `shipped` (2026-11-16). Matches the roadmap table. Changelog: [`changelog.md`](../../../changelog.md) heading `0.9.0 — 2026-11-16`.
- **Previous:** [0.7.0](../0.7.0/INDEX.md) (`sign_transactions`, shipped). [0.8.0](../0.8.0/INDEX.md) is also shipped and does not change this method.
- **Design demo:** the same phone frames as 0.7.0 in [`wallet-overall-v1.html`](../../design-demos/wallet-overall-v1.html): `approve-sign-tx`, `approve-sign-tx-v1`, `approve-sign-tx-fail`, and `approve-sign-mismatch`. This version does not draw the AI card those frames include.

## Product

A connected dApp can ask the wallet to **sign one transaction and submit it** through Mobile Wallet Adapter `sign_and_send_transactions`. The approval screen is the 0.7.0 sign-transaction screen. Approve partial-signs, calls `sendTransaction`, and returns one signature to that request. `sign_transactions` still returns signed bytes and does not submit.

## Read order

1. This file
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)
4. [0.7.0 INDEX](../0.7.0/INDEX.md) (decode, budget, simulation, screen, one pending request)

## Decided

| Topic | Decision |
|-------|----------|
| Method | One new method: `sign_and_send_transactions` (`solana:signAndSendTransaction`). Advertise it in `get_capabilities` next to the existing `solana:signTransactions`. `sign_transactions` keeps the 0.7.0 behavior and does not call `sendTransaction`. `sign_messages` is unchanged |
| Payload | Same gate as 0.7.0. Exactly one non-empty payload. Empty, missing, or more than one: `completeWithDecline()` and do not `offer`. Bytes that do not decode as legacy, v0, or v1 decline the same way |
| Chain | Submit uses `RpcResolver.rpcUrlForSubmit` on the request chain, not the wallet's selected cluster. Unknown chain, or mainnet with no allowed active RPC, declines before `offer`. `solana:testnet` and `testnet` also decline before `offer`, because this wallet has no testnet cluster. Devnet and mainnet open the screen when that function returns a URL |
| Cluster match | When the request chain maps to mainnet or devnet and that cluster is not the wallet's selected cluster, the screen uses the mismatch title, hides changes, fees, and instructions, and disables Approve. Do not call `sendTransaction`. Its body is `mwa.signSend.clusterMismatch` and states the wallet cluster and the request chain. It does not claim the blockhash is valid. A Devnet strip still shows when the wallet cluster is devnet, and no strip shows on mainnet. A blockhash that is valid only on the other of mainnet and devnet still uses `mwa.signTx.mismatchBody` |
| Account | Same as 0.7.0. A watch account does not `offer` and `MwaActivity` shows `mwa.watchAccount` until teardown. Approve partial-signs: fill this account's empty slot and leave every other slot as it arrived. When this account's slot is already non-zero, leave it unchanged |
| Submit | After a successful partial sign, `sendTransaction` posts the signed bytes to the submit URL. `encoding` is base64. `skipPreflight` is false. The config does not copy `commitment`, the request's `skipPreflight`, `maxRetries`, or `waitForCommitmentToSendNextTransaction`. When `getMinContextSlot()` is non-null, copy that integer onto the config object; when it is null, omit the field. Do not wait for confirmation. Connect timeout and read timeout are 15 seconds. The signed bytes and the RPC URL stay in memory on this request. They are not written to disk or preferences. After that partial sign, Apply is disabled for the rest of this request. Refresh re-simulates those signed bytes and does not build a new working copy. A later Approve only calls `sendTransaction` with those bytes |
| Result | A non-empty RPC signature string is success only when its base58 decoding is the 64 bytes now in this account's signature slot. Then `completeWithSignatures` returns that one 64-byte array to this request only. A null RPC result, a JSON-RPC error, a timeout, or a signature that does not match the slot does not complete the request. The screen shows the send-failure block. When the JSON-RPC error has a `message` string, that string is the short reason. A timeout or a response with no message shows the failure title and no reason line. Approve stays enabled so the same signed bytes can be sent again. Reject and system back decline only this request. Process death fails it |
| Screen | Same blocks, order, glyphs, and fee editor as the 0.7.0 screen row. No back control, no lock control, no AI card. The centered title is `mwa.signSend.title` instead of `mwa.signTx.title`. Sticky bottom stays Reject then Approve (`mwa.reject`, `mwa.approve`). While `sendTransaction` is in flight, both buttons are disabled and the changes area shows `mwa.signSend.sending` instead of delta rows. A send failure uses the 0.7.0 failure block inside the changes card: title `mwa.signSend.sendFail`, the short RPC reason when one exists, the same details disclosure, and `mwa.signTx.failHint`. The disclosure body is that same message when one exists, and is empty when there is no message. Do not read `error.data`. Refresh still re-runs simulation and does not send. System back rejects |
| Locked vault | Same as 0.7.0: `UnlockScreen` on `MwaActivity` first. The request stays in memory. Process death fails it. When the unlock screen appears, the password field takes focus and the keyboard opens |
| Unlock focus | The shell unlock screen and the MWA unlock screen are the same `UnlockScreen`. The password field requests focus when that screen is composed, and the software keyboard is shown. A failed password leaves the field focused |
| Workflow | One in-memory `WalletOperationRequest` of kind `SignAndSend`. A second MWA request fails and does not replace the first. `completeWith*` does not `finish()` the Activity. The legacy-only `WalletModel.signAndSend` helper is not this path |
| Copy keys (en / zh-Hant / zh-Hans) | `mwa.signSend.title` Sign and send / 簽署並送出 / 签署并送出. `mwa.signSend.sending` Sending / 送出中 / 送出中. `mwa.signSend.sendFail` Could not send the transaction / 無法送出交易 / 无法送出交易. `mwa.signSend.clusterMismatch` states the wallet cluster and the request chain: This wallet is on {0}. The request is for {1}. / 此錢包在 {0}。請求要送往 {1}。 / 此钱包在 {0}。请求要送往 {1}。 `{0}` is Mainnet or Devnet. `{1}` is the same two names. Every other string on this screen stays on the 0.7.0 keys |
| Files | `AppStrings.kt` is the message catalog. This version adds the keys above and does not split that file. `SignTxWire.kt` is already over 400 lines. This version adds `signerSlot` there and does not split it, because that read is the same wire format the file already parses. Do not add a second submit client. Extend `SolanaJsonRpc.sendTransaction` |
| Not this version | AI analysis. Waiting for confirmation. A settings row for the default priority fee. Submitting from `sign_transactions`. Testnet as a wallet cluster. A new Activity |

## Still to decide before implementation

(empty)

## Not this version

| Item | Where it goes |
|------|----------------|
| AI card and report | Unscheduled |
| `sign_transactions` submitting | Not this version. That method still returns signed bytes |
| Confirmation wait after send | Not this version. The dApp receives the signature when `sendTransaction` returns |
| Testnet cluster in the wallet | Not this version. A testnet request declines before the screen |
| Default priority fee settings row | Unscheduled. v1 still fills a missing priority fee with 5000 lamports |

## Acceptance

From `android/`, set `JAVA_HOME` to the Android Studio JBR (this machine: `C:\Program Files\Android\Android Studio\jbr`):

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

One Android emulator. `bun run playground` at the repo root, then the playground **Sign and send** button, is the hand-test driver. Use devnet, with the wallet cluster set to Devnet, so a public RPC exists.

1. An unsigned devnet transfer opens this screen: account pill, site chip, title Sign and send, expected-changes card, fee card, instruction disclosure, Reject and Approve. No AI card. Approve shows Sending, then the playground receives one signature for that request only. Reject fails only that request and does not submit.
2. `sign_transactions` still returns signed bytes and does not call `sendTransaction`.
3. With the wallet on Mainnet and no allowed mainnet RPC, a mainnet sign-and-send request does not open this screen. A testnet request does not open this screen. With the wallet on Devnet, a mainnet request that has an RPC URL shows the mismatch block and Approve stays disabled.
4. Lock is not on this screen. System back rejects. Kill the app during this screen: that request fails and does not return. A watch account, an empty payload, more than one payload, or bytes that are not legacy, v0, or v1 do not open this screen.

Unit tests cover: decline before offer for empty, multiple, unknown version, unknown chain, testnet, and mainnet with no RPC; a signature match that would complete; a signature mismatch that would not complete. Do not write a mnemonic or a private key into a test. Do not require a live RPC in the unit tests.
