# 0.7.0 handoff

Status is `in progress`. Design-review round 2 passed. Implementation has started and is not complete.

## Done this session

INDEX, reasoning, and how for `sign_transactions` only. Screen follows the four `approve-sign-tx*` frames without the AI card. `sign_and_send_transactions` stays declined.

## Next session

`SignTxRun` calls `simulateTransaction` and `isBlockhashValid`. The screen shows estimating, the failure block, network mismatch, refresh, and the CU apply fields. Approve signs the in-memory working bytes. Emulator acceptance 1–4 was not walked. Token rows beyond the fee payer's SOL delta, and the verified shield, are still not drawn.

## Paste-ready

Read AGENTS.md, dev-docs/roadmap/GUIDELINES.md, dev-docs/roadmap/0.7.0/INDEX.md, docs/reasoning.md, and docs/how.md. Design-review the 0.7.0 proposal. Do not implement. `sign_transactions` signs one legacy, v0, or v1 payload and does not submit. No AI card. `sign_and_send_transactions` still declines.
