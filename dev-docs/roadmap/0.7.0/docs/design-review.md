# Design review — 0.7.0 Airwave Wallet App

- Date: 2026-11-08 (Asia/Hong_Kong)
- Round: **re-review 2** (initial text kept below; statuses updated where the contract now contains the fix)
- Role: design review (does not edit INDEX / HOW / reasoning / HANDOFF / code; this file is not the contract)
- Baseline: decided sentences and acceptance in [`../INDEX.md`](../INDEX.md); [`reasoning.md`](reasoning.md); [`how.md`](how.md); [`../HANDOFF.md`](../HANDOFF.md). Previous contract: [0.6.0 INDEX](../../0.6.0/INDEX.md) (session, unlock, one pending request, `sign_messages` address rule). Phone frames in [`wallet-overall-v1.html`](../../../design-demos/wallet-overall-v1.html): `approve-sign-tx`, `approve-sign-tx-v1`, `approve-sign-tx-fail`, `approve-sign-mismatch`. The AI card on those frames is omitted by the INDEX.
- Architecture bans: [`../../GUIDELINES.md`](../../GUIDELINES.md) section 4 and section 12. This proposal keeps the pending request and the working copy in memory, returns bytes only to that request, does not submit, does not add a hardcoded password, and states lamport math as `BigInteger`.
- Current-code sample: `MwaHostViewModel.onSignTransactionsRequest`, `MwaHostViewModel.onSignAndSendTransactionsRequest`, `ui/mwa/MwaSignTxScreen.kt`, `session/WalletOperation.kt`
- **Verdict:** the review gate passes. No open HIGH. Round 1 MEDIUMs are written into INDEX and HOW. The proposal is feasible. L3 is non-blocking.

## Load-bearing decisions

Reasoning names the approach (decode version from bytes, budget edit only while every signature slot is empty, partial sign, no submit) and the dropped alternatives (AI, wallet-side submit, a priority-fee settings row, a new package, treating v1 as v0). HOW stays inside the INDEX: `sign_and_send_transactions` still declines, no new Activity, no AI call. Session ownership matches 0.6.0 (`MwaHostViewModel`, `MwaSession`, one `WalletOperationRequest`).

## Findings (this round)

Closed = written into INDEX / HOW / HANDOFF. Open = the contract still diverges. Non-blocking = record only.

### HIGH

#### H1 — Approval chrome copy and the failure heading do not match the phone frames — **closed**

The INDEX screen row and copy keys disagree with the frames on controls this version ships. The AI card is the only omission the INDEX names.

- `approve-sign-tx-v1` button text is `套用 CU`. The copy key `mwa.signTx.apply` is Apply / 套用 / 套用.
- `approve-sign-tx` and `approve-sign-tx-fail` disclosure text is `交易明細 · 指令`. The copy key `mwa.signTx.instructions` is Transaction details / 交易明細 / 交易明细.
- `approve-sign-tx` puts a refresh glyph on 預期變動. `approve-sign-tx-fail` keeps that heading and does not draw the glyph. The screen sentence puts the refresh glyph on the Expected changes heading and then says the failure state only replaces the rows.

Why HIGH: agent workflow screen spec. Decided copy would ship labels and a failure control the named frames do not show.

Sentence to write into the Screen and Copy keys rows (one lock): the Apply control label is Apply CU / 套用 CU / 套用 CU; the instruction disclosure summary is Transaction details · Instructions / 交易明細 · 指令 / 交易明细 · 指令; the failure state keeps the Expected changes heading and does not draw the refresh glyph.

### MEDIUM

| ID | Status | Basis |
|----|--------|--------|
| M1 | closed | INDEX: a blockhash invalid on both mainnet and devnet is the failure block, not mismatch. HOW Track B only selects mismatch when the hash is valid on the other cluster. Write the both-invalid outcome into Track B. |
| M2 | closed | Screen says a duplicate compute-budget discriminator keeps the inputs enabled so `cuWriteError` is visible. No copy key names that error. Add the en / zh-Hant / zh-Hans sentence. |
| M3 | closed | Simulation outcomes are success, on-chain failure, RPC failure, and unparseable. Acceptance 3 only binds an instruction error to the failure block. `mwa.signTx.estimating` is unused in the screen row. Say which outcomes use the failure block, what a success with no balance rows shows, and when Estimating replaces the rows. |
| M4 | closed | HOW writes `ceil(unitsConsumed × 1.1)` and `× 1.25` for the limit estimate. GUIDELINES bans `double` for simulated deltas. State the integer ceiling (for example `ceil(n * 11 / 10)` and `ceil(n * 5 / 4)` with `BigInteger`) in Track B. |
| M5 | closed | Signature fee is `5000` lamports times the signer count. Lock the count as the message's required-signature count (header), including already-filled slots. |
| M6 | closed | "Approve still partial-signs this account's empty slot" does not say what Approve does when that slot is already non-zero. Lock it: leave the slot unchanged and still return the bytes to that request. |
| M7 | closed | `approve-sign-tx-v1` draws only the expanded fee card. The success frame and the screen sentence still have the pill, site line, title, expected changes, and instruction disclosure. HOW Track C should say the v1 frame replaces the expanded fee fields only. |
| M8 | closed | Widget ownership: Track B does not name which object starts the phase-2 (and limit-probe) RPC call, where the result is stored, and that reject, a new Apply, or teardown cancels the in-flight call. The working copy stays on the in-memory request. |

### LOW

| ID | Status | Basis |
|----|--------|--------|
| L1 | closed | Track A check lists empty, multi-payload, unknown version, and address mismatch. INDEX acceptance also covers a watch account (`mwa.watchAccount`, no offer). Point the Track A check at that sentence. |
| L2 | closed | Delta row text on `approve-sign-tx` is `SYMBOL · shortAddress` when a symbol exists, and the amount repeats the symbol (`+0.017204532 SOL`). INDEX says short address, symbol, and a signed amount, without the joiner or the amount suffix. One sentence on the screen row should match that frame. The verified shield is the existing home shield (score included when that component shows one). |

## Acceptance cross-check

| Acceptance sentence | Testable at design level | Gap |
|---------------------|--------------------------|-----|
| Gradle `assembleDebug`, `testDebugUnitTest`, `lintDebug` from `android/` with Android Studio JBR | Yes | None |
| Local dApp sends one `sign_transactions` payload per case; playground Sign and send is not the driver | Yes | None |
| 1. Legacy or v0 unsigned transfer: pill, site, title, changes with refresh, fee row, instruction disclosure, Reject and Approve; no AI card; Approve and Reject hit only that request; sign-and-send does not open an approval | Yes | H1 closed in round 2 |
| 2. v1 expanded fee card: CU limit and priority fee in lamports, no CU price; Apply re-simulates and the total updates; a non-zero signature disables inputs and Approve signs the original bytes | Yes | M6 and M7 closed in round 2 |
| 3. Instruction error shows the failure block, Approve stays enabled; blockhash valid only on the other of mainnet and devnet disables Approve | Yes | M1 and M3 closed in round 2 |
| 4. No lock control; system back rejects; process death fails the request and does not return it; watch, empty, multiple, or non-legacy/v0/v1 bytes do not open the screen | Yes | L1 closed in round 2 |
| Unit tests: version dispatch, decline-before-offer, legacy priority fee with `BigInteger`, v1 missing limit left unset, missing v1 priority fee 5000 lamports, already-signed skips the write; no mnemonic or private key in a test | Yes | M4 closed in round 2 |

Hand-test is one emulator path with named screens, not "test everything".

## Current-code sample

Current code does not implement this version. That is not a design HIGH.

- `onSignTransactionsRequest` and `onSignAndSendTransactionsRequest` both `completeWithDecline()`. The proposal replaces only the first. The second stays declined. `get_capabilities` does not advertise `sign_transactions` yet.
- `WalletOperationKind` is `Authorize`, `Reauthorize`, `SignMessage`, `SignAndSend`. There is no `SignTransaction`. `MwaStep` has no `UnlockSignTransaction` or `SignTransactionApproval`.
- `SignTxBody` is a sign-and-send preview that is not offered. It draws `LockButton` and `AiStub`. The proposal forbids both on this approval. Building Track C replaces that body; leaving the old composable reachable from `SignTransaction` would contradict the screen row.
- `WalletOperationRequest` is in memory on `WalletModel` and is not a disk row. The proposal keeps that and adds an original `ByteArray` plus an optional working copy, also not on disk. `signAndSend` in `WalletApproval.kt` remains off this path.
- `settings.defaultCuPrice` already exists (`Long`, default `25000`). The proposal reads it for a missing legacy/v0 price and does not add a v1 settings row.

Backlog and the Chrome extension are not this contract. Research `web-sign-transaction.md` is background only, as the INDEX says.

## Fix tracker

| ID | Grade | Status | Where it closed |
|----|-------|--------|-----------------|
| H1 | HIGH | closed | INDEX Copy keys `mwa.signTx.apply`, `mwa.signTx.instructions`; INDEX Screen failure heading; HOW Track C |
| M1 | MEDIUM | closed | INDEX Network mismatch; HOW Track B blockhash check |
| M2 | MEDIUM | closed | INDEX Copy keys `mwa.signTx.cuWriteError`; INDEX Screen |
| M3 | MEDIUM | closed | INDEX Simulation |
| M4 | MEDIUM | closed | INDEX Fees; HOW Track B integer ceilings |
| M5 | MEDIUM | closed | INDEX Fees |
| M6 | MEDIUM | closed | INDEX Already signed; HOW Track B |
| M7 | MEDIUM | closed | INDEX Screen; HOW Track C |
| M8 | MEDIUM | closed | INDEX Simulation; HOW Track B |
| L1 | LOW | closed | HOW Track A check |
| L2 | LOW | closed | INDEX Screen |
| L3 | LOW | open, non-blocking | |

## Prior rounds

| Round | Date | Conclusion |
|-------|------|------------|
| initial | 2026-11-08 | Open H1. Gate not passed. Proposal is feasible. |
| re-review 2 | 2026-11-08 | H1 and M1–M8 and L1–L2 closed in the contract. No open HIGH. Gate passes. L3 non-blocking. Proposal is feasible. |

## Round 2

- Date: 2026-11-08 (Asia/Hong_Kong)
- Round: **re-review 2**
- Baseline: current INDEX decided sentences and acceptance, HOW, reasoning, HANDOFF. Phone frames re-read: `approve-sign-tx`, `approve-sign-tx-v1`, `approve-sign-tx-fail`, `approve-sign-mismatch`. AI card still omitted by the INDEX.
- **Verdict:** the review gate passes. No open HIGH. Agreed MEDIUMs are in the contract. The proposal is feasible. L3 does not block implementation.

### Findings (this round)

H1 is closed. Copy keys now lock Apply CU / 套用 CU / 套用 CU and Transaction details · Instructions / 交易明細 · 指令 / 交易明细 · 指令. The Screen row and HOW Track C keep the Expected changes heading in the failure state and do not draw refresh. Those three strings match the frames.

M1–M8 and L1–L2 are closed. Both-invalid blockhash is the failure block (INDEX Network mismatch, HOW Track B). `mwa.signTx.cuWriteError` has three locales. Simulation names the four outcomes, the failure block, Estimating, and a success with no delta rows, and names `MwaSession` as the caller that stores the result on the in-memory request and cancels on reject, a new Apply, or teardown. Fee math is `BigInteger`, including `(unitsConsumed × 11 + 9) / 10` and `(sum × 5 + 3) / 4`. Signature fee uses the header required-signature count. Approve leaves this account's already non-zero slot unchanged and still returns the bytes. The v1 frame replaces only the expanded fee fields. Track A names the watch account.

No new HIGH. Architecture bans hold: one in-memory request, no disk rehydrate, no submit, no hardcoded password, no AI call, `sign_and_send_transactions` still declines.

#### L3 — Native SOL delta name and the mismatch body locales — **open, non-blocking**

`approve-sign-tx` labels native SOL as `SOL`, and the USDC row as `USDC · EPjF…Dt1v`. The Screen sentence applies `SYMBOL ·` short address whenever Jupiter metadata has a symbol, which would add a short address on the SOL row the frame does not show. `mwa.signTx.mismatchBody` says what the sentence must state and does not lock en / zh-Hant / zh-Hans. The mismatch frame's zh-Hant line is「錢包目前是 Devnet，這筆交易的 blockhash 在 Mainnet 仍有效。」

Non-blocking: row order, glyphs, the USDC joiner, the amount suffix, and the mismatch rule are already decided. A later edit can say native SOL's name is the symbol alone, and can paste the three mismatch sentences. Implementation can follow the current Screen and mismatch sentences.

### Acceptance cross-check

| Acceptance sentence | Testable at design level | Gap |
|---------------------|--------------------------|-----|
| Gradle `assembleDebug`, `testDebugUnitTest`, `lintDebug` from `android/` with Android Studio JBR | Yes | None |
| Local dApp sends one `sign_transactions` payload per case; playground Sign and send is not the driver | Yes | None |
| 1. Legacy or v0 unsigned transfer chrome, no AI card, Approve and Reject hit only that request, sign-and-send does not open an approval | Yes | None |
| 2. v1 expanded fee card, Apply re-simulates, a non-zero signature disables inputs and Approve signs the original bytes | Yes | None. Already-signed on this account's own slot is locked in INDEX Already signed |
| 3. Instruction error shows the failure block and keeps Approve; blockhash valid only on the other cluster disables Approve | Yes | Both-invalid is decided in INDEX and is not a separate acceptance step. Not a HIGH |
| 4. No lock, system back rejects, process death fails the request, watch / empty / multiple / unknown version do not open the screen | Yes | None |

Hand-test remains one emulator path with named screens.

### Current-code sample

Unchanged from the initial round. Current code still declines `sign_transactions`. That is not a design HIGH. Display of base units already uses `BigDecimal` in `holdings/BaseUnits.kt` (chosen in 0.3.0). This INDEX forbids `double` for the fee math and does not need a second decimal type.
