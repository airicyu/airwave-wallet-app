# Research — Mobile Wallet Adapter (Android wallet)

Notes for **0.1.0**. Product decisions that follow from this live in [`roadmap/0.1.0/INDEX.md`](../roadmap/0.1.0/INDEX.md) and [`roadmap/0.1.0/docs/workflow.md`](../roadmap/0.1.0/docs/workflow.md). This file is background, not the contract.

## Sources

| Doc | URL |
|-----|-----|
| MWA 2.0 specification | https://solana-mobile.github.io/mobile-wallet-adapter/spec/spec.html |
| Protocol overview | https://docs.solanamobile.com/developers/mobile-wallet-adapter |
| Session / intent overview | https://docs.solanamobile.com/developers/mobile-wallet-adapter-deep-dive |
| Kotlin client usage (dApp side) | https://docs.solanamobile.com/android-native/using_mobile_wallet_adapter |
| walletlib 2.0 migration (wallet side) | https://docs.solanamobile.com/mwa/migration/wallets/walletlib |

Checked against the published 2.0 spec text (feature lists and local association URI). SDK version pins belong in `how.md` when implementation starts; the migration page cited `mobile-wallet-adapter-walletlib:2.0.0-beta1` as an example, not a pin for this repo.

## What the standard is

**Mobile Wallet Adapter** is Solana Mobile’s protocol for a dApp to talk to a **wallet app** on the same device: authorize accounts, then request signatures. The dApp sends requests; the wallet shows them and returns a result only to that session.

It is **not** a custom URL scheme owned by one wallet (Phantom, Solflare, and similar apps have shipped their own deeplinks). Those are out of scope here. The association scheme in the spec is **`solana-wallet:`**.

## How a session starts (local, Android)

1. The dApp opens a **local association URI**:

   `solana-wallet:/v1/associate/local?association=<association_token>&port=<port_number>&v=<version>`

   The first path segment (`v1`) is protocol-version negotiation. Breaking protocol changes increment that segment.

2. If a wallet has registered an Activity for that scheme and shape, Android launches it.

3. The wallet starts a **WebSocket server on `localhost` only**, path `/solana-wallet`, on `port_number`, and keeps listening for **at least 10 seconds**. The dApp connects to `ws://localhost:<port>/solana-wallet`.

4. WebSocket subprotocols: `com.solana.mobilewalletadapter.v1` (binary frames) or `com.solana.mobilewalletadapter.v1.base64` (UTF-8 Base64 text). For a local URI the dApp offers both; the wallet picks one. The wallet, as server, sends periodic `PING` frames.

“Register” in this design means **the intent filter** (and then hosting that socket). It is not a separate user-facing registration screen.

The spec also defines remote association (reflector host, QR, clipboard), optional `https` URI prefixes (App Links) returned as `wallet_uri_base`, Bluetooth LE, and Nostr. **0.1.0 uses local association only.**

## Methods relevant to a wallet endpoint

Non-privileged / session:

| Method | Role |
|--------|------|
| `authorize` | User grants the dApp. Result includes `auth_token` and one or more accounts. Account `address` in the JSON result is **base64** public key bytes; display form is chain-specific (`display_address` when present). Optional inputs: chain, identity (name, uri, icon), features, addresses, prior `auth_token` (silent reauthorize), Sign In With Solana payload. |
| `deauthorize` | Revoke that authorization. |
| `get_capabilities` | Wallet reports supported chains and features. |
| `reauthorize` | **Deprecated** in 2.x (folded into `authorize` + `auth_token`). |

Privileged (after a successful authorize for that session):

| Feature id | Method | Spec status |
|------------|--------|-------------|
| `solana:signAndSendTransaction` | `sign_and_send_transactions` | **Mandatory.** Wallet signs, submits, returns signatures. |
| `solana:signMessages` | `sign_messages` | **Mandatory.** |
| `solana:signTransactions` | `sign_transactions` | **Deprecated.** Optional compatibility only; advertise via `get_capabilities` if implemented. |
| `solana:signInWithSolana` | extension of `authorize` | Optional. |
| `solana:cloneAuthorization` | `clone_authorization` | Optional. |

Chain identifiers: `solana:mainnet`, `solana:testnet`, `solana:devnet`. Older `cluster` values `mainnet-beta`, `testnet`, `devnet` remain accepted for compatibility.

## What this repo takes from that

- Wallet implementation target: Android **`walletlib`** (wallet role), not `clientlib` (dApp role) and not a hand-rolled Phantom-style scheme.
- User-visible MWA surfaces: approval for `authorize`, `sign_messages`, and `sign_and_send_transactions`.
- Not in 0.1.0: SIWS, clone authorization, remote/QR/BLE/Nostr association, a private deeplink protocol.
- Open: whether to implement deprecated `sign_transactions` for old dApps; which RPC submits `sign_and_send_transactions`.
