# 0.1.0 — how

Engineering contract. Product paths: [`workflow.md`](./workflow.md). Screens: [`dev-docs/design-demos/wallet-010.html`](../../../design-demos/wallet-010.html).

## Stack

| Layer | Choice |
|-------|--------|
| Platform | Native **Android** |
| UI | **Jetpack Compose** + Material 3, dark scheme matching the demo |
| Language | **Kotlin**, JVM **17** |
| Layout | Gradle root **`android/`**, module **`:app`** |
| Application id | `com.airwave.wallet` |

No Flutter or Dart application code.

## Dependencies

| Need | Artifact | Why |
|------|----------|-----|
| MWA wallet endpoint | `com.solanamobile:mobile-wallet-adapter-walletlib` 2.x | Spec wallet role; local association |
| Solana bytes / base58 | `com.solanamobile:web3-solana` and `io.github.funkatronics:multimult` as walletlib requires | Pubkey and transaction bytes |
| Vault at rest | AndroidX Security encrypted file | Wraps that one JSON file with the device keystore. It does not replace the password box |
| Password KDF | `com.lambdapioneer.argon2kt:argon2kt` (Argon2id) plus JDK AES-GCM | The JSON blob is the password box. The password is not stored |

Pin versions in `android/app/build.gradle.kts` when the module is created. Record the pins in `reasoning.md` if they change.

## Durable store

One store. Key **`airwave.vault.v1`**: a JSON file (`kdf`, `salt`, `nonce`, `ciphertext`). The ciphertext is the password box: Argon2id derives the AES-GCM key from the app password. The password is not in the file. AndroidX Security encrypted file only wraps this file; unlocking still requires the app password. The vault holds zero or one burner keypair. No pending-approval key. No second rehydrate path.

## Session

Process memory only: unlocked flag, decrypted secret bytes, public address, and at most one pending MWA request. The session owner creates the approval screen for that request and disposes it when Approve finishes, Reject finishes, Lock runs, or the request fails. The approval composable is not kept above that owner across later screens. **Lock** zeroes secret bytes and fails any open MWA request. Process death does the same for the in-flight request; the vault file remains.

## Screens and copy

English copy below is normative for 0.1.0 (same sentences as the design demo).

| Screen | Copy |
|--------|------|
| Setup | Title `Airwave`. Lead `Set a password to lock this wallet.` Fields Password, Re-enter. Button `Get started`. Mismatch: `Passwords do not match.` Shorter than 8 characters: `Password is too short.` |
| Unlock | Title `Unlock`. Lead `Wallet locked.` Button `Unlock`. Wrong password: `Wrong password.` |
| No account | Lead `No wallet account yet.` Button `Create burner wallet` |
| Generate | Title `Create burner wallet`. Lead `A new key on this device. The secret is not shown.` Label `Solana address`. Short address on screen. Copy control accessible name `Copy address` puts the full preview address on the clipboard. Buttons `Roll again`, `Generate`. No password field |
| Home | Titles `Assets` / `Activity`. Empty: `No assets yet.` / `No activity yet.` Button `Lock`. Copy control accessible name `Copy address` |
| Connect | Title `Connect`. Buttons `Reject`, `Approve`, `Lock`. Shows app name, site, chain, account |
| Sign message | Title `Sign message`. Shows app, account, full message |
| Sign and send | Title `Sign and send`. Shows app, chain, from, to, send amount, fee, instruction name. Fee and destination stay on the screen |

## MWA

Intent filter: `solana-wallet` local association only (`/v1/associate/local`). Localhost WebSocket. No custom scheme.

| Method | Behavior |
|--------|----------|
| `authorize` | If there is no vault or no burner account, fail this request and do not keep it for later. If the vault is locked and an account exists, Unlock first, then show this approval. Returns `auth_token` and the one account. Chain does not switch keys |
| `deauthorize` | Drops that auth token. No extra screen |
| `get_capabilities` | Reports `solana:signMessages` and `solana:signAndSendTransaction` only |
| `sign_messages` | Same no-vault / no-account failure as `authorize`. Otherwise approval, then ed25519 signature |
| `sign_and_send_transactions` | Same no-vault / no-account failure. Otherwise approval, sign, submit, return signatures. Show each instruction name. Show destination and amount only when an instruction is a system transfer. Fee is a `BigInteger` from a string, taken from simulation or the fee field on the public RPC, shown on the screen (SOL text may use `BigDecimal` for display only). If a destination, amount, or fee is required by that rule and cannot be parsed, fail before signing. Unknown chain fails before signing |
| `sign_transactions` | **Not implemented.** Fail that request immediately. Not advertised |
| SIWS, clone authorization, remote/QR/BLE/Nostr | Out of scope. Fail if a client requires them |

At most one pending approval in memory.

**Submit RPC** (no settings screen in 0.1.0): map the request chain to a public Solana RPC and POST there.

| Chain (or legacy cluster) | URL |
|---------------------------|-----|
| `solana:mainnet` / `mainnet-beta` | `https://api.mainnet-beta.solana.com` |
| `solana:devnet` / `devnet` | `https://api.devnet.solana.com` |
| `solana:testnet` / `testnet` | `https://api.testnet.solana.com` |

Unknown chain: fail that request before signing. Lamports and fees are `BigInteger` built from decimal strings. Do not use `double`. Display-only SOL text may use `java.math.BigDecimal` and must not be what gets signed.

## Tracks

| Track | Work | Done when |
|-------|------|-----------|
| **A** | `android/` scaffold, vault create/unlock unit tests | `testDebugUnitTest` covers password vault |
| **B** | Setup, Unlock, lead, generate, Home tabs | Hand-test INDEX A–B |
| **C** | walletlib association + three approval screens + submit | Hand-test INDEX C |
| **D** | lint + record hand-test in HANDOFF | INDEX commands and A–D |

Order A → B → C → D.

## Commands

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```
