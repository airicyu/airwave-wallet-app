# 0.4.0 — how

對齊 0.3.0 之後的樹。套件 `com.airwave.wallet`。

## Track A — Vault 清單與遷移

`vault/AccountRecord.kt`：id（UUID 字串）、kind（`seed` | `burner` | `secret` | `watch`）、label、address、可選 secret 負載（助記詞或 seed bytes；`watch` 沒有）。

`vault/VaultAccounts.kt`：解析明文 JSON；把舊 `{ "seed": "<base64>" }` 遷成單一元素的 `accounts` 陣列；序列化時不要頂層 `seed` 鍵；插入時偵測重複地址。

`WalletModel` 解鎖：打開密文 → `VaultAccounts.applyPlain` → 設記憶體帳戶清單，並從設定解析 `activeAccountId`（空則用遷移出的 id）→ 把作用中簽名密鑰載入 session → 若有遷移，**同一輪解鎖再封存** vault → `Route.Home`，並在適用時 `syncHoldings(false)`。AEAD 打開後 apply 失敗：清 session，顯示 `password.wrong`（與 INDEX vault apply 失敗同一條路）。

`changePassword` 封存的是目前的 `accounts` 明文，不是舊 `{seed}`。

設定：`AirwaveSettings` 加 `activeAccountId: String`；JSON 鍵 `activeAccountId`；空字串表示「尚未遷移或尚未第一次挑選」。

其他功能碼之前先更新 `DOMAIN.md` 四種 kind。

**驗收：** 單元測試把 `{seed}` fixture 遷成一個 burner、拿掉 `seed`、再封存寫出 `accounts`。既有解鎖手驗仍進 Home。

## Track B — 清單、改名、刪除、pill

`ui/AccountsScreen.kt` 取代空帳戶畫面。在 `HomeShell` 與選單（`ShellScreens.kt` 選單列）接上 `HomeDestination.Accounts`。Home pill 改 `clickable` 並 `push(Accounts)`（0.3.0 的 pill 只顯示）。

列 UI：作用中邊框／badge、kind 標記、改名、刪除，依 INDEX。切列：`setActiveAccountId`、`pop()` 回 Home、`syncHoldings(refresh = false)`（holdings round id 依 INDEX 作廢）。

**驗收：** 切帳戶、殺行程、再解鎖 — 作用中 id 相同。Holdings 地址對齊作用中那列。

## Track C — 產生、匯入、burner、watch

`vault/Bip39.kt`：產生 128-bit 熵、核 checksum、PBKDF2 成 seed。`vault/Bip39English.kt`：只有詞表。`vault/Slip10.kt`：具名路徑種類的 ed25519 衍生。

chooser 在 `ui/AddAccountChooser.kt`（五列 StrokeIcon）。內頁在 `ui/AddAccountForms.kt`，共用 chrome 在 `ui/AddAccountChrome.kt`，對齊電話框 `add-generate-seed`／`add-generate`／`add-import`／`add-import-seed`／`add-import-secret`／`add-watch`。匯入助記詞第一步是詞格＋「下一步」，第二步才選路徑索引 0–4。Combined 仍是 `CombinedLater`，不寫入。

草稿狀態在 `WalletModel`（或小型 `AccountDraft`）：`lock` 時清掉；commit 經 `VaultAccounts` 寫入，並在 INDEX 要求時更新 `activeAccountId`。

`LocalAssociation`：`offer` 之前若作用中 kind 是 `watch`，以 watch 錯誤字串結束請求 — 不進 approval 路由。

**驗收：** INDEX 手驗 2–4。單元測試：無效 checksum；無效密鑰長度；可選公開 BIP39 向量，只核 phantom 索引 0 的地址字串。

## 檔案形狀

新檔有英文職責註解；含空白不超過 400 行，見 [`code-design.md`](../../code-design.md)。衍生邏輯不進 `WalletModel.kt`。
