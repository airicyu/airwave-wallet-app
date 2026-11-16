# 0.10.0 — Source structure

- **Status:** `shipped` (2026-11-16). Matches the roadmap table. Changelog: [`changelog.md`](../../../changelog.md) heading `0.10.0 — 2026-11-16`.
- **Previous:** [0.9.0](../0.9.0/INDEX.md) (`shipped`)

## Product

No new screen and no new behavior. This version splits two oversized Kotlin files and moves existing code into the packages named in [`dev-docs/code-design.md`](../../code-design.md). Signing, submit, vault storage, and routes stay as they were in 0.9.0.

## Read order

1. This file
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)

## Decided

| Topic | Decision |
|-------|----------|
| Wire split | `SignTxCodec` reads and writes little-endian integers and short vectors. `SignTxV1` decodes, edits, and signs a v1 message. `SignTxWire` keeps legacy and v0. Callers still use `SignTxWire` for inspect, budget write, partial sign, and the compiled view |
| Combined screens | `CombinedManageScreens` draws the manage menu and the member sheet for an existing combined row. `CombinedScreens` keeps create, add, paste, and local pick |
| Packages | The root package keeps `AirwaveApp`, `MainActivity`, and `MwaActivity`. `account` holds vault-row edits and the account screens. `settings` holds the settings store and the settings screens. `shell` holds `WalletApp`, the home shell, and the route enums `Route`, `Tab`, and `HomeDestination`. `home` holds the assets screen. `session` keeps `WalletModel` and the in-memory approval. `ui` keeps shared chrome. `vault`, `holdings`, `mwa`, and `mwa/tx` stay |
| Behavior | Routes, storage keys, copy, and request results do not change. `AppStrings.kt` stays the message catalog and is not split |
| Not this version | A new feature. A new Activity. A behavior change to sign-and-send, accounts, or settings |

## Still to decide before implementation

(empty)

## Not this version

| Item | Where it goes |
|------|----------------|
| New product behavior | A later version |

## Acceptance

From `android/`, set `JAVA_HOME` to the Android Studio JBR:

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:testDebugUnitTest
```

The command passes. No screen copy changes. `SignTxWire`, `SignTxV1`, `SignTxCodec`, `CombinedScreens`, and `CombinedManageScreens` are each at or under 400 code lines. `AppStrings.kt` may stay over 400.
