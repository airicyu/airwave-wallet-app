# 0.5.0 — how

對齊 0.4.0 之後的樹。套件 `com.airwave.wallet`。

## 未歸版併入

手測用官方 Solana Mobile playground。倉庫根 `bun run playground`。不要改成 extension Wallet Standard。本版刪除 `mwa-test-web/`。

## Track A — 批准畫面 chrome

`WalletApp` 只在 `Route.Approval` 時建立核准 composable（`ui/MwaScreens.kt`）。`pending == null` 或離開 `Route.Approval` 後該 composable 不得留在樹上。Approve／Reject／Lock／失敗都經 `WalletModel` 清掉 `pending`；不要在畫面檔另存一份請求。

`Pending` 具名 `kind`：`Authorize`、`SignMessage`、`SignAndSend`。欄位（需要的才填，其餘 null）：`identityName`、`identityUri`、`chain`、`accountLabel`、`accountShort`、`messageUtf8: String?`、`messageHex: String?`（二擇一顯示）、`amountLamports: BigInteger?`（Transfer 支出為帳戶付出的正整數，顯示時加負號，不要用 `double`）、`feeLamports: BigInteger?`、`instructionNames: List<String>`。禁止把金額只塞進 `Pair<String,String>` 再在 UI 用浮點解析。可刪除舊 `lines`。

連線／簽訊息／簽交易版面對齊 INDEX。返回＝`reject()`。三面右側 Lock＝`lock()`。簽交易不出貨 refresh、不出貨第二 mint 列。

文案鍵與三語見 INDEX。英文是源文案，進 `AppStrings`。

**驗收：** 靜態對照電話框列順序與 glyph 檔名。手驗 INDEX 1–2。

## Track B — LocalAssociation 組裝

`solana-wallet:` 只進 `MwaActivity`。`MainActivity` 不再 register 該 intent。`WalletModel` 活在 `AirwaveApp` 的 ViewModelStore，兩個 Activity 共用同一解鎖 session。

`createScenario` 的 callbacks 必須是 `LocalScenario.Callbacks`（含 `onLowPowerAndNoConnection`）。只實作 `Scenario.Callbacks` 會在 associate 時崩潰，WebSocket 不會綁埠。

`onAuthorizeRequest`／`onSignMessagesRequest`／`onSignAndSendTransactionsRequest` 組出 Track A 的 `Pending`，仍只經 `WalletModel.offer`。`offer`：無 vault、或無地址且不是 Unlock → 立刻 `finish(Failed)`。有 pending 且 `address == null` 且 `route == Unlock`：維持先 Unlock，解鎖成功後進 `Route.Approval`（現碼已如此，不要拆掉）。Watch 與第二筆 pending 的拒絕留在 `offer`。

對照官方 FakeWallet（`MobileWalletAdapterActivity` + ViewModel）的 **整個 session**，不是只對 Connect：

| 步驟 | FakeWallet | 本錢包 |
|------|------------|--------|
| 入口 | 獨立 Activity、`singleTask`、自己的 `taskAffinity`、不透明 | 同左（`com.airwave.wallet.mwa`） |
| Intent | `solana-wallet` scheme；無 data 則 `finish()` | 同左 |
| 啟動 Scenario | ViewModel `processLaunch`：`createScenario(...).also { it.start() }`；Activity 銷毀不關 Scenario | 同左：Scenario 掛在 `WalletModel`；先 `start()` 再 Compose；舊 session teardown 不得 `finish()` 新的 |
| 等待第一個 request | 一開始就有 layout／NavHost | 品牌標 + Airwave，不要空白黑底 |
| authorize／sign message／sign-and-send | 換成對應 request 畫面 | `WalletModel.pending` → `MwaScreens` |
| 使用者批准 | 只 `completeWith*`，畫面留著 | 同左：`approvalSettled`，pending 不清到 teardown |
| reauthorize | 靜默 complete，不 finish Activity | 同左 |
| serving complete | main 上 `scenario.close()`，request 清成 None | main 上 `close()`；teardown 呼叫 `endAssociation()` |
| teardown | `SessionTerminated` → `finish()` | `markMwaHostFinished()` → Activity `finish()` |
| 低電量／啟動失敗 | dialog 後 finish | 不自動 `finish()`（避免 3 秒內還沒連上就把 session 殺掉） |
| 新 associate | 換 session | `processMwaLaunch` close 再 `start()`；已 settled 的不得再 `completeWithDecline` |

INDEX 刻意不做、因此不抄 FakeWallet：SIWS、`sign_transactions` 當能力、identity 驗證、versioned tx。Watch 帳戶與 0.5.0 電話框維持本產品契約。

`onReauthorizeRequest`：可簽名且已解鎖 → `completeWithReauthorize()`。鎖著先 Unlock。Sign messages：`addresses` 對不上作用中公鑰 → `completeWithAuthorizationNotValid()`。Unlock 與 watch 錯誤畫面不退。

`onSignTransactionsRequest` 維持 `completeWithDecline()`。capabilities 維持 `solana:signMessages` 與 `solana:signAndSendTransaction`。

Sign-and-send：未知 chain／`rpcForSubmit` 空／payload 空或缺欄 → 不 `offer`，直接 decline。摘要與手續費在 worker（`getFeeForMessage`），再主執行緒 `offer`。顯示 `formatBaseUnits(lamports, 9)`。使用者批准後簽名或送出失敗走 `completeWithInternalError`，不要再 `completeWithDecline`（playground 會印成 declined）。使用者拒絕／Lock 才 decline。

Sign messages：`payloads` 空或第一筆空 → 不 `offer`，decline。否則把第一筆 bytes 試 UTF-8；失敗則填 `messageHex`，不要只呼叫 `decodeToString()`。

**驗收：** INDEX 手驗 3–6 與單元測試。

## Track C — 測試頁對齊

根 `package.json` 的 `playground` 腳本能在未把 `adb` 加進系統 PATH 時啟動官方 playground。不維護本倉庫內的測試 dApp。SIWS 不實作。

**驗收：** INDEX 裝置步驟 1–3。

## 檔案形狀

新檔英文職責註解（型別宣告上）；程式列 ≤ 400（空行與註解不算）。不要把 Compose 與 walletlib callback 塞進同一個檔。`AppStrings.kt` 可超過 400。
