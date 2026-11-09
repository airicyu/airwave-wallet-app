# Web wallet sign-transaction: fees, compute budget, simulation

Background for the Android app. Not a version contract. Source is `airicyu/airwave-wallet` as of 2026-11-08, mainly `wallet/src/background/simulate/sign-tx-simulate.ts`, `simulate-v1-sign.ts`, `v1-config-tx.ts`, `compute-budget-tx.ts`, `phase2-deltas.ts`, `simulate-rpc.ts`, and `wallet/src/approval/dom/fee-details.ts`. Format background is that repo's `dev-docs/research/solana-tx-versions.md`.

Optional AI analysis exists on the same approval and is out of this note. A later app version can add it. It never signs.

## What the dApp sends

Wallet Standard `signTransaction` gives one serialized transaction. The wallet does not ask the dApp which version it is. `@solana/kit` decodes the bytes. `compiledMessage.version` is `"legacy"`, `0`, or `1`.

| Version | Compute budget on the wire | Priority figure |
|---------|----------------------------|-----------------|
| legacy, v0 | `ComputeBudget` instructions. Discriminator `2` is set-limit (`u32` compute units). Discriminator `3` is set-price (`u64` micro-lamports per compute unit). | `ceil(limit × price / 1_000_000)` lamports |
| v1 | Message config (`setTransactionMessageConfig`). No `ComputeBudget` instructions are added. | `priorityFeeLamports` is already total lamports |

v1 config can also hold loaded-accounts data size and heap size. Sign-transaction does not edit those fields on a dApp message.

## Working copy

The original bytes stay on the pending request. A successful budget write stores a second copy, `workingTx`, in memory on that request id (`sign-tx-pending-state.ts`). Approve signs `workingTx` when it exists, otherwise the original bytes (`finishSignTransaction`). Reject does not sign. Process death drops both.

A later simulate with no user override and an existing `workingTx` re-simulates that copy. It does not rebuild the budget from the original bytes.

## Already signed, including partial

`isTransactionSigned` is true when any signature slot is 64 bytes and any byte in it is non-zero. One co-signer is enough. The wallet then:

- sets `cuEditable` false
- does not write limit, price, or v1 config
- runs phase-2 simulation on the original bytes
- on approve, calls `partiallySignWireTransaction`, which fills this wallet's slot and leaves other signatures in place

The fee card inputs are disabled. Apply does nothing.

## First open: legacy and v0

`parseCuFromMessage` reads the last set-limit and last set-price instruction. Two set-limit or two set-price instructions set `hasDuplicateCbDisc`. The wallet then refuses to write (`CU_WRITE_FAILED`), leaves the bytes alone, and still runs phase 2. Inputs stay enabled so the user sees the failure.

When the message is unsigned, has no duplicate budget instructions, and both limit and price are already present: no write, no phase-1 estimate. Phase 2 runs on the original bytes. `cuEditable` is true.

When limit is missing, phase 1 estimates it. When price is missing, the wallet fills Settings **Default CU price** (`defaultCuPrice`, factory `25000`, bounds `0` … `1_000_000_000`). The write goes into `workingTx` only. Phase 1 never writes the probe into `workingTx`.

### Phase 1 (limit only)

1. Copy the unsigned transaction and set limit to `1_400_000` and price to `0` on that copy. If the copy cannot be written, the probe is the original bytes.
2. `simulateTransaction` on the probe (`sigVerify: false`, `replaceRecentBlockhash: false`, `commitment: confirmed`, 15 second deadline).
3. If `err` is null and `unitsConsumed` is a non-negative integer: suggested limit is `ceil(unitsConsumed × 1.1)`, then `max` with the original limit when one existed, then clamped to `1` … `1_400_000`.
4. If that simulate fails and the message already had a limit, keep that limit.
5. If that simulate fails and there was no limit, static fallback: each compiled instruction is `3_000` units when its program id is a named builtin, otherwise `200_000`. Sum × `1.25`, ceil, same clamp. This fallback does not call RPC again.

The wallet does not call Kit `estimateResourceLimitsFactory` on this path anymore. It reads `unitsConsumed` from its own probe simulate.

### Fee numbers shown for legacy and v0

Signature fee is `5_000 × numSignerAccounts` lamports. Priority fee is `ceil(limit × price / 1_000_000)` lamports, and only when both numbers exist. Total is signature fee plus that priority fee. The card shows signature fee, priority fee, and the total. The two inputs are **CU limit** (units) and **CU price** (micro-lamports per unit).

## First open: v1

`readV1Budget` uses Kit `getTransactionMessageComputeUnitLimit` and `getTransactionMessagePriorityFeeLamports`. There is no phase-1 estimate and no static per-instruction estimate. A missing CU limit stays missing. The wallet does not fill loaded-accounts data size on a dApp v1 message. A v1 message whose limit is still unset can fail at runtime; the approval does not invent one.

When priority fee is missing, the wallet writes Settings **Default priority fee** (`defaultPriorityFeeLamports`, factory `"5000"` lamports) into the v1 config and commits that as `workingTx`. It does not insert `ComputeBudget` instructions.

Fee numbers: signature fee is the same `5_000 × signers`. Priority fee is the config lamports (treated as `0` when still absent). Total is the sum. `cuPrice` on the result is always null. The card inputs are **CU limit** (units) and **priority fee** (lamports). There is no CU price field.

Wallet-built send and close-empty are a different path. They measure one provisional v1 simulate (`measureV1ResourceUse`) and then set their own limit. That measurement is not used for a dApp `signTransaction`.

## User edits, then simulation updates

Apply is enabled only when `cuEditable`, the draft differs from the last applied pair, and the pair is in bounds.

| | CU limit | Second field |
|--|----------|----------------|
| legacy, v0 | integer `1` … `1_400_000` | CU price integer `0` … `1_000_000_000` |
| v1 | same limit bounds | priority fee as a base-10 lamport integer in the settings priority-fee range |

Apply sets `cuUserOverride` and calls `ui.simulatePendingTx` with both fields. The background requires both or neither.

- Legacy and v0: `writeCuToTransactionBytes` upserts set-limit and set-price on the **original** unsigned bytes (not on top of an older working copy). Success replaces `workingTx`. Phase 2 runs on the written bytes. Phase 1 does not run again.
- v1: `writeV1Budget` sets `computeUnitLimit` and `priorityFeeLamports` on the original unsigned message config. Same commit and phase-2 behavior. No `ComputeBudget` instructions.

`cuWriteError` surfaces as `CU_WRITE_FAILED`. The card total becomes that error. Inputs disable while the error is set. A failed write simulates the original bytes and does not replace `workingTx`.

While a new simulate is in flight, the fee total can stay on the previous figure (`feeFrozenForSim`) until the new pair is ready. Apply itself clears that freeze and shows the estimating state. An older response whose `seq` is behind `lastAcceptedSimSeq` is ignored.

## Phase 2 (what “simulation” shows)

`runPhase2Simulation` is the same for legacy, v0, and v1. It does not rewrite compute budget.

RPC `simulateTransaction`:

- encoding `base64`
- `sigVerify: false` (unsigned and partially signed packets both simulate)
- `replaceRecentBlockhash: false` (an expired blockhash fails the simulate)
- `commitment: confirmed`

Outcomes:

| `outcome` | When |
|-----------|------|
| `unparseable` | Wire bytes do not decode |
| `rpc` | HTTP or RPC error, 15s timeout, v0 address-lookup load failed, or balance arrays cannot be paired |
| `fail` | Simulate returned `err`. Logs are the last 20 lines. Deltas are included when balances parsed. |
| `ok` | No `err`. Deltas for the signer: native lamports and token amount strings, from `preBalances` / `postBalances` and token balance arrays. |

Instruction rows come from the compiled message. v0 lookup-table accounts come from `loadedAddresses` on the simulate result, or from a follow-up account fetch when the simulate did not return every lookup. v1 has no lookup tables; addresses are inline.

Deltas are display only. They are not a second fee calculation. Jupiter price enrichment on those deltas is display only and is not required to sign.

Approve stays available when simulation fails. The signed bytes are still `workingTx` or the original, not a packet rebuilt from the simulate result.
