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

Tech design written. Design review: no open HIGH; M1–M6 closed.

## Next session

Track A: create `android/` (`com.airwave.wallet`) and vault unit tests.

## Paste-ready

```text
Implement Airwave Wallet App 0.1.0 Track A only.
Read dev-docs/roadmap/0.1.0/INDEX.md, docs/how.md, and docs/reasoning.md.
Set INDEX status to in progress when code starts.
Create android/ (application id com.airwave.wallet) and unit tests for the password-boxed vault airwave.vault.v1.
Do not start Track B. Do not commit unless asked.
```
