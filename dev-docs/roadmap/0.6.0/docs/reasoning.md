# 0.6.0 — reasoning

## What ships

`sign_messages` becomes a real approval. 0.5.0 already draws `SignMessageBody` and already declines the callback. This version offers that pending, lists `solana:signMessages` in capabilities, and returns one ed25519 signature.

The user sees the bytes as UTF-8 or as lowercase hex. The wallet does not interpret Solana off-chain message layout. A preamble is just bytes.

## Why

The playground Sign message button is the next step after Connect. Advertising the feature without a screen would let a dApp request a signature the user never sees. Showing a parsed off-chain domain would invent a format this version refused.

Signing only when there is exactly one non-empty payload keeps the screen honest: one body, one signature. Extra payloads are declined instead of silently dropped.

## What was dropped

Off-chain message detection, SIWS, sign-and-send, and a new phone frame. No new dependency. `MwaPayloads` already chooses UTF-8 or hex.
