# 0.4.0 — handoff

## 狀態

`shipped`（2026-10-26）。`version.md` 與 Android `versionName` 是 `0.4.0`。不要開 0.5.0，除非使用者點名。

## 可貼上

```text
0.3.0 and 0.4.0 are shipped. version.md is 0.4.0. Do not open the next version unless the user names it. Read dev-docs/roadmap/README.md. Do not commit unless asked. Do not push.
```

## 程式地圖

- Vault：`vault/AccountRecord.kt`、`VaultAccounts.kt`、`Bip39.kt`、`Bip39English.kt`、`Slip10.kt`；詞表 `res/raw/bip39_english.txt`。
- Session：`WalletModel.kt`、`WalletAccountOps.kt`；設定 `activeAccountId` 在 `SettingsModels.kt`。
- UI：`ui/AccountScreens.kt`、`ui/AddAccountChooser.kt`、`ui/AddAccountForms.kt`、`ui/AddAccountChrome.kt`。Combined 不建立。
