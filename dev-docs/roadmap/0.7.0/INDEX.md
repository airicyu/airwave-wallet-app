# 0.7.0 — MWA sign transaction

- **Status:** `in progress`. Matches the roadmap table.
- **Previous:** [0.6.0](../0.6.0/INDEX.md) (`sign_messages`)
- **Background:** [web sign-transaction](../../research/web-sign-transaction.md). That note is not this contract.
- **Design demo:** [`wallet-overall-v1.html`](../../design-demos/wallet-overall-v1.html) phone frames `approve-sign-tx`, `approve-sign-tx-v1`, `approve-sign-tx-fail`, and `approve-sign-mismatch`. This version does not draw the AI card those frames include. AI is a later version.

## Product

A connected dApp can ask the wallet to **sign one transaction** through Mobile Wallet Adapter `sign_transactions`. The wallet shows the expected balance changes, the fee, and the instruction list, then returns the signed transaction bytes. It does not submit the transaction. The dApp submits.

The same screen handles a legacy message, a v0 message, and a v1 message. The request has no version field. The version is inside the payload bytes.

## Read order

1. This file
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)
4. [0.6.0 INDEX](../0.6.0/INDEX.md) (session, unlock, one pending request)
5. [web sign-transaction](../../research/web-sign-transaction.md) (behavior this contract copies; AI stays out)

## Decided

| Topic | Decision |
|-------|----------|
| Method | One new method: `sign_transactions` (`solana:signTransactions`). Advertise it in `get_capabilities`. `sign_and_send_transactions` still declines immediately and stays out of `get_capabilities`. `sign_messages` stays as in 0.6.0. The wallet returns signed transaction bytes and does not call `sendTransaction` |
| Payload | Exactly one non-empty `payloads` element. Empty, missing, or more than one: `completeWithDecline()` and do not `offer`. Bytes that do not decode as legacy, v0, or v1: decline the same way. A version byte other than legacy, v0, or v1 declines. Do not treat an unknown version as v0 |
| Account | Same address rule as 0.6.0 `sign_messages`. A `watch` account does not `offer` and `MwaActivity` shows `mwa.watchAccount` until teardown. Sign with the active account. Approve calls partial sign: fill this account's signature slot and leave every other slot as it arrived |
| Already signed | Any signature slot that is 64 bytes and contains a non-zero byte means the transaction is already signed, including a partial sign. Do not rewrite compute budget. Fee inputs are disabled. Phase-2 simulation uses the original bytes. Approve fills this account's slot when that slot is empty. When this account's slot is already non-zero, Approve leaves that slot unchanged and still returns the bytes to that request |
| Budget, legacy and v0 | Compute budget is `ComputeBudget` instructions. Discriminator 2 is the compute-unit limit. Discriminator 3 is the price in micro-lamports per compute unit. Two instructions with the same discriminator: do not write, set `cuWriteError`, simulate the original bytes, and keep the inputs enabled so the error is visible. When both values are already present, do not estimate and do not write. When the limit is missing, estimate it (HOW Track B) and write it. When the price is missing, write `settings.defaultCuPrice`. Writes go to an in-memory working copy on this request, not to the original bytes and not to disk |
| Budget, v1 | Compute unit limit and priority fee live in the v1 message config. Do not append `ComputeBudget` instructions. Do not fill a missing compute-unit limit and do not fill loaded-accounts data size. When the priority fee is missing, write `5000` lamports (the factory default; this version adds no settings row). A user Apply writes both the limit and the priority fee into that config on a new working copy built from the original unsigned bytes |
| Fees | Signature fee is `5000` lamports times the message header's required-signature count, including slots that are already filled, as `BigInteger`. Legacy and v0 priority fee is `ceil(limit × price / 1_000_000)` lamports, and only when both numbers exist. v1 priority fee is the config lamports. Total is signature fee plus priority fee. Do not use `double`. The limit estimate uses integer ceilings: `ceil(unitsConsumed × 11 / 10)` and `ceil(sum × 5 / 4)` |
| Simulation | `MwaSession` starts the limit probe and phase 2. Phase 2 is `simulateTransaction` on the bytes that will be signed (working copy when one exists, otherwise the original), `encoding` base64, `sigVerify` false, `replaceRecentBlockhash` false, `commitment` confirmed, 15 second timeout, against the RPC for the wallet's selected cluster. The result is stored on the in-memory request. Reject, a new Apply, and teardown cancel the in-flight call and ignore a late result. Outcomes are success, on-chain failure, RPC failure, and unparseable. On-chain failure, RPC failure, and unparseable use the failure block. Approve stays enabled when simulation fails, except on the network-mismatch screen. Reject stays enabled. While a call is in flight, the changes area shows `mwa.signTx.estimating` and does not show delta rows. A success with no balance change draws the heading and no delta rows |
| Network mismatch | If the recent blockhash is not valid on the wallet cluster and is valid on the other of mainnet and devnet, show the mismatch block and disable Approve. Testnet is not a wallet cluster. A blockhash that is invalid on both mainnet and devnet is the failure block, not mismatch |
| Screen | `MwaActivity` draws one scrollable approval. Top: the account pill (signing glyph plus the account name). No back control and no lock control. Then one site line (`mwa.site` plus the identity URI, or the identity name when the URI is empty). Then the title, centered. No AI card. Then one simulation card whose heading is **Expected changes**, with a refresh button and an explorer inspector button. The failure state stays inside that card and still shows both buttons. Each success row keeps a shield column that is blank unless the token is verified, then a token mark, a name, and a signed amount that repeats the symbol (green when positive, red when negative). Native SOL uses the existing SOL mark. When Jupiter metadata has a symbol, the name is `SYMBOL ·` short address; otherwise the name is the short mint address. The shield is the existing home shield, including its score when that component shows one. No row invents a shield. The failure state replaces the rows with the failure block: title, the short reason, a disclosure whose body is at most 320dp and has a copy control, and the hint that changes could not be estimated. A duplicate compute-budget discriminator shows `mwa.signTx.cuWriteError` on the fee total and keeps the inputs enabled. The account pill matches the web wallet pill and has a copy control for the address. The site line is a centered chip with a copy control. Then one fee card, collapsed by default, whose chevron sits on a square. Expanding it shows signature fee and priority fee with the label on the left and the SOL amount on the right, then CU limit and CU price side by side with a check that applies. The v1 frame replaces only the second field with priority fee lamports. Then a disclosure of instructions, collapsed by default: one row per compiled instruction with the program name, the decoded fields when the web wallet would decode them, otherwise the account shorts and data hex, then the fee payer and a raw-transaction disclosure. Sticky bottom: Reject then Approve. System back rejects this request. The mismatch state keeps the pill, a Devnet strip when the wallet cluster is devnet (no strip on mainnet), the site line, the title, and the mismatch copy; it hides changes, fees, and instructions |
| Edits | Apply is enabled only when the inputs are editable, the draft differs, and both values are in range. CU limit is an integer from 1 through 1,400,000. CU price is an integer from 0 through 1,000,000,000. v1 priority fee is an integer lamport amount from 0 through 1,000,000,000. Apply writes from the original unsigned bytes, stores the working copy in memory, and runs phase 2 again. It does not run the limit estimate again. A failed write does not replace the working copy |
| Locked vault | Same as 0.6.0: `UnlockScreen` on `MwaActivity` first. The request stays in memory. Process death fails it. Lock is not a control on this screen |
| Workflow | One in-memory `WalletOperationRequest` of kind `SignTransaction`. A second MWA request fails and does not replace the first. The signed bytes return only to that request. `completeWith*` does not `finish()` the Activity |
| Copy keys (en / zh-Hant / zh-Hans) | `mwa.signTx.title` Sign transaction / 簽署交易 / 签署交易. `mwa.signTx.changes` Expected changes / 預期變動 / 预期变动. `mwa.signTx.fee` Transaction fee / 交易費 / 交易费. `mwa.signTx.sigFee` Signature fee / 簽名費 / 签名费. `mwa.signTx.priorityFee` Priority fee / 優先費 / 优先费. `mwa.signTx.cuLimit` CU limit / CU limit / CU limit. `mwa.signTx.cuPrice` CU price / CU price / CU price. `mwa.signTx.apply` Apply CU / 套用 CU / 套用 CU. `mwa.signTx.instructions` Transaction details · Instructions / 交易明細 · 指令 / 交易明细 · 指令. `mwa.signTx.cuWriteError` Could not update the compute budget / 無法更新計算預算 / 无法更新计算预算. `mwa.signTx.failTitle` Transaction is expected to fail / 預計交易失敗 / 预计交易失败. `mwa.signTx.failDetails` Failure details / 失敗詳情 / 失败详情. `mwa.signTx.failHint` Could not estimate changes / 無法估計變動 / 无法估计变动. `mwa.signTx.mismatchTitle` Network mismatch / 網路不符 / 网络不符. `mwa.signTx.mismatchBody` states the wallet cluster and the cluster where the blockhash is still valid. `mwa.signTx.estimating` Estimating / 估計中 / 估计中. `mwa.approve` and `mwa.reject` stay as in 0.6.0 |
| Landed layout | The unversioned move of MWA screens into `ui/mwa` and of `WalletModel` into `session` ships with this version. Behavior of those moves stays as already landed |
| Not this version | AI analysis. `sign_and_send_transactions`. A settings row for the default priority fee. Jupiter price quotes on deltas. A new Activity |

## Still to decide before implementation

(empty)

## Not this version

| Item | Where it goes |
|------|----------------|
| AI card and report | Unscheduled |
| `sign_and_send_transactions` | Unscheduled. Still declines |
| Default priority fee settings row | Unscheduled. This version always fills a missing v1 priority fee with 5000 lamports |
| Submitting the transaction from the wallet | Not this version |

## Acceptance

From `android/`, set `JAVA_HOME` to the Android Studio JBR (this machine: `C:\Program Files\Android\Android Studio\jbr`):

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

One Android emulator. The dApp is a local page that sends one `sign_transactions` payload for each case below (legacy, v0, v1, partial, failure, mismatch). `bun run playground` is not sufficient: the playground Sign and send button is `sign_and_send_transactions`, which this version still declines.

1. An unsigned legacy or v0 transfer opens this screen: account pill, site line, title Sign transaction, expected-changes heading with refresh, fee row, instruction disclosure, Reject and Approve. No AI card. Approve returns signed bytes to that request only. Reject fails only that request. Sign and send still does not open an approval.
2. A v1 payload shows CU limit and priority fee in lamports in the expanded fee card, and no CU price field. Apply with a new pair re-simulates and the fee total updates. A payload that already has a non-zero signature disables both inputs, and Approve signs the original bytes.
3. A simulate that returns an instruction error shows the failure block, keeps Approve enabled, and does not hide the error. A blockhash that is valid only on the other of mainnet and devnet shows the mismatch copy and disables Approve.
4. Lock is not on this screen. System back rejects. Kill the app during this screen: that request fails and does not return. A watch account, an empty payload, more than one payload, or bytes that are not legacy, v0, or v1 do not open this screen.

Unit tests cover: version dispatch (legacy, v0, v1, unknown), decline-before-offer, the legacy priority-fee formula with `BigInteger`, v1 missing-limit left unset, missing v1 priority fee written as 5000 lamports, and already-signed skipping the write. Do not write a mnemonic or a private key into a test.
