# 0.5.0 — handoff

## 狀態

`in progress`。設計審查通過。Track A／B 已寫進 `ui/MwaScreens.kt`、`LocalAssociation.kt`、`mwa/MwaPayloads.kt`。`solana-wallet:` 由 `MwaActivity` 接。手測：`bun run playground`。下一動：INDEX 手驗 1–6，然後實作審查。

## 讀檔順序

`INDEX.md` → `docs/reasoning.md` → `docs/how.md` → 0.1.0 workflow MWA 節 → `LocalAssociation.kt`。

## 產品摘要

MWA 連線、簽署訊息、簽署並送出。畫面對齊 `connect-dapp` 與 `approve-sign-tx` 骨架；三面都有 Lock；不出貨 refresh／第二 mint／即時 AI。手測用 `bun run playground`。`sign_transactions` 仍 decline。待批只在記憶體。`Pending` 欄位見 how Track A。

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
