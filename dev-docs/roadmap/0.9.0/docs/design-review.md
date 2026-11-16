# Design review — 0.9.0

- Date: 2026-11-16
- Round: 1 (no prior review file)
- Role: design review. This file is not the contract. INDEX, HOW, reasoning, HANDOFF, and code were not edited.
- Reviewed: [`../INDEX.md`](../INDEX.md), [`reasoning.md`](reasoning.md), [`how.md`](how.md). No `HANDOFF.md`.
- Baseline: decided sentences and acceptance in the 0.9.0 INDEX. Previous contract: [0.7.0 INDEX](../../0.7.0/INDEX.md) (decode, budget, simulation, screen, one in-memory request, no submit). [0.8.0](../../0.8.0/INDEX.md) is shipped and does not change this method. Phone frames reused from [`wallet-overall-v1.html`](../../../design-demos/wallet-overall-v1.html): `approve-sign-tx`, `approve-sign-tx-v1`, `approve-sign-tx-fail`, `approve-sign-mismatch`. The AI card on those frames stays omitted.
- Architecture: [`../../GUIDELINES.md`](../../GUIDELINES.md) section 4. The proposal keeps the pending request, the submit URL, and the signed bytes in memory, returns one signature only to that request, adds no package, adds no hardcoded password, and leaves fee math on the 0.7.0 `BigInteger` rules. Optional AI is not on this path.

## Load-bearing decisions

Reasoning names the approach: the 0.7.0 sign-transaction screen, then `sendTransaction`, then one signature returned only when it is this account's 64-byte slot. Dropped alternatives are named: AI, a confirmation wait, testnet as a wallet cluster, a new Activity, a new dependency, and the legacy-only `WalletModel.signAndSend` helper.

That signature match is deliberate. `sendTransaction` returns the transaction signature, which is the first signature. The contract accepts it only when it equals this account's slot, so a partial signer is not reported as the fee payer. The hand-test transfer is the fee payer, so acceptance 1 can succeed. A non-fee-payer partial sign that the RPC accepts stays on the send-failure block. That is the written rule, not a gap.

`sign_transactions` stays the 0.7.0 path and does not submit. This version replaces the immediate decline of `sign_and_send_transactions`. It does not run beside a second submit path.

## Screen spec

Decided does not rely on a filename alone. The Screen row points at the 0.7.0 screen row for blocks, order, glyphs, and the fee editor, then states the differences this version adds:

- Centered title `mwa.signSend.title` (Sign and send / 簽署並送出 / 签署并送出).
- No back control, no lock control, no AI card. Sticky bottom is Reject then Approve.
- While `sendTransaction` is in flight, both buttons are disabled and the changes area shows `mwa.signSend.sending` instead of delta rows.
- Send failure uses the 0.7.0 failure block inside the changes card: `mwa.signSend.sendFail`, the short RPC reason when one exists, the same details disclosure, and `mwa.signTx.failHint`.
- Refresh re-runs simulation and does not send. System back rejects.

The 0.7.0 screen row still supplies the row order: account pill, site line (a centered chip), title, expected-changes card, fee card, instruction disclosure, Reject then Approve. The four named frames exist. Acceptance 1 names that same chrome with the new title. M2 below is about the mismatch body, not about a missing frame.

## Findings

No HIGH. Open items are fix-by-default. Closed = written into INDEX or HOW. This round has no closed items.

### HIGH

None.

### MEDIUM

#### M1 — After a failed send, Apply can change what the screen shows while Approve resubmits the old bytes

INDEX Result: a failed send leaves the request open and Approve stays enabled so the same signed bytes can be sent again. HOW Track B: a second Approve sends those bytes and does not partial-sign again. The Screen row keeps the 0.7.0 fee editor. 0.7.0 Edits: Apply writes a new working copy from the original unsigned bytes and runs phase 2 again.

Nothing disables Apply after the first successful partial sign. The user can Apply, see a new fee and a new simulation, and Approve. The wallet then posts the earlier signed bytes. The screen and the submitted transaction are not the same bytes.

Sentence to add: after a successful partial sign, Apply is disabled for the rest of this request. Refresh may re-simulate those signed bytes and must not build a new working copy. A second Approve only calls `sendTransaction` with those bytes.

#### M2 — Chain mismatch reuses a blockhash sentence

INDEX Cluster match: when the request chain is mainnet or devnet and that cluster is not the selected cluster, the screen is the 0.7.0 mismatch block, Approve is disabled, and `sendTransaction` is not called. HOW selects that screen before simulation.

The 0.7.0 copy row and `approve-sign-mismatch` say the body states the wallet cluster and the cluster where the blockhash is still valid (`mwa.signTx.mismatchBody`; the frame body is the blockhash sentence). On the new path the blockhash has not been checked. The body can be false. The blockhash-only mismatch (hash valid on the other of mainnet and devnet) still fits that sentence.

The 0.7.0 mismatch state also hides changes, fees, and instructions and, on devnet, shows the Devnet strip. INDEX does not say whether the chain-mismatch path keeps that chrome.

Sentence to add: chain mismatch uses the mismatch title, disables Approve, and hides changes, fees, and instructions. Its body states the wallet cluster and the request chain. It does not claim the blockhash is valid. The existing blockhash mismatch keeps `mwa.signTx.mismatchBody`.

#### M3 — HOW does not carry the submit call the INDEX decided

INDEX Submit: `encoding` base64, `skipPreflight` false, no `maxRetries`, copy `minContextSlot` only when the walletlib request exposes it, same 15 second timeout style as 0.7.0 simulation, and the send-failure block shows the short RPC reason when one exists.

HOW Track B says `SolanaJsonRpc.sendTransaction` posts the bytes and that `minContextSlot` is included when exposed. It does not mention the timeout or the error string. Reasoning says this is the existing `SolanaJsonRpc` call.

`SolanaJsonRpc.sendTransaction` today takes `(rpcUrl, signedTx)` and returns `String?`. Its `post` has no connect or read timeout. A JSON-RPC `error` object becomes null, so the short reason is discarded. `minContextSlot` cannot be passed. `SignTxRpc.post` already uses 15 seconds, but it also drops the error object. Walletlib `SignAndSendTransactionsRequest.getMinContextSlot()` exists (`Integer`, nullable), so the INDEX condition is real.

Sentence to add in Track B: extend `SolanaJsonRpc.sendTransaction` (do not add a client) so the config can include `minContextSlot`, both timeouts are 15 seconds, and a JSON-RPC error yields the short `message` for the in-memory failure. A timeout or a missing message still uses the failure block with no reason line.

### LOW

#### L1 — Other walletlib submit options are unnamed

`SignAndSendTransactionsRequest` also exposes `commitment`, `skipPreflight`, `maxRetries`, and `waitForCommitmentToSendNextTransaction`. INDEX forces `skipPreflight` false and omits `maxRetries`. It does not say that `commitment` is omitted. One sentence should say the config does not copy `commitment`, `skipPreflight`, `maxRetries`, or the wait-for-commitment flag. Only one payload is accepted, so the wait flag has no second transaction.

#### L2 — "addresses" does not exist on this request

HOW Track A says watch account and `addresses` use the 0.7.0 rules. `SignAndSendTransactionsRequest` has `authorizedAccounts` and no `addresses` array. 0.7.0 sign-transaction code does not call `addressesAllow`. The account gate that matters here is the 0.7.0 watch-account rule: do not `offer`, and show `mwa.watchAccount` until teardown. A sentence in Track A should say that, so an implementer does not invent an `addresses` check.

## Acceptance cross-check

| Check | Can a human or a unit test do it? |
|-------|-------------------------------------|
| Gradle `assembleDebug`, `testDebugUnitTest`, `lintDebug` from `android/` with `JAVA_HOME` set | Yes. Command is in INDEX and HOW. |
| 1. Devnet playground Sign and send: pill, site chip, title Sign and send, changes card, fee card, instructions, Reject and Approve, no AI card. Approve shows Sending, then one signature for that request. Reject does not submit. | Yes, on one emulator with the wallet cluster on Devnet. Chrome matches the screen row plus the 0.7.0 order. |
| 2. `sign_transactions` returns signed bytes and does not call `sendTransaction` | Yes. |
| 3. Mainnet with no allowed RPC does not open the screen. Testnet does not open the screen. Wallet on Devnet plus a mainnet request that has an RPC URL shows mismatch and Approve stays disabled. | Yes. M2 is the body text of that mismatch, not whether the case can be run. |
| 4. No lock control. System back rejects. Process death fails the request. Watch account, empty payload, more than one payload, and bytes that are not legacy, v0, or v1 do not open the screen. | Yes. |
| Unit tests: decline before offer for empty, multiple, unknown version, unknown chain, testnet, and mainnet with no RPC. Signature match would complete. A different decoding would not. No mnemonic, no private key, no live RPC. | Yes. HOW Track A also names a watch-account test; acceptance 4 already covers that on device. |

No "and so on". Tracks A and B have checks that can run before Track C. Track C is acceptance 1 through 4.

## Current-code sample

Sampled only. Current code not yet implementing 0.9.0 is not a design HIGH.

| Anchor | What it does today | Against this proposal |
|--------|--------------------|------------------------|
| `MwaHostViewModel.onSignAndSendTransactionsRequest` | `request.completeWithDecline()` | Expected until this version. The proposal replaces this with `MwaSession.offerSignAndSend`. |
| `RpcResolver.rpcUrlForSubmit` | Unknown chain returns null. Mainnet returns null when no allowed active RPC. Devnet returns the allowed custom URL or the public devnet URL. Testnet returns `https://api.testnet.solana.com`, not null. | Matches the decline rules for unknown chain and mainnet-without-RPC. Testnet must be declined before offer even though this function returns a URL. INDEX and HOW already say that. Do not treat a non-null URL as "open the screen" for testnet. |
| `SolanaJsonRpc.sendTransaction` | Base64, `skipPreflight` false, no `maxRetries`. No timeout. Error JSON returns null. No `minContextSlot` parameter. | The success shape matches. Timeout, error text, and `minContextSlot` are M3. |
| `WalletOperationKind` | Already includes `SignAndSend`. `MwaApprovalScreen` draws nothing for that kind (`Unit`). | The kind can be reused. The screen is still the 0.7.0 sign-transaction composition, which the proposal assigns to step `SignAndSendApproval`. |
| `WalletModel.signAndSend` | Legacy-only sign, then `sendTransaction`, then `base58Decode` with no comparison to a signature slot. | INDEX and reasoning keep this helper off the path. `BurnerKeys.base58Decode` is the existing decoder HOW names. |

`completeWithSignatures` on walletlib 2.1.1 takes `byte[][]`. Returning one 64-byte array matches that method. Pending `WalletOperationRequest` is still a field on `WalletModel`, not a disk row.

## Fix tracker

| ID | Status | Where to write the sentence |
|----|--------|-----------------------------|
| M1 | open | INDEX Result or Screen, and HOW Track B |
| M2 | open | INDEX Cluster match, and HOW Track B |
| M3 | open | HOW Track B |
| L1 | open, non-blocking | HOW Track B |
| L2 | open, non-blocking | HOW Track A |

No waiver.

## Prior-round summary

Round 1. There is no earlier `design-review.md` for 0.9.0.

## Verdict

**Proceed after fixes.** The proposal is feasible. The gate does not pass while M1, M2, and M3 are open. No HIGH. Do not implement until those three sentences are in the contract, or a human waives them in a later round of this file.

---

# Design review — 0.9.0 — round 2

- Date: 2026-11-16
- Round: 2. Appended to this file. Round 1 titles and IDs are unchanged (M1, M2, M3, L1, L2).
- Role: design review. INDEX, HOW, reasoning, HANDOFF, and code were not edited.
- Reviewed again: [`../INDEX.md`](../INDEX.md), [`reasoning.md`](reasoning.md), [`how.md`](how.md). No `HANDOFF.md`.
- Baseline: the current decided sentences and acceptance. Previous contract remains [0.7.0 INDEX](../../0.7.0/INDEX.md).

## Load-bearing decisions

Unchanged from round 1. Reasoning still names the 0.7.0 screen plus `sendTransaction`, the signature-slot match, and the dropped alternatives (AI, confirmation wait, testnet as a wallet cluster, a new Activity, a new dependency, and `WalletModel.signAndSend`).

The round 1 sentences are now in the contract:

- After a successful partial sign, Apply stays disabled, Refresh re-simulates those signed bytes and does not build a new working copy, and a later Approve only calls `sendTransaction` (INDEX Submit, HOW Track B).
- Chain mismatch uses the mismatch title, hides changes, fees, and instructions, disables Approve, and uses `mwa.signSend.clusterMismatch`. It does not claim the blockhash is valid. The blockhash-only case still uses `mwa.signTx.mismatchBody` (INDEX Cluster match and copy keys, HOW Track B). The Devnet strip sentence matches the 0.7.0 mismatch chrome.
- `SolanaJsonRpc.sendTransaction` is extended in place: base64, `skipPreflight` false, optional `minContextSlot`, 15 second connect and read timeouts, and `error.message` as the short reason (INDEX Submit, Result, and Files; HOW Track B).
- The config does not copy `commitment`, the request's `skipPreflight`, `maxRetries`, or `waitForCommitmentToSendNextTransaction` (INDEX Submit, HOW Track B).
- This request has no `addresses` array. A watch account does not `offer`, and `MwaActivity` shows `mwa.watchAccount` until teardown (INDEX Account, HOW Track A).

## Screen spec

The Screen row still points at the 0.7.0 blocks, order, glyphs, and fee editor, and names this version's title, sending line, and send-failure block. Cluster mismatch now names the title, the hidden cards, the new body, and the Devnet strip. Acceptance 1 still names the happy-path chrome. No frame is missing.

## Findings

No HIGH. M1, M2, M3, L1, and L2 are closed because the sentences are in INDEX or HOW. One new non-blocking LOW.

### HIGH

None.

### MEDIUM

None open.

#### M1 — After a failed send, Apply can change what the screen shows while Approve resubmits the old bytes

Closed. INDEX Submit and HOW Track B disable Apply after the successful partial sign, keep Refresh on those signed bytes, and send only those bytes on a later Approve.

#### M2 — Chain mismatch reuses a blockhash sentence

Closed. INDEX Cluster match, the `mwa.signSend.clusterMismatch` copy row, and HOW Track B give this path its own body and keep `mwa.signTx.mismatchBody` for the blockhash-only case.

#### M3 — HOW does not carry the submit call the INDEX decided

Closed. HOW Track B now states the config fields, both 15 second timeouts, and that `error.message` is the short reason. A timeout or a body with no message shows the failure block and no reason line. INDEX Submit, Result, and Files say the same and forbid a second client.

### LOW

#### L1 — Other walletlib submit options are unnamed

Closed. INDEX Submit and HOW Track B omit `commitment`, the request's `skipPreflight`, `maxRetries`, and `waitForCommitmentToSendNextTransaction`.

#### L2 — "addresses" does not exist on this request

Closed. HOW Track A says this request has no `addresses` array and not to add an address-list check. The watch-account gate matches INDEX Account.

#### L3 — Send-failure disclosure body is unnamed

Open, non-blocking. The Screen row reuses the 0.7.0 failure block: title, short reason, the same details disclosure, and `mwa.signTx.failHint`. Result says a JSON-RPC `message` is the short reason. Neither INDEX nor HOW says what the disclosure body shows for a send failure (the message again, `error.data`, or an empty body).

An implementer can show an empty disclosure or paste the message twice and still match the written sentences. One sentence in the Screen row would name the body. It does not change the submit rule or the happy path.

## Acceptance cross-check

The round 1 checks are still runnable. No "and so on". Tracks A and B still have checks before Track C. Acceptance does not walk a failed send or the disabled Apply control. Those rules are in INDEX and HOW, so the missing hand-test step is not a second open MEDIUM.

| Check | Can a human or a unit test do it? |
|-------|-------------------------------------|
| Gradle `assembleDebug`, `testDebugUnitTest`, `lintDebug` | Yes |
| 1. Devnet Sign and send chrome, Sending, one signature, Reject does not submit | Yes |
| 2. `sign_transactions` does not submit | Yes |
| 3. Mainnet without RPC, testnet, and cluster mismatch | Yes. M2's body is now specified |
| 4. No lock, system back, process death, watch account, bad payloads | Yes |
| Unit tests named in INDEX | Yes |

## Current-code sample

Still a sample. Current code is not a design HIGH.

| Anchor | What it does today | Against this proposal |
|--------|--------------------|------------------------|
| `MwaHostViewModel.onSignAndSendTransactionsRequest` | `completeWithDecline()` | Replaced by `offerSignAndSend` when implemented |
| `RpcResolver.rpcUrlForSubmit` | Unknown chain is null. Testnet returns the public testnet URL. Mainnet is null without an allowed RPC. Devnet always has a URL | INDEX still declines testnet before offer even when the URL is non-null |
| `SolanaJsonRpc.sendTransaction` | Base64, `skipPreflight` false, no timeout, error object becomes null, no `minContextSlot` | The extension is now specified. This is implementation work, not an open design gap |
| Pending request | In memory on the session model | Matches. No disk payload |

## Fix tracker (round 2)

| ID | Status | Where the sentence is |
|----|--------|------------------------|
| M1 | closed | INDEX Submit; HOW Track B |
| M2 | closed | INDEX Cluster match and copy keys; HOW Track B |
| M3 | closed | INDEX Submit, Result, Files; HOW Track B |
| L1 | closed | INDEX Submit; HOW Track B |
| L2 | closed | INDEX Account; HOW Track A |
| L3 | open, non-blocking | Screen row, if a later edit names the disclosure body |

No waiver. The proposal is feasible. Architecture bans hold: one in-memory pending request, one signature only to that request, no second durable store, no hardcoded password, fee math stays on the 0.7.0 integer rules, no AI on this path.

## Verdict

**Proceed.** Gate passed. No open HIGH. M1, M2, and M3 are in the contract. L3 is explicitly non-blocking. Do not treat L3 as a reason to stop implementation.
