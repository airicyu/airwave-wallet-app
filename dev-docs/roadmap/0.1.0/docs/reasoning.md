# 0.1.0 — reasoning

## MVP

A generated burner after an **app password** is set, wallet home with widget, Assets/Activity tab shells (no content), and an MWA wallet endpoint — before balances, activity history, or import.

## Why native Android

1. **0.1.0 ship target** — one mobile OS for acceptance; iOS and desktop deferred.
2. **MWA, not a private scheme** — Solana Mobile’s wallet–dApp standard on Android is Mobile Wallet Adapter (association URI `solana-wallet:`, then WebSocket). Mandatory wallet features in spec 2.x are `solana:signMessages` and `solana:signAndSendTransaction`, after `authorize`. Phantom/Solflare-style custom deeplinks are a different, non-official path and are out of scope.
3. **Why these engineering defaults** — Deprecated `sign_transactions` is omitted so there is one signing path (`sign_and_send_transactions`). Submit uses Solana’s public cluster RPCs because 0.1.0 has no RPC settings screen. A user-configured RPC can replace that later without changing the approval UI.

## Dependencies (why)

| Package | Reason |
|---------|--------|
| `mobile-wallet-adapter-walletlib` | Wallet-side MWA session, association, and RPC methods |
| `web3-solana` and `multimult` | Public keys and transaction bytes the wallet library expects |
| AndroidX Security encrypted file | Wraps the one vault file with the device keystore |
| `org.bouncycastle:bcprov-jdk18on:1.78.1` | Argon2id password box and Ed25519. Replaces `argon2kt` so `testDebugUnitTest` can run without Android JNI |

Versions are pinned in Gradle when `android/` is created.
