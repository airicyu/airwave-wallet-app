# 0.4.0 — 錢包帳戶

- **狀態：** `shipped`
- **Changelog：** [`changelog.md`](../../../changelog.md) 標題 `0.4.0 — 2026-10-26`
- **上一版：** [0.3.0](../0.3.0/INDEX.md)
- **對齊現況：** 對準 0.3.0 之後的樹：`HoldingsModel`、`WalletSettings.kt`、已拆的 `ShellScreens`／`RpcScreens`／`SettingsScreens`。遷移完成前 vault 仍是 `{seed}`。設定檔 `airwave.settings.v1` 在本版之前沒有 `activeAccountId`。
- **Design demo：** [`wallet-overall-v1.html`](../../design-demos/wallet-overall-v1.html) 畫面 `accounts`、`add-account`、`add-generate-seed`、`add-generate`、`add-import`、`add-import-seed`、`add-import-secret`、`add-watch`。不實作 `add-combined*`／`aggregate-create`。

## 產品

把空的 Wallet accounts 換成真實清單。使用者可新增 12 詞帳戶、另一個 burner、匯入助記詞或密鑰、或 watch 地址，並選擇哪個帳戶為作用中。Home 資產跟該地址。Combined 列可見，但不建立帳戶。

## 閱讀順序

1. 本檔
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)
4. [0.3.0 INDEX](../0.3.0/INDEX.md)（本版延續的 holdings session 規則）

## 已定案

| 主題 | 決定 |
|------|------|
| Store | 仍是單一 vault 檔 `airwave.vault.v1`。密碼箱內明文 JSON 改為 `accounts` 陣列。設定只多 `activeAccountId`。沒有第二個 vault 檔 |
| 遷移 | 解鎖時，若正文是 `{ "seed": "<base64>" }` 且沒有 `accounts`，變成一個 `kind: "burner"` 帳戶。id 為隨機 UUID。`activeAccountId` 為空時設成該 id。舊的 `seed` 欄不再留下。同一輪解鎖、遷移成功後立刻再封存 vault，讓 `accounts` 在使用者離開 Home 前已持久化 |
| Vault apply 失敗 | 無論正文是舊 `{seed}` 還是 `accounts` 陣列，正確密碼打開後若明文無法套用，只走一條失敗路徑：不進 Home、清掉 session 密鑰、畫面用既有錯密碼風格（`password.wrong`／各語系對應）。不要另加「打不開 vault」字串 |
| Holdings session | 延續 0.3.0：`lock` 與改 `activeAccountId` 都作廢 holdings round id；遲到的 HTTP 不得畫上畫面。`lock` 清掉 session 簽名材料並呼叫 `holdings.clear()`。產生／匯入草稿只在記憶體；`lock` 丟棄，上鎖後不得 commit。Pending MWA 只在記憶體；`lock` 使其失敗；行程死掉不恢復 |
| Null endpoint | 與 0.3.0 相同：沒有 endpoint 時不設 `loading`、不抓取。切帳戶對新地址呼叫 `HoldingsModel.sync`，在檢查 endpoint 之前不設 `loading`。切換後該輪 holdings 失敗仍用 `Couldn't load balances.`／`讀不到餘額。`／`读不到余额。` |
| Kinds | `seed`（BIP39）、`burner`（32-byte 隨機）、`secret`（匯入密鑰）、`watch`（只有地址）。Watch 沒有 secret 欄 |
| 產生 12 詞 | BIP39 English、128 bit 熵、路徑 `m/44'/501'/0'/0'`。畫面對齊電話框 `add-generate-seed`：選填名稱、紅色警告「離開後無法再顯示助記詞」、右上紫色圓形骰子再產生、12 格唯讀詞（三列）、截斷公鑰＋複製鈕（只複製公鑰，不複製助記詞）、底欄「建立」。Commit 只把助記詞寫進 vault 密文。Commit 後與 `lock` 時從記憶體清掉詞 |
| 匯入助記詞 | 電話框 `add-import-seed`：提示貼上 12／24 詞、編號 12 格（貼滿 24 詞則擴成 24 格）、計數「已填 n／12」、底欄「下一步」。checksum 通過後才進第二步（demo 未畫）：路徑種類 `phantom`／`cli`／`change`（預設 phantom），預覽索引 0 到 4 選一個。沒有自訂路徑字串 |
| 匯入密鑰 | 電話框 `add-import-secret`：選填名稱、密鑰 textarea（placeholder `base58 或 [193, 240, …]`）、綠點提示「base58 或 [bytes]」、底欄「匯入」。接受 Base58 的 32 或 64 bytes，或 JSON 數字陣列同長度。64-byte 取前 32。其他長度拒絕 |
| Burner | 電話框 `add-generate`：選填名稱、紫色骰子再產生、截斷公鑰＋複製、底欄「產生」、無助記詞格。產生器與 0.1.0 相同，追加進 `accounts` |
| Watch | 電話框 `add-watch`：選填名稱、地址欄（placeholder Solana 公鑰）、底欄「建立」。Base58 解碼須為 32 bytes。任何 kind 的重複地址都拒絕 |
| 新增帳戶畫面 | 電話框 `add-account`（[`wallet-overall-v1.html`](../../design-demos/wallet-overall-v1.html)）。**五**列，不是六列。每列是 icon（36dp 圓角方）+ 標題 + 一句副標。Glyph 是從 web StrokeIcon 抄成的 vector XML：pin、flame、download、eye、layers（`ic_stroke_pin`、`ic_stroke_flame`、`ic_stroke_download`、`ic_stroke_eye`、`ic_stroke_layers`）。文案對齊 web `onboarding.create*`／`onboarding.chooser.*`（en／zh-Hant／zh-Hans）。不要出貨只有動詞的純文字清單 |
| Import 列 | 第三列是單一 **Import wallet** 列。打開子選單，兩列標題+chevron（Seed phrase、Secret key）。不要把助記詞與密鑰拆成五列清單上的兩列 |
| Combined 列 | 五列的第五列。打開短畫面：combined 帳戶尚未提供，只有 Back。不寫入任何帳戶 |
| 清單 | 作用中那列有 badge 與 accent 邊框。Kind 標記：鑰匙 `#D08B5B`，watch 用眼睛。列上可改名、刪除。最後一個可簽名帳戶不得刪。刪掉作用中帳戶時，若還有可簽名帳戶就選它，否則選另一個 watch |
| Pill | **相對 0.3.0 的變更：** Home pill 打開帳戶清單（0.3.0 不可點）。選一列會設 `activeAccountId`、pop 回 Home，並為該地址刷新 holdings |
| 導航 | `HomeDestination.Accounts` 取代 `EmptyAccounts`。選單 **Wallet accounts** 與 Home pill 都 `push(Accounts)`。新增帳戶流程在 home stack 上 push 子目的地。匯入子選單是 `HomeDestination.ImportChooser`。Combined 尚未提供是 `HomeDestination.CombinedLater` |
| 簽名 | MWA authorize 與 sign 用 `activeAccountId` 解析出的作用中帳戶密鑰。若 kind 是 `watch`，`offer` 在建立 pending approval 之前失敗該請求，字串 `mwa.watchAccount`（`AppStrings` 三語系）。解鎖期間 session 記憶體只持有作用中簽名密鑰；`lock` 清零 |
| 衍生 | BIP39 PBKDF2 與 SLIP-0010 ed25519 走既有依賴 Bouncy Castle。不加新套件。英文詞表獨立成檔 |
| DOMAIN | 功能碼之前，先把錢包帳戶 kinds 寫進 [`DOMAIN.md`](../../DOMAIN.md)。Combined wallet 仍在 Not adopted |
| 畫面地址 | 所有 UI 截斷走 `BurnerKeys.short`：前 4 字 + `…` + 後 4 字（對齊 web）。複製仍是完整地址。長度不大於 8 則整段顯示 |
| Trust 盾 | 驗證盾對齊 demo `.token-trust--verified`：26dp 圓角方、底是 `Ok` 18% 混 `BgElevated`、16dp 盾置中。`organicScore` 徽章貼在盾右下，不是獨立文字列 |
| 檔案形狀 | 新檔放 `vault` 與 `ui`。不要把衍生邏輯塞進 `WalletModel`。檔首英文職責註解依 [`code-design.md`](../../code-design.md)：新 `.kt`（含空白）不超過 400 行；單一職責畫面只有本 INDEX 點名時才可暫留 400 以上 |

## 實作前仍待決定

（空）

## 本版不做

| 項目 | 去處 |
|------|------|
| Token detail、send、activity、address book、建立 combined 帳戶 | 未開版。沒有 INDEX。不要實作 |
| 一次 commit 多個衍生索引、自訂路徑文字 | 不在本長任務 |
| Commit 後再顯示助記詞 | 不在本長任務 |

## 驗收

在 `android/`，`JAVA_HOME` 設為 Android Studio JBR（本機 `C:\Program Files\Android\Android Studio\jbr`）：

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

模擬器：

1. 既有 0.3.0 vault 解鎖成一個 burner，仍是作用中帳戶，Assets 仍會載入。
1a. 新增帳戶顯示五列 icon+標題+副標。Import 是一列；點進去是助記詞／密鑰加 chevron。助記詞內頁是 12 格＋下一步；拋棄式有骰子與「產生」；密鑰有名稱＋textarea＋「匯入」；觀察帳戶有名稱＋地址＋「建立」。Combined 不寫入。
2. 產生 12 詞帳戶、reroll、commit。上鎖再開。清單有新地址。Logcat 沒有那些詞。
3. 匯入 watch 地址。Assets 載入。MWA authorize 以 watch 錯誤失敗，不開 approval。
4. 切回 burner。Assets 跟該地址。Combined 新增列不建立帳戶。
5. 殺掉 app。帳戶清單與作用中 id 仍在。進行中的 MWA approval 不在。

單元測試：故意無效的 12 詞清單要被 BIP39 checksum 拒絕；密鑰匯入長度拒絕；可選：僅用**公開** BIP39 測試向量核 phantom 路徑地址（倉庫裡不要放有資金的助記詞）。不要 commit 真實或有資金的助記詞。
