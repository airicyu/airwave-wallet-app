# 0.7.0 — how

Session ownership stays as in [0.6.0 how](../../0.6.0/docs/how.md): `MwaHostViewModel` owns the Scenario, `MwaSession` owns the step, `MwaOffers` builds one in-memory `WalletOperationRequest`. This file adds `SignTransaction`.

## Track A — Offer

`onSignTransactionsRequest` calls `MwaSession.offerSignTransaction`. Empty, missing, or multiple payloads call `completeWithDecline()` and do not `offer`. Decode the one payload before `offer`. Failure to decode, or a version other than legacy, v0, or v1, declines the same way.

Watch account and `addresses` use the 0.6.0 rules. A locked vault uses step `UnlockSignTransaction` and keeps the protocol request on `MwaSession`.

The request stores the original `ByteArray` and a version tag. It may store a working `ByteArray` after Track B writes one. Neither array is written to disk or preferences. `onSignAndSendTransactionsRequest` stays `completeWithDecline()`.

**Check:** unit tests for empty, multi-payload, unknown version, address mismatch, and a watch account that does not offer and shows `mwa.watchAccount`.

## Track B — Budget and simulation

All lamport math uses `BigInteger`.

Legacy and v0:

- Read set-limit (discriminator 2, `u32`) and set-price (discriminator 3, `u64` micro-lamports).
- Duplicate discriminator: skip the write, record `cuWriteError`, simulate the original bytes.
- Missing limit on an unsigned message: copy the bytes, set limit `1400000` and price `0` on the copy, `simulateTransaction`, and if `unitsConsumed` is a non-negative integer and `err` is null, suggested limit is the integer ceiling `(unitsConsumed × 11 + 9) / 10`, then the max of that and any original limit, clamped to `1..1400000`. If the probe fails and a limit existed, keep it. If the probe fails and no limit existed, sum `3000` per named builtin instruction and `200000` per other instruction, then the integer ceiling `(sum × 5 + 3) / 4`, and clamp. Use `BigInteger` for both ceilings.
- Missing price: write `settings.defaultCuPrice`.
- Priority lamports: `ceil(limit × price / 1000000)` when both exist.

v1:

- Read compute-unit limit and priority-fee lamports from the message config.
- Do not append `ComputeBudget` instructions. Do not invent a missing limit.
- Missing priority fee: write `5000` lamports into the config.

`MwaSession` starts the limit probe and phase 2. The result is stored on the in-memory request. Reject, a new Apply, and teardown cancel the in-flight call and ignore a late result. A successful write replaces the in-memory working copy built from the original unsigned bytes. Phase 2 simulates the working copy when present, otherwise the original, with `sigVerify` false and `replaceRecentBlockhash` false. Any non-zero signature skips the write and simulates the original. When this account's slot is already non-zero, Approve leaves that slot unchanged and still returns the bytes.

Blockhash check: `isBlockhashValid` on the wallet cluster RPC and on the other of mainnet and devnet. Valid only on the other cluster selects the mismatch state. Invalid on both clusters selects the failure block.

Approve partial-signs the working copy, or the original when there is no working copy. Reject, system back, and process death decline that request only.

**Check:** unit tests named in the INDEX acceptance paragraph.

## Track C — Screen and capability

Add `solana:signTransactions` to `MobileWalletAdapterConfig` optional features. Do not add `solana:signAndSendTransaction`.

`MwaActivity` composes the approval when the step is `SignTransactionApproval`. Layout and copy keys are the INDEX screen and copy rows. The v1 phone frame replaces only the expanded fee fields. Do not draw an AI card. Do not add a vector XML file for a glyph the app already has (refresh, copy, shield, SOL mark, signing account mark). The failure state keeps the Expected changes heading and does not draw refresh. Sticky Reject then Approve. System back rejects.

**Check:** INDEX acceptance 1 through 4 on one emulator. The driver is a local dApp that calls `sign_transactions`, not the playground Sign and send button.

## Commands

From `android/`, `JAVA_HOME` set to the Android Studio JBR:

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```
