# 0.2.0 — HANDOFF

## Done this session

- Tracks **A–D** implemented under `android/` (theme, hamburger + Network, settings store + screens, MWA sign-and-send RPC wiring).
- `gradlew` (via Android Studio JBR): `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug` **green** (2026-10-19).
- INDEX / roadmap README → **`shipped`** (2026-10-20). App `versionName` **0.2.0**. Root `version.md` **0.2.0**.
- Mobile UI pass: `wallet-020.html`, unlock chrome, Home tabs, PickRow Network/Language, inline confirm/discard alignment.
- **Hand-tests A–E**: run locally per INDEX when validating a release build; waived in `implementation-review.md` at user ship.

## Read order

1. `INDEX.md`
2. `docs/reasoning.md`
3. `docs/how.md`
4. `dev-docs/design-demos/wallet-020.html`
5. `android/`

## Product summary

Hamburger (placeholders + Network + Settings + Lock), `airwave.settings.v1`, Airwave blue theme, MWA submit keyed off **request chain** with optional per-cluster custom RPC.

## Track order

A Theme → B Hamburger + Network → C Settings + submit wire → D assemble / test / lint + hand-tests A–E.

## Out of bounds

No real accounts / address-book CRUD / connected-sites / about; no cluster on RPC; no eye in secret edit; no AI assess; no pending on disk; do not edit the Chrome extension repo; do not commit unless asked.

## Paste-ready starter

```text
Implement Airwave Wallet App 0.2.0 from the contract only.

Read: INDEX.md → docs/reasoning.md → docs/how.md → design-demos/wallet-020.html → android/.
When starting code: set INDEX and roadmap README status to in progress.
Tracks: A → B → C → D. Windows + Android Studio emulator.

Must: EncryptedSharedPreferences JSON airwave.settings.v1; Menu dropdown ownership (how.md); Network cluster vs RPC endpoints; SecretKeyField idle eye+pencil / edit confirm+discard only; AI Default hardcoded chains / Custom slots; MWA sign-and-send RPC = request chain then settings endpoints (how.md); keep 0.1.0 vault/MWA.
Must not: fill empty menu products; cluster on RPC; eye in edit; AI assess; persist pending approvals; invent semantics not in INDEX/how; edit airicyu/airwave-wallet; commit unless asked.

Done when: INDEX hand-tests A–E recorded; gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug green.
```

## Next session

1. Hand-test INDEX A–E on Windows + Android Studio emulator; record in `docs/implementation-review.md`.
2. Spawn **實作審查** → fix findings → ship when user says ship.
