# 0.1.0 — HANDOFF

Design review passed (round 2). Status stays **`draft`** until implementation starts, then set INDEX and the roadmap table to **`in progress`**.

## Read first

1. [`INDEX.md`](./INDEX.md)
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)
4. [`docs/workflow.md`](./docs/workflow.md)
5. [`dev-docs/design-demos/wallet-010.html`](../../design-demos/wallet-010.html)

## Product

App password, then one generated burner. Home widget plus empty Assets and Activity. MWA wallet endpoint: authorize, sign messages, sign-and-send. No import, no custom deeplink.

## Tracks

A → B → C → D as in `how.md`. Do not scaffold before this handoff is the starting point of an implementation session.

## Do not

Persist a pending approval. Hard-code a password. Use `double` for lamports. Implement `sign_transactions`, SIWS, or a second store.

## Done this session

Track A: `android/` builds. `testDebugUnitTest` covers vault seal/open. `assembleDebug` succeeds.

Track B: Compose screens for setup, unlock, lead, generate, home.

Track C: `LocalAssociation` starts walletlib `LocalWebSocketServerScenario` for authorize and sign-messages. `sign_transactions` declines immediately. Sign-and-send declines until a transaction parser and RPC submit exist. Device hand-test A–D not run.

## Next session

Finish sign-and-send (parse system transfer, fee as BigInteger, submit to the public RPC), then run INDEX hand-test A–D on an Android device.

## Paste-ready

```text
Implement Airwave Wallet App 0.1.0 Track A only.
Read dev-docs/roadmap/0.1.0/INDEX.md, docs/how.md, and docs/reasoning.md.
Set INDEX status to in progress when code starts.
Create android/ (application id com.airwave.wallet) and unit tests for the password-boxed vault airwave.vault.v1.
Do not start Track B. Do not commit unless asked.
```
