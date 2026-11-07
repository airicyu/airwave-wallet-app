# 0.5.0 — handoff

## 狀態

`shipped`（2026-11-07）。`version.md` 與 Android `versionName` 是 `0.5.0`。Playground Connect 已由使用者手測通過。不要開 0.6.0，除非使用者點名。

## 讀檔順序

`INDEX.md` → `docs/reasoning.md` → `docs/how.md` → 0.1.0 workflow MWA 節 → `LocalAssociation.kt`。

## 產品摘要

MWA 連線頁（`connect-dapp`）。簽署訊息是 0.6.0。簽署交易未排程，三個 sign 方法都直接 decline。手測用 `bun run playground` 的 Connect。待批只在記憶體。

## Track 順序

A 批准 chrome → B LocalAssociation 組裝 → C 測試頁手驗。

## 禁區

Pending 不上磁碟。硬編碼密碼。即時 AI。只簽不送。`approve-sign-tx-fail`。改 extension 倉庫。

## 完成檢查

INDEX 驗收 1–6；`:app:assembleDebug` `:app:testDebugUnitTest` `:app:lintDebug`。

## 可貼上

```text
0.5.0 is draft. Read dev-docs/roadmap/0.5.0/INDEX.md then docs/how.md. Do not implement until design review has no open HIGH. Do not commit unless asked. Do not push. Work only in airwave-wallet-app.
```
