# 0.10.0 — how

The moves are already in the tree. Do not change call results.

## Track A — Splits

`SignTxCodec` and `SignTxV1` sit beside `SignTxWire` in `mwa/tx`. `CombinedManageScreens` sits beside `CombinedScreens` in `account`.

## Track B — Packages

Account operations and account screens are `com.airwave.wallet.account`. Settings operations and settings screens are `com.airwave.wallet.settings`. `WalletApp`, `ShellScreens`, and `ShellRoute` are `com.airwave.wallet.shell`. `HomeAssetsScreen` is `com.airwave.wallet.home`. Shared chrome stays in `com.airwave.wallet.ui`.

## Commands

From `android/`:

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:testDebugUnitTest
```
