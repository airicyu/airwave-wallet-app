# 0.7.0 — reasoning

## What ships

`sign_transactions` becomes a real approval on `MwaActivity`. The wallet decodes one payload as legacy, v0, or v1, simulates it, lets the user edit the compute budget when every signature slot is empty, and returns partially signed bytes. It does not submit.

The screen follows the overall-v1 phone frames for the success, failure, v1 fee card, and network-mismatch states. The AI card on those frames is left out. A later version can add it. Optional AI stays read-only and never signs; this version does not call it.

## Why

Mobile Wallet Adapter does not label the transaction version. The bytes do. Legacy and v0 keep the compute budget in `ComputeBudget` instructions. v1 keeps the compute-unit limit and the priority fee in the message config, and the priority fee is already lamports. Treating a v1 payload as v0 would show a CU price that is not on the transaction and could append instructions the format does not use.

Signing without submitting matches the dApp flow this version is for: the dApp builds the transaction, the wallet signs, the dApp submits. `sign_and_send_transactions` is a different method and stays declined.

One payload matches the one screen. A partial signature must be preserved, so a non-zero slot freezes the budget and approve only fills this account's slot.

The default v1 priority fee is the constant 5000 lamports from the web wallet factory default. This version does not add a settings row, because the scheduled frames are the approval screens.

## What was dropped

AI analysis, wallet-side submit, a default-priority-fee settings row, token price quotes, and a new dependency. Decode, budget edit, and fee math stay in this repo on top of `web3-solana` where that library already decodes the message. A v1 config read or write that `web3-solana` 0.2.5 cannot do is implemented here, not by adding a package. Simulation is JSON-RPC `simulateTransaction` to the cluster RPC the user already configured.

## Already landed

MWA approval files under `ui/mwa`, and `WalletModel` under `session`, moved before this version opened. This version ships those moves unchanged and builds the sign-transaction screen on them.
