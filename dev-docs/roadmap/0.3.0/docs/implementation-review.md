# Implementation review — 0.3.0 Airwave Wallet App

- 日期：2026-10-25（Asia/Hong_Kong）
- 輪次：**第 2 輪複審**（同一份報告累加；H／M／L 編號不重編）
- 角色：實作審查（不改程式、不加功能、不 commit）
- 對照基準：[`../INDEX.md`](../INDEX.md) 已定案＋驗收；[`how.md`](./how.md)、[`reasoning.md`](./reasoning.md)、[`../HANDOFF.md`](../HANDOFF.md)
- 架構禁區：[`../../GUIDELINES.md`](../../GUIDELINES.md)。檔案形狀見 [`../../../code-design.md`](../../../code-design.md)
- 對照程式：working tree（未以某一個 commit 為準）
- 本檔的 H／M／L 是實作審查自己的編號，與 [`design-review.md`](./design-review.md) 的編號不是同一批
- **總評：** 未關閉 **HIGH** 已經沒有（H1 本輪關閉）。INDEX 寫明的三個 Gradle 任務這次重跑通過。手驗沒有在裝置上走完。新的 **M6** 仍開，所以還不能出貨。

## 架構禁區（本版持倉路徑）

第 2 輪再抽樣 `holdings/`、`WalletModel.lock`／`pending`、`SettingsStore`、`VaultStore`。結論與初審相同。

- 待批仍是 `WalletModel.pending` 的記憶體物件。`lock` 呼叫 `failPending()`。持倉程式沒有把待批寫進檔案或 preferences。
- 耐久儲存仍是 vault `airwave.vault.v1` 與 settings `airwave.settings.v1`。`holdings/` 沒有寫這兩個 store。Skip 與 snapshot 只在 `HoldingsModel`。`lock` 的 `clear()` 會清掉 Skip。
- 持倉的 lamports 與 token base units 用 `BigInteger`。USD、organicScore 與 Helius 小數移位用 `BigDecimal`。這條路徑沒有 `double`、`Float`、`optDouble`、`toDouble`。
- 沒有新增第三方套件。HTTP 是 `HttpURLConnection`。
- Mainnet 沒有 active RPC 時 `RpcResolver.endpointForCluster` 回 null，不會改用 `PUBLIC_MAINNET`。
- 一輪有單調 id。`lock`、Skip、endpoint 為 null、以及新的一輪都會把 id 作廢。回應只有 id 仍是當前值才寫 snapshot。

## Findings

### HIGH

#### H1 — 餘額失敗時沒有重新整理 — **關閉**（第 2 輪）

初審：`HomeAssetsScreen` 在 `HoldingPhase.Failed` 只畫失敗文案。重新整理按鈕只在 `Ready` 與 `Idle`。失敗後使用者按不到 refresh，手測 6 的失敗分支不成立。

第 2 輪：`Failed` 現在有 `TextButton`，按下呼叫 `refreshHoldings()`。`HoldingsModel.sync` 的 3 秒冷卻只擋 `Ready`；失敗後 `inFlight` 已是 false，所以這一按會立刻 `start` 新的一輪，並先把畫面設成沒有舊列的 `loading`。

### MEDIUM

| ID | 本輪狀態 | 依據 |
|----|----------|------|
| M1 | **關閉** | 初審：改 RPC 只寫 settings，從 Settings hub 返回不會開輪。第 2 輪：`selectRpcEndpoint`、`addRpcEndpoint`、`removeRpcEndpoint`、`replaceRpcEndpoint`、`setCluster` 寫完 settings 都會 `syncHoldings`。`pop` 在堆疊回到空時也會 sync。endpoint 仍是 null 時走 `absentEndpoint`（`guide` 或 `skipped`），不抓取、不進入 `loading`。endpoint 從 null 變成 URL 時 key 對不上，會開新輪。從 Settings hub 進 RPC 再一路返回 Home，堆疊清空時會再 sync；若在 RPC 畫面已經選了 endpoint，那一輪當時就已經開始 |
| M2 | **關閉** | 初審：`JsonNumbers.numberToken` 把 `1.23e-4` 讀成 `1.23`，JSON 字串形式的數字會被當成沒有這個欄。第 2 輪：有引號的值只接受 `^\d+(\.\d+)?$` 整段；未加引號的數字在後面還有數字、小數點或 `e`／`E` 時不採用。`getBalance`、Helius `balance`、Jupiter `usdPrice`／`organicScore` 都走這個函式。`getBalance` 的 token 還必須是整數數字，否則整輪失敗 |
| M3 | **關閉** | 初審：Jupiter 只補到 name 或只補到 symbol 時，另一邊留著 short，畫面上會混用。第 2 輪：`JupiterMintMeta.patch` 在 name 或 symbol 有一邊是空的時候，兩個都留在列上原有的值；`fallback` 再把空的 name 和 symbol 都設成 `BurnerKeys.short`。兩邊都有文字才採用 Jupiter 的名字。原生 SOL 與 wrapped SOL 的名稱與符號維持寫死 |
| M4 | **關閉** | 初審：organicScore 只畫在盾裡面，未驗證的列不會畫徽章。第 2 輪：盾只在 `verified` 時畫，位置在圖示左邊。分數是盾旁邊的獨立文字，不包在盾裡；`organicScore` 只在非原生、非 wrapped、且 `BigDecimal` 判定大於 0 時保留，並顯示原本的數字文字。原生 SOL 與 wrapped SOL 的分數維持 null |
| M5 | **關閉** | 初審：`AppStrings.kt` 沒有英文職責註解。第 2 輪：檔首有英文 block comment，說明它是 en／zh-Hant／zh-Hans 的字串目錄，不到 100 字。`WalletModel.kt` 與 `ShellScreens.kt` 的註解仍在 |
| M6 | **仍開** | 沒有待批時，解鎖會 `syncHoldings`：`applyPlain` 把 route 設成 Home，接著才 sync。若解鎖時已經有 in-memory 待批，`unlock` 改走 `Route.Approval`，這次不開輪。使用者在批核畫面按拒絕後，`reject` 只把 route 設回 Home，沒有 sync。上一輪 `lock` 已經 `clear()`，所以不會看到舊清單，但新的一輪也不會自己開始，畫面停在 `Idle`（有重新整理按鈕、沒有列）。同意那條待批會在 `finishApproval` 開輪。INDEX 的觸發包含解鎖之後；這條拒絕路徑沒有走到 |

### LOW

| ID | 本輪狀態 | 依據 |
|----|----------|------|
| L1 | **關閉** | 初審：排序用 scale 8 的未縮放整數，和畫面上 half-up 到 2 位的金額可能不一致。第 2 輪：`usdSort` 與 `usdLabel` 都是 `divide(..., 2, RoundingMode.HALF_UP)`。排序比較的是這個 2 位金額的 `unscaledValue()`。原生 SOL、wrapped SOL 仍排在最前 |
| L2 | **關閉** | 初審：沒有 decimals 超出 0…18 的單測。第 2 輪：`HoldingRulesTest.dropsZeroDecimalsAndKeepsWrappedSeparate` 用 decimals 19 的帳戶呼叫 `tokenAccounts`，期望筆數是 0。回傳空清單代表跳過該帳戶而且這一輪沒有失敗（失敗會是 null）。`parseAccount` 在 0…18 之外回 null |
| L3 | **關閉** | 初審：`HomeAssetsScreen` 自己開 `URL.openStream` 下載圖示。第 2 輪：下載在 `TokenIconLoader`。畫面只在 https URL 時呼叫它，不解析 RPC JSON，也不另存一份列清單 |

## 與 how.md

| Track | 結果 |
|-------|------|
| A 型別、非 Helius RPC、round id、`lock` → `clear()` | 有。native SOL 在 `ready` 時排第一，lamports 為 0 也在。endpoint 為 null 不進入 `loading`。M1 的 endpoint 切換已接到 settings 寫入與返回 Home |
| B Jupiter、盾、USD | 有 Mainnet 才打 Jupiter；失敗留下該輪餘額列，不改 `SOL`／`wSOL`。M2、M3、M4、L1 本輪與已定案一致 |
| C Helius | host 與 query 的開關有單測：`api-key` 優先於 `apiKey`，非 Helius host 拒絕，Devnet 不走 wallet API，不用 `heliusConfigured`。路徑上只有 `getBalance`、一次 wrapped SOL 的 `getTokenAccountsByOwner`、以及 wallet API。M2 的數字 token 規則已補上 |
| D 引導、pill、Devnet 標、冷卻 | 文案三語與 `https://www.helius.dev/` 對得上 how.md。Skip 只用記憶體，空狀態用 `home.noAssets`，按鈕用引導的 Set RPC 文案。pill 不是按鈕、沒有複製、兩邊 tab 共用。Devnet 標在頂列下方。H1 的失敗態 refresh 已有。M6 是「帶著待批解鎖再拒絕」不會自動開輪 |

新檔都在 `com.airwave.wallet.holdings` 或 `HomeAssetsScreen.kt`。含空白的行數：新檔最長是 `HomeAssetsScreen.kt` 162 行，沒有超過 400。`WalletModel.kt` 433 行、`ShellScreens.kt` 829 行；INDEX 寫明本版不拆這兩個檔。本版新增與改到的 `.kt` 都有英文職責註解。

## 驗收對照

| 驗收句 | 結果 | 證據 |
|--------|------|------|
| `assembleDebug`、`testDebugUnitTest`、`lintDebug` | 通過 | 見下方測試結果。機器上沒有 `gradlew.bat`，用 wrapper jar 跑同一組任務 |
| 手測 1：Devnet 解鎖看到 Solana 與非零 token；refresh；殺掉行程再解鎖，沒有持倉檔 | 手驗未走 | 沒有待批時，解鎖後 `syncHoldings(false)`，endpoint 非 null 會先進入沒有舊列的 `loading`。持倉不寫 vault／settings。M6 是例外：帶著待批解鎖再拒絕，不會自動開輪。沒有在裝置上解鎖 |
| 手測 2：鎖定後下一次解鎖，新一輪完成前不顯示上一份清單 | 手驗未走 | `lock` → `holdings.clear()` 作廢 id 並清空 snapshot。沒有待批的下一次解鎖會先設 `loading`。沒有在裝置上走 |
| 手測 3：Mainnet 沒有 active RPC 顯示引導，不打公開 mainnet；Skip 空狀態；Set RPC 打開既有 RPC 畫面 | 手驗未走 | `endpointForCluster` 在這個情況回 null。`absentEndpoint` 設 `guide` 或 `skipped`，不抓取。Skip 不抓取。Set RPC 是 `push(HomeDestination.Rpc)`。單測覆蓋「不會選到 `PUBLIC_MAINNET`」。logcat 沒有在裝置上看 |
| 手測 4：設好允許的 Mainnet RPC 後返回，看得到 Solana；Helius URL 帶 `api-key` 時 SPL 仍在，原生 SOL 仍是自己的一列 | 手驗未走；程式上會開輪 | 選取、新增、移除、取代 endpoint 都會 sync。從引導或 Settings hub 返回、堆疊清空時也會 sync。endpoint 已非 null 才抓取。Helius 路徑仍把原生 SOL 留在 `getBalance`，wrapped SOL 另列 |
| 手測 5：已驗證 token 的盾在圖示左邊；Activity 仍是沒有活動；MWA authorize 仍打開既有批核 | 手驗未走 | 盾畫在 `TokenIcon` 之前。未驗證但分數大於 0 的徽章見 M4，本輪已分開畫。Activity 仍是 `home.noActivity`。`onAuthorizeRequest` 仍 `offer` 一個 in-memory `Pending` |
| 手測 6：成功後 3 秒內的 refresh 不開第二輪；失敗後 refresh 立刻開新一輪 | 程式符合；手驗未走 | 成功後的 3 秒在 `HoldingsModel`（`cooldownUntil`，只擋 `Ready`）。進行中的第二次 refresh 直接 return。失敗態有按鈕，見 H1。沒有在裝置上按 |
| 手測 7：Devnet sign-and-send 用請求的 chain；Mainnet 沒有 active RPC 在簽名前失敗 | 手驗未走 | `rpcUrlForSubmit` 依請求 chain 選 endpoint。`rpcUrl == null` 時 `completeWithDecline()`，在 `offer` 與 `signAndSend` 之前返回 |
| `formatBaseUnits(1, 9)` 為 `0.000000001`；去掉小數尾端 0；整數沒有小數點；`halfTowardZero(1)` 為 0；不用 `double` | 通過 | `HoldingRulesTest.formatOneLamportAndHalf`。尾端 0 由 `10^9`、decimals 9 得到 `1` 蓋到。實作只用 `BigInteger` |
| 同一 mint、同一 program 用 `BigInteger` 相加；畸形帳戶跳過且不失敗整輪 | 通過 | `sumsSameMintAndSkipsMalformed` |
| decimals 超出 0…18 同樣跳過 | 通過 | `dropsZeroDecimalsAndKeepsWrappedSeparate` 的 decimals 19。見 L2 |
| Mainnet 空的 active RPC 不選 `PUBLIC_MAINNET` | 通過 | `mainnetWithoutRpcIsNotPublic`，與 `RpcResolver.endpointForCluster` 一致 |
| Helius：`api-key` 優先；非 Helius host 拒絕；Devnet 不走 wallet API；不用 `heliusConfigured` | 通過 | `heliusKeyAndCluster`。URL 用 how.md 的 placeholder，不是真實金鑰 |
| decimals 0 與金額 0 的 SPL 丟掉；lamports 為 0 仍留原生 SOL；wrapped SOL 不併進原生 SOL | 通過 | `dropsZeroDecimalsAndKeepsWrappedSeparate`。`token-2022` 字串另有 `heliusProgramString` |

## 測試結果

工作目錄 `android/`。這台機器沒有 `gradlew.bat`。`java` 不在 PATH 上，所以把 `JAVA_HOME` 設為 `C:\Program Files\Android\Android Studio\jbr`，並把該目錄的 `bin` 放進 PATH，再執行 wrapper。Gradle 9.6.0。

指定指令：

`java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`

`BUILD SUCCESSFUL in 11s`。`:app:testDebugUnitTest` 與 `:app:assembleDebug` 是 UP-TO-DATE，`:app:lintDebug` 有執行，exit 0。

同一組任務再加上 `--rerun-tasks`，讓三個任務都真的重跑：`BUILD SUCCESSFUL in 25s`，52 個任務都有執行。

| 任務 | 結果 |
|------|------|
| `:app:assembleDebug` | 通過 |
| `:app:testDebugUnitTest` | 通過。15 tests，0 failures，0 errors，0 skipped。`HoldingRulesTest` 6、`RpcResolverTest` 4、`SettingsModelsTest` 3、`VaultCipherTest` 2 |
| `:app:lintDebug` | 通過（exit 0）。HTML 報告標題是 17 warnings。warnings 沒有讓任務失敗 |

編譯有既有 deprecation warning（`LocalAssociation`、`SolanaJsonRpc`、`ShellScreens` 的 `MenuBook`），不是本次失敗。

手驗：沒有在裝置上走完。`adb` 不在 PATH 上，改用 `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`。`adb devices` 沒有已連線裝置。`emulator -list-avds` 看得到 `Medium_Phone_API_37.0`，沒有開機。INDEX 的 7 步手測都沒有在 emulator 上操作。靜態讀碼不算手驗。

## 修復追蹤

| ID | 級 | 狀態 | 說明 |
|----|----|------|------|
| H1 | HIGH | 關閉 | 第 2 輪：失敗態有重新整理，按下立刻開新一輪 |
| M1 | MEDIUM | 關閉 | 第 2 輪：endpoint 寫入與返回 Home 都會 sync |
| M2 | MEDIUM | 關閉 | 第 2 輪：JSON 數字 token 整段符合 how.md，字串形式會讀 |
| M3 | MEDIUM | 關閉 | 第 2 輪：name 或 symbol 缺一個時，兩個都用 `BurnerKeys.short` |
| M4 | MEDIUM | 關閉 | 第 2 輪：其他 mint 的 organicScore 大於 0 會畫徽章，不必先 verified |
| M5 | MEDIUM | 關閉 | 第 2 輪：`AppStrings.kt` 已有英文職責註解 |
| M6 | MEDIUM | 仍開 | 第 2 輪新增：帶著待批解鎖後按拒絕，不會自動開餘額輪 |
| L1 | LOW | 關閉 | 第 2 輪：排序用已經 half-up 到 2 位的金額 |
| L2 | LOW | 關閉 | 第 2 輪：已有 decimals 19 的單測 |
| L3 | LOW | 關閉 | 第 2 輪：圖示下載在 `TokenIconLoader` |

## 歷審摘要

| 輪次 | 日期 | 結論 |
|------|------|------|
| 初審 | 2026-10-25 | 不可出貨。H1 仍開。三個 Gradle 任務通過。手驗未在裝置上走完 |
| 第 2 輪複審 | 2026-10-25 | 未關閉 HIGH 沒有。H1、M1–M5、L1–L3 關閉。M6 仍開。三個 Gradle 任務重跑通過。手驗未在裝置上走完 |
