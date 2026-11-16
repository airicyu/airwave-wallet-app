# 0.9.0 — reasoning

## What ships

`sign_and_send_transactions` becomes a real approval on `MwaActivity`. The screen, decode, compute-budget edit, and simulation stay the 0.7.0 sign-transaction path. Approve then calls `sendTransaction` and returns one signature to that request.

`sign_transactions` still returns signed bytes and does not submit. There is no AI card.

## Why

The Solana Mobile playground Sign and send button is this method. Through 0.8.0 the wallet declines it before any screen, so the hand-test client cannot finish a transfer.

Submit uses the request chain through the existing `RpcResolver.rpcUrlForSubmit`. The wallet's selected cluster stays the cluster for simulation and for the home screen. A request whose chain is the other of mainnet and devnet uses the existing mismatch block and cannot send. Testnet and an unknown chain decline before the screen, because this app has no testnet cluster and an unknown chain has no RPC.

The signature returned to the dApp is this account's 64-byte slot. The RPC result is accepted only when it decodes to those same bytes. That keeps a partial sign from reporting the fee payer's signature as if this account had produced it. A failed send leaves the protocol request open so the same signed bytes can be sent again. Reject is the way to abandon it.

The wallet does not wait for confirmation. The dApp asked the wallet to send, and `sendTransaction` returning a signature is that step. A confirmation wait would hold the approval screen after the request is already submitted.

## What was dropped

AI analysis, a confirmation commitment, testnet as a wallet cluster, a new Activity, and a new dependency. `sendTransaction` is the existing `SolanaJsonRpc` call. The legacy-only `WalletModel.signAndSend` helper is not reused, because it only signs a legacy payload and would skip the 0.7.0 partial-sign rules.
