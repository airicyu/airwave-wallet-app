# Design review — 0.3.0 Airwave Wallet App

- 日期：2026-10-25（Asia/Hong_Kong）
- 輪次：**第 2 輪複審**
- 角色：設計審查（不改 INDEX／HOW／reasoning／HANDOFF／程式；不以本檔當已定案）
- 對照基準：[`../INDEX.md`](../INDEX.md) 已定案＋驗收；[`reasoning.md`](./reasoning.md)、[`how.md`](./how.md)、[`../HANDOFF.md`](../HANDOFF.md)
- 架構禁區：[`../../GUIDELINES.md`](../../GUIDELINES.md)
- 現行程式抽樣：`WalletModel.kt`（404 行，含空白）、`settings/RpcResolver.kt`、`settings/SettingsModels.kt`、`solana/SolanaJsonRpc.kt`、`ui/ShellScreens.kt`（809 行，含空白）。本版持倉程式尚無
- **總評：** 無未關閉 HIGH。審查門檻未通過：M5、M10、M11、M12、M13 仍開，須寫進 INDEX／HOW，或在本檔標成非阻擋。提案可行，不是不可行。

本輪只核對現在的 INDEX、reasoning、how、HANDOFF。初審要寫死的句子，有寫進這些檔才算關閉。不把 backlog、未排程構想、或 Chrome extension 的行為當成已定案。`wallet-overall-v1.html` 只在 INDEX 點名的 `home-assets` 與 `rpc-guide` 電話框內當視覺契約；旁註不是出貨文案。INDEX 已用完整句子排除的電話框控制（無底 tab 的引導頁、pill 的 button、複製圖示、kebab）維持排除。

## Findings（本輪）

關閉＝已寫進 INDEX／HOW／HANDOFF；仍開＝契約仍分叉；非阻擋＝記錄即可。

### HIGH

#### H1 — Helius 路徑與無條件的 token program 掃描互相矛盾 — **關閉**

**本輪核對：** 已採初審的第 2 案，三處的失敗集合相同。

| 位置 | 內容 |
|------|------|
| INDEX「Fetch」 | 非 Helius：`getBalance` 加上兩個 token program；任一 token program 失敗則整輪失敗。Helius：不掃兩個 program。原生 SOL 只來自 `getBalance`。Wrapped SOL 只來自一筆對 mint `So111…` 的 `getTokenAccountsByOwner`。其餘 SPL 只來自 wallet API。這三個呼叫任一失敗則整輪失敗，不留下只有原生 SOL 的半套。空的 wrapped-SOL 結果不是失敗 |
| `how.md` Track C、RPC wire | 同一組 RPC：wrapped-SOL 的第二個參數是 `mint`，不是 `programId`。三者之一失敗則整輪失敗 |
| `reasoning.md` | Helius 路徑不再同時掃兩個 token program；原生與 wrapped 各走自己的 RPC，失敗集合只有一套 |

手測 4 對得上這一套：SPL 來自 wallet API，原生 SOL 仍是自己的一列。

#### H2 — 在途餘額輪次沒有作廢規則 — **關閉**

**本輪核對：** INDEX「Session」已寫單調 round id、該輪的 cluster 與 endpoint、`lock`／換 cluster／再開始一輪時作廢舊 id、只有目前 id 可以寫入、`lock` 清空 snapshot、作廢回應不得寫回、解鎖後先是沒有舊列的 loading。`HoldingsModel` 由 `WalletModel` 持有，`HomeAssetsScreen` 只讀 snapshot。`how.md` Track A 寫 `lock` → `clear()` 作廢 id 並清空。`reasoning.md` 寫 `WalletModel` 在上鎖後仍在，所以遲到的 HTTP 不得把舊列畫回去。

### MEDIUM

| ID | 本輪狀態 | 依據 |
|----|----------|------|
| M1 | **關閉** | `how.md`「Numbers in JSON」「RPC wire」「Helius wire」「Jupiter wire」寫了 commitment、`jsonParsed`、欄位路徑、十進位文字、`BigInteger`／`BigDecimal`、禁止 `optLong`／`optDouble`、Helius 的 URL 與分頁、以及 Jupiter 每批最多 100 個 mint。開關是 INDEX 的 host／query，不用 `heliusConfigured`。執行期先完成餘額輪（含 Helius）再跑 Jupiter。`tokenProgram` 的對照值見新的 M10 |
| M2 | **關閉** | INDEX「Mainnet without RPC」與 `how.md` Track D：引導是 `HomeAssetsScreen` 的 phase `guide`，不是新的 `HomeDestination`；留底 tab 與 0.2.0 選單；無返回。en／zh-Hant 原文與 `https://www.helius.dev/` 已寫。正文不寫免費、不寫 API key。zh-Hans 譯英文 |
| M3 | **關閉** | INDEX「Chrome」採共用頂列：pill 取代 `home.wallet` 與短地址，資產與活動同一顆。外觀是簽名標記與 `BurnerKeys.short`。不是 button、無複製圖示、不導頁、不複製。`how.md` Track D 第 5 步與 File map 一致：pill 在殼上，不在 `HomeAssetsScreen` |
| M4 | **關閉** | INDEX「Rows」「Jupiter」「Trust shield」「USD」：缺 name 或 symbol 時兩者都用該 mint 的 `BurnerKeys.short`；icon 只接受 https，不畫字母圓。原生 SOL 與 Wrapped SOL 的名稱與 `wSOL` 維持硬編碼，盾與 USD 改採 wrapped mint 那筆 hit 的 `isVerified` 與 `usdPrice`。電話框上這兩列的圖示與分數徽章仍見 M12 |
| M6 | **關閉** | INDEX「Unit tests」與 `how.md`「Types」：`formatBaseUnits(1, 9)` 為 `0.000000001`；去掉小數尾端的 0；整數不帶小數點；`halfTowardZero(1)` 為 0。同 mint 同 program 以 `BigInteger` 相加；畸形帳戶與 decimals 超出 0…18 都略過且不讓整輪失敗 |
| M7 | **關閉** | INDEX「Mainnet without RPC」：Skip 旗標只在 `HoldingsModel` 記憶體，不寫 settings 或 vault；`lock` 或行程死亡後，Mainnet 仍無 endpoint 就再顯示引導；skipped 沒有 native SOL 列。從 RPC 畫面回到 Home，或 endpoint 從 null 變成有 URL，開始新輪；endpoint 仍是 null 不抓取。`how.md` Track A／D 同此 |
| M8 | **關閉** | INDEX 手測 6、7 與「Refresh」。成功後 3 秒內不再開一輪；失敗可立刻再試。Devnet sign-and-send 仍用該請求的 chain；Mainnet 沒有 active RPC 仍在簽名前失敗。與 0.2.0 INDEX「MWA submit RPC」一致。Authorize 仍是手測 5 |
| M9 | **關閉** | INDEX「File shape」：本版不拆 `WalletModel.kt` 與 `ShellScreens.kt`。`WalletModel` 只持有 `HoldingsModel` 並在 `lock` 呼叫 `clear()`。兩檔都在本版實作審查接受之後、0.4.0 reconcile 之前的 structure pass 再拆。改到的既有 `.kt` 補上最多 100 字的英文職責註解。`how.md` File map 寫明 `ShellScreens.kt` 的註解是既有殼，本版只加資產分頁分支與共用 pill |

#### M5 — USD 的計算、零價格與排序鍵 — **仍開**

INDEX「USD」已寫上初審要的計算、`$0.00`、`—`、用 `BigDecimal` 排序、以及原生 SOL 與 Wrapped SOL 固定在前兩位。這一段關閉條件裡還沒對上的是 `how.md` Track B 第 2 步：Jupiter HTTP 失敗時「sorts the non-native rows by symbol」。

`non-native` 把 Wrapped SOL 算進依 symbol 排序的那一組，第二位就不再固定。INDEX 寫的是失敗或 Jupiter 尚未回來時，只有其餘列依 symbol，前兩位不動。兩邊可以做出兩種列序。

請把 Track B 第 2 步改成與 INDEX「USD」同一句：Jupiter 失敗或尚未回來時，前兩位仍是原生 SOL、Wrapped SOL；其餘列只依 symbol。成功回到後的 USD 排序仍照 INDEX，不解析 `usdLabel`。

#### M10 — Helius `tokenProgram` 對到 program id，公開回應是列舉字串 — **仍開**

`how.md`「Helius wire」寫：`tokenProgram` 等於 `TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb` 才設 `token-2022`；其他值與缺欄都設 `spl-token`。

本版點名的 `GET https://api.helius.xyz/v1/wallet/{address}/balances`，公開 schema 裡這個欄位的 enum 是 `spl-token` 與 `token-2022`，不是 program id。依 HOW 原句實作，Token-2022 列仍會出現在清單，但列上的 `tokenProgram` 全部是 `spl-token`。非 Helius 路徑也還沒寫掃到哪一個 programId 就填哪一個列舉值。

請改 `how.md`「Helius wire」，並在 INDEX「Helius」點到這一句：JSON 字串 `token-2022` 設 `token-2022`；JSON 字串 `spl-token` 或然缺欄設 `spl-token`。不要拿 program id 比這個欄位。非 Helius 路徑：`Tokenkeg…` 的帳戶設 `spl-token`，`Tokenz…` 的帳戶設 `token-2022`。

#### M11 — `native-sol` 永遠存在，和引導／Skip／`getBalance` 失敗並陳 — **仍開**

INDEX Product 與「Rows」寫 native SOL 永遠有一列、永遠排第一。同一張 Decided 表的「Mainnet without RPC」寫 Skip 的空狀態沒有 native SOL 列；`guide` 也不是餘額清單。手測 3 的空狀態與「永遠有一列」可以各做一套。

「Fetch」只在 Helius 路徑寫明 `getBalance` 失敗則整輪失敗。非 Helius 路徑只寫兩個 token program 失敗則整輪失敗。`getBalance` 失敗時可以交出沒有原生 SOL 的 SPL 半套，並同時違反「永遠有 native SOL」。

請改 INDEX「Rows」與「Fetch」：成功輪（phase `ready`）永遠有 `native-sol`，包含 0 lamports，名稱 `Solana`、符號 `SOL`、排第一。`guide` 與 `skipped` 不放這列。兩條路徑都是 `getBalance` 失敗則整輪失敗，不交出沒有原生 SOL 的半套。

#### M12 — 原生 SOL 的圖示與分數徽章，電話框和 Jupiter 欄位各說各的 — **仍開**

`home-assets` 電話框的 Solana 列有 token 圖、有盾、盾上沒有分數。USDC 列才有分數徽章。INDEX「Jupiter」寫原生 SOL 與 Wrapped SOL 向 wrapped mint 取 `usdPrice`、`isVerified`、`organicScore`，沒有把 `icon` 列進去。「Trust shield」又寫 `organicScore` 大於 0 就在該列的盾上畫徽章，並顯示原文字。

依 INDEX，這兩列沒有 icon 來源；SOL 的 organic score 大於 0 時盾上會有數字。依電話框，Solana 列有圖、有盾、無數字。M4 已把盾和 USD 寫死，這兩項還沒寫死。

請在 INDEX「Jupiter」與「Trust shield」擇一寫死：

1. 這兩列的 icon 用 wrapped mint 那筆 hit 的 https `icon`，沒有則不畫圖、不畫字母圓。盾只看該 hit 的 `isVerified`。這兩列不畫 organicScore 徽章。其餘 mint 仍照現有的盾與徽章規則。
2. 這兩列不取 icon。`organicScore` 大於 0 時盾上照畫徽章。INDEX 同時寫明：這是 `home-assets` 電話框 Solana 列的明示例外。

#### M13 — 換 cluster 與餘額輪失敗時，舊列和失敗句沒寫死 — **仍開**

INDEX「Session」把「沒有舊列的 loading」寫在解鎖之後。換 cluster 只寫作廢舊 id 並開新輪，沒寫畫面上的上一輪列是否立刻清掉。Devnet 的列可以一直留到 Mainnet 那輪寫入為止。

餘額輪失敗：Helius 路徑寫了不留下「只有原生 SOL」的半套，沒寫是否仍顯示再上一輪的 ready 列，也沒有失敗句。`how.md` Track A 只說畫面會畫「the failure sentence」。INDEX「Copy」要求新字串的英文在 `how.md`，這句英文不在。Jupiter 失敗要留下該輪餘額列，這句已在 INDEX「Jupiter」，維持不變。

請寫進 INDEX「Session」與 `how.md` Track A、Copy，擇一：

1. 餘額輪一開始（解鎖之後、換 cluster、重新整理、endpoint 從 null 變成有 URL）就把 phase 設為 `loading` 並清空列。這輪餘額失敗則 phase 為 `failed`、列為空。Jupiter 失敗不走這條，仍留下該輪餘額列。
2. 解鎖與換 cluster 清空列並進入 `loading`。同一 cluster 上的餘額輪失敗時，保留上一份 `ready` 列。Jupiter 失敗仍留下該輪餘額列。

兩案都加上失敗句，放在 phase `failed`（第 2 案則放在保留的列之下）：英文 `Couldn't load balances.`；zh-Hant `讀不到餘額。`；zh-Hans 另譯。一句，不附加原因碼。

### LOW

| ID | 本輪狀態 | 依據 |
|----|----------|------|
| L1 | **關閉** | INDEX「Chrome」與 `how.md` Track D 第 6 步：Devnet 時既有 `network.devnet` 直接放在共用頂列下方，資產與活動都顯示 |
| L2 | **關閉** | INDEX「Rows」「Chrome」：列不導頁、不可點。明細與送出仍在 0.5.0 |
| L3 | **關閉** | INDEX「Rows」與 `how.md` RPC wire：decimals 超出 0…18 略過該帳戶，不讓整輪失敗。單測已列 |
| L4 | **關閉** | 函式名 `halfTowardZero`，寫在 `how.md`「Types」與 INDEX 單測 |
| L5 | 仍開 | USD 是 `BigDecimal` 乘 raw 再除以 `10^decimals`，再 half-up 到小數兩位。除不盡時若沒有指定 scale 與捨入，Kotlin／Java 的除法會拋錯，整次 Jupiter 會被當成失敗。請在 `how.md`「Jupiter wire」加一句：中間除法自帶 scale 與捨入，最終仍 half-up 到兩位；不得因除法拋錯改走 Jupiter 失敗 |
| L6 | 仍開 | Skip 的空狀態已規定沒有 native SOL 列，並有一個打開 Settings → RPC 的控制。句子和按鈕文字沒寫。請在 `how.md` Track D 寫：空狀態沿用 `home.noAssets`（`No assets yet.`）；控制沿用引導的 `Set RPC`／`設定 RPC`／簡體譯文 |
| L7 | 仍開 | 手測 1 寫「Pull or tap refresh」。電話框有重新整理按鈕。請在 `how.md` Track D 寫明本版是按鈕、下拉、或兩者都有 |

## 驗收對照

| 驗收句 | 設計層是否可測 | 缺口 |
|--------|----------------|------|
| `gradlew.bat :app:assembleDebug` | 可測 | 無 |
| `gradlew.bat :app:testDebugUnitTest` | 可測 | M6 已關。單測清單含格式、half、相加、畸形略過、decimals 範圍、`PUBLIC_MAINNET`、Helius query |
| `gradlew.bat :app:lintDebug` | 可測 | 無 |
| 手測 1：Devnet 解鎖後看到 Solana 與非零代幣；重新整理；殺掉行程再解鎖會再抓，磁碟沒有持倉檔 | 可測 | 非 SOL 的 name／symbol 已由 M4 寫死。重新整理是拉還是點，見 L7 |
| 手測 2：上鎖後再解鎖，新輪完成前不顯示舊列 | 可測 | H2 已關 |
| 手測 3：Mainnet 無 active RPC 時顯示引導；logcat 沒有 `api.mainnet-beta.solana.com` 的持倉呼叫；Skip 留下空狀態；Set RPC 打開既有 RPC 畫面 | 可測 | 引導殼層與三語已由 M2 寫死。Skip 沒有 native SOL 列與「永遠有 native SOL」見 M11。空狀態文案見 L6 |
| 手測 4：設好允許的 Mainnet RPC 回到資產看到 Solana 列；`api.helius.xyz` 且 query 含 `api-key` 時 SPL 仍載入，原生 SOL 自成一列 | 可測 | H1、M1、M7 已關。M10 不改變「列是否出現」，但會把 Token-2022 標成 `spl-token`。手測看不出標錯 |
| 手測 5：已驗證代幣的盾在圖示左側；活動仍無活動；MWA authorize 仍打開既有批准 | 部分可測 | 盾的位置已定案。原生 SOL 的圖與分數徽章見 M12。活動文案維持 `No activity yet.` |
| 手測 6：成功後 3 秒內重新整理不開第二輪；失敗可立刻再試 | 可測 | M8 已關。失敗時列還在不在，見 M13 |
| 手測 7：Devnet sign-and-send 仍用請求的 chain；Mainnet 無 active RPC 仍在簽名前失敗 | 可測 | 與 0.2.0「MWA submit RPC」相同，M8 已關 |
| 單測：`formatBaseUnits`、Mainnet 空 active 不選 `PUBLIC_MAINNET`、Helius query、丟棄 decimals 0 與零餘額、原生 SOL 在 0 lamports 仍在、Wrapped SOL 不併入原生 SOL | 可測 | M6 已關 |

## 與現碼抽樣

現碼沒有本版持倉，這不是設計 HIGH。抽樣裡沒有和提案互斥的現行行為。行數只說明豁免仍然對得上現檔，不是本輪的新缺陷。

| 錨點 | 與提案的關係 |
|------|----------------|
| `RpcResolver.endpointForCluster` | Devnet：允許的 active 自訂 URL，否則 `PUBLIC_DEVNET`。Mainnet：只有允許的 active 自訂 URL，否則 `null`。`PUBLIC_MAINNET` 只是常數。提案沿用此函式 |
| `SettingsModels.AirwaveSettings` | 持久欄位已有 `heliusApiUrl`、`jupiterApiKey`。提案不新增 store。`heliusConfigured` 仍比 Helius 路徑寬；契約已寫不用它當開關 |
| `SolanaJsonRpc` | 沒有 `getBalance` 或 `getTokenAccountsByOwner`。`estimateFeeLamports` 仍用 `optLong`。持倉新檔依 HOW 從十進位文字建 `BigInteger`，不沿用 `optLong` |
| `WalletModel.lock` | 已清 session、失敗化記憶體中的 `pending`，route 回到 Unlock。`WalletModel.kt` 404 行、檔首是 `package`、無英文職責註解。INDEX 已豁免本版不拆，並要求改到時補註解、在 `lock` 呼叫 `clear()` |
| `ShellScreens.HomeTabs` | 資產空文案 `No assets yet.`，活動 `No activity yet.`。頂列是大標題、hamburger、`Wallet` 與短地址。提案只換掉 `Wallet` 與短地址，選單維持 0.2.0，因此不採用 overall demo 的 kebab。`ShellScreens.kt` 809 行、無職責註解，豁免已寫在 INDEX「File shape」 |
| 待批與儲存 | `pending` 只在 process 記憶體。持久狀態是 `airwave.vault.v1` 與 `airwave.settings.v1`。提案不把持倉或 Skip 寫入任一 store |

## 修復追蹤表

| ID | 級 | 狀態 | 關閉位置（INDEX 條／HOW 節） |
|----|----|------|------------------------------|
| H1 | HIGH | **關閉** | INDEX「Fetch」；`how.md` Track C、RPC wire；`reasoning.md` Approach |
| H2 | HIGH | **關閉** | INDEX「Session」；`how.md` Track A；`reasoning.md` Approach |
| M1 | MEDIUM | **關閉** | `how.md` Numbers in JSON、RPC wire、Helius wire、Jupiter wire；INDEX「Helius」「Jupiter」 |
| M2 | MEDIUM | **關閉** | INDEX「Mainnet without RPC」「Copy」；`how.md` Track D |
| M3 | MEDIUM | **關閉** | INDEX「Chrome」；`how.md` Track D 第 5 步、File map |
| M4 | MEDIUM | **關閉** | INDEX「Rows」「Jupiter」「Trust shield」「USD」 |
| M5 | MEDIUM | 仍開 | INDEX「USD」已寫；`how.md` Track B 第 2 步仍分叉 |
| M6 | MEDIUM | **關閉** | INDEX「Unit tests」；`how.md` Types |
| M7 | MEDIUM | **關閉** | INDEX「Mainnet without RPC」「Refresh」；`how.md` Track A、Track D |
| M8 | MEDIUM | **關閉** | INDEX 手測 6、7；「Refresh」「Chrome」 |
| M9 | MEDIUM | **關閉** | INDEX「File shape」；`how.md` File map |
| M10 | MEDIUM | 仍開 | — |
| M11 | MEDIUM | 仍開 | — |
| M12 | MEDIUM | 仍開 | — |
| M13 | MEDIUM | 仍開 | — |
| L1 | LOW | **關閉** | INDEX「Chrome」；`how.md` Track D 第 6 步 |
| L2 | LOW | **關閉** | INDEX「Rows」「Chrome」 |
| L3 | LOW | **關閉** | INDEX「Rows」；`how.md` RPC wire；單測 |
| L4 | LOW | **關閉** | `how.md` Types 的 `halfTowardZero` |
| L5 | LOW | 仍開 | — |
| L6 | LOW | 仍開 | — |
| L7 | LOW | 仍開 | — |

## 歷審摘要

| 輪次 | 日期 | 結論 |
|------|------|------|
| 初審 | 2026-10-25 | 有未關閉 HIGH（H1、H2）。審查門檻未通過。提案可行，不是整份不可行 |
| 第 2 輪複審 | 2026-10-25 | H1、H2 與 M1–M4、M6–M9、L1–L4 已寫進 INDEX／HOW。M5 仍分叉。新增 M10–M13、L5–L7。無未關閉 HIGH。審查門檻未通過。提案可行，不是不可行 |

---

## 初審全文（保留）

- 日期：2026-10-25（Asia/Hong_Kong）
- 輪次：**初審**
- 角色：設計審查（不改 INDEX／HOW／reasoning／HANDOFF／程式；不以本檔當已定案）
- 對照基準：[`../INDEX.md`](../INDEX.md) 已定案＋驗收；[`reasoning.md`](./reasoning.md)、[`how.md`](./how.md)。`HANDOFF.md` 不存在，本輪未讀
- 架構禁區：[`../../GUIDELINES.md`](../../GUIDELINES.md)
- 現行程式抽樣：`WalletModel.kt`、`settings/RpcResolver.kt`、`settings/SettingsModels.kt`、`solana/SolanaJsonRpc.kt`、`ui/ShellScreens.kt`（本版持倉程式尚無）
- **總評：** 有未關閉 HIGH。審查門檻未通過。提案可行：記憶體持倉、沿用 `endpointForCluster`、Mainnet 才走 Helius、Jupiter 失敗仍留餘額，這四個方向寫在 reasoning，補上下面的句子後可以實作。

本輪不把 backlog、未排程構想、或 Chrome extension 的行為當成已定案。`wallet-overall-v1.html` 只在 INDEX 點名的 `home-assets` 與 `rpc-guide` 電話框內當視覺契約；旁註不是出貨文案。

### Findings（初審）

關閉＝已寫進 INDEX／HOW／HANDOFF；仍開＝契約仍分叉；非阻擋＝記錄即可。

#### H1 — Helius 路徑與無條件的 token program 掃描互相矛盾 — **仍開**（初審時）

INDEX「Fetch」寫每一輪都做 `getBalance`，再對 Token 與 Token-2022 呼叫 `getTokenAccountsByOwner`；任一個 token program 失敗就整輪失敗，不合併半套。INDEX「Helius」與 HOW Track C 又寫：條件成立時 SPL 列來自 `GET /v1/wallet/{address}/balances`，原生 SOL 與 Wrapped SOL 來自同一輪的 RPC；wallet 呼叫或「該次 RPC」失敗才整輪失敗。

兩句可以實作成兩套不同的呼叫與失敗條件。一套是 Helius 路徑仍掃兩個 token program，掃失敗即使 wallet API 已成功也整輪失敗。另一套是 Helius 路徑不再呼叫 `getTokenAccountsByOwner`，SPL 只來自 wallet API。手測 4 只要求 SPL 列出現、原生 SOL 自成一列，分不出是哪一套。這是已定案互相矛盾，主網有 Helius URL 時的失敗行為無法實作。

建議在 INDEX「Fetch」與「Helius」寫死同一段，二擇一：

1. 每一輪都呼叫 `getBalance` 與兩個 token program 的 `getTokenAccountsByOwner`。任一個 token program 失敗則整輪失敗。Helius 路徑另外呼叫 wallet API，該呼叫失敗也整輪失敗。畫面上的原生 SOL 與 Wrapped SOL 用 RPC 結果；其餘 SPL 列用 wallet API，並丟掉其中的 wrapped SOL mint。非 Helius 路徑的列全部來自上述 RPC。
2. 非 Helius 路徑維持兩個 `getTokenAccountsByOwner`，任一失敗則整輪失敗。Helius 路徑不呼叫 `getTokenAccountsByOwner`。原生 SOL 只來自 `getBalance`。Wrapped SOL 只來自此處寫明的單一 RPC 方法與篩選。其餘 SPL 只來自 wallet API。wallet 呼叫、`getBalance`、或 Wrapped SOL 的 RPC 失敗則整輪失敗，不留下只有原生 SOL 的半套。

HOW Track C 改成與所選句子相同的失敗集合。

#### H2 — 在途餘額輪次沒有作廢規則 — **仍開**（初審時）

手測 2 要求上鎖後再次解鎖，新的一輪完成前不顯示上一輪的列。已定案另寫：換 cluster 會丟掉上一份 snapshot 並開新輪。HOW 只寫 `WalletModel.lock` 呼叫 `holdings.clear()`，以及 Home 出現、cluster 變更、或使用者重新整理時開新輪。

現碼的 `WalletModel` 是同一個 `AndroidViewModel`：`lock()` 清 session 並把 route 改回 Unlock，物件還在；閒置上鎖也是呼叫這同一個 `lock()`。遲到的 HTTP 回應若在 `clear()` 之後寫回 snapshot，解鎖當下就會看到舊列，手測 2 不成立。換 cluster 或連點重新整理時，舊回應也可以蓋掉新輪。這和「UI 跟著記憶體 snapshot」以及手測 2 不能同時成立。

建議寫進 INDEX「Session」與 HOW Track A：

HoldingsModel 的每一輪有單調 round id，並記下該輪的 cluster 與 endpoint。`lock`、cluster 變更、以及再開始一輪時，舊 id 作廢。只有 id 仍是目前這一輪，才可以寫入 snapshot。`lock` 清空 snapshot。作廢後的回應不得寫回。解鎖後先進入沒有舊列的 loading，新的一輪完成前不顯示上一輪的列。`HoldingsModel` 由 `WalletModel` 持有；`HomeAssetsScreen` 只讀 snapshot，不自己保存一份列。

#### MEDIUM（初審時均仍開）

##### M1 — 餘額線格與 Helius 分頁未寫死 — **仍開**（初審時）

Helius 路徑失敗會整輪失敗，同 mint 且同 program 的 base units 會用 `BigInteger` 相加。線格未寫時，實作可以猜錯欄位而整輪失敗，或把同一頁重複加總。

建議寫進 HOW：

- `getTokenAccountsByOwner` 使用 `encoding=jsonParsed` 與一個寫明的 commitment。mint、amount、decimals 的欄位路徑寫明。amount 與 `getBalance` 的 lamports 都從十進位文字建成 `BigInteger`，不用 JSON number 的 `double` 或 `optLong`。
- Helius：從 `heliusApiUrl` 取出的 key 以寫明的 query 名附在 `GET https://api.helius.xyz/v1/wallet/{address}/balances`。儲存 URL 的 path 不當 API path。回應裡 mint、amount、decimals、token program 的欄位路徑寫明；沒有 program 時 `tokenProgram` 怎麼填也寫明。wallet API 是否包含 Token-2022 寫明。分頁 query 名寫明；某一頁筆數少於 `limit` 或為空就停止，最多 20 頁。
- 開關是 INDEX 的 host 與 query 規則。不用 `AirwaveSettings.heliusConfigured`（現碼只檢查 `isAllowedCustomUrl`，任何 https URL 都為真）。
- Jupiter：100 個 mint 如何放進 `GET https://api.jup.ag/tokens/v2/search`、以及 `name`、`symbol`、`icon`、`isVerified`、`organicScore`、價格的欄位路徑寫明。`organicScore` 用 `BigDecimal` 判斷是否大於 0，徽章顯示原文字。
- 執行期順序：先組好該輪餘額（含 Helius 路徑的 SPL），再跑 Jupiter。HOW 的 Track B 在 Track C 之前只是交付順序。

##### M2 — 引導是資產頁的 phase，文案尚未寫成三語 — **仍開**（初審時）

`rpc-guide` 電話框是獨立一頁：有標題、沒有帳戶 pill、沒有底 tab、沒有返回，離開靠「略過」與「設定 RPC」。HOW 把引導放在同時負責 pill 的 `HomeAssetsScreen`。`HoldingsSnapshot.phase` 只有 `idle`、`loading`、`ready`、`failed`。引導若被做成 `ready` 且列是空的，會和「成功的一輪永遠有 native SOL」混在一起。

INDEX 寫 English 是原文，但 HOW 的 Copy 只有 key，沒有英文句子。電話框裡已有繁中句子，design-principles 把電話框內容當出貨文案。

建議寫進 INDEX「Mainnet without RPC」與 HOW Track D、Copy：

引導是 `HomeAssetsScreen` 在 `endpointForCluster` 為 null 時的具名 phase，不 push 新的 `HomeDestination`。資產分頁仍留底 tab 與 0.2.0 選單。這一頁不畫返回；離開方式是略過與設定 RPC。本版不把 `rpc-guide` 做成沒有底 tab 的獨立 route。en 原文進 HOW。zh-Hant 採用電話框：標題「設定 Mainnet RPC」、正文「Mainnet 必須填你自己的 RPC，才能查餘額與送出。」、連結文字「向 Helius 申請 RPC endpoint」、按鈕「略過」與「設定 RPC」。連結 URL 是 `https://www.helius.dev/`。正文不寫免費，不寫 API key。zh-Hans 另譯。

##### M3 — 帳戶 pill 的範圍和電話框的複製圖示未對齊 — **仍開**（初審時）

現碼 Home 頂列是 `home.wallet`（Wallet）加 `BurnerKeys.short`。HOW 把 pill 放在 `HomeAssetsScreen`，`ShellScreens.kt` 只做資產分頁分支。依字面做，資產頁會同時留下舊地址文字與 pill，活動分頁仍是 0.2.0 頂列。

Track D 已寫 pill 不可點。`home-assets` 電話框的 pill 是 button，並有複製圖示。旁註把 pill 寫成帳戶切換器；帳戶切換在 0.4.0，本版 INDEX 已排除。

建議寫進 INDEX「Chrome」，二擇一，並加上圖示句：

1. pill 放在資產與活動共用的頂列，取代 `home.wallet` 與短地址文字。
2. pill 只在資產分頁；活動分頁維持 0.2.0 的 Wallet 與短地址。

兩案都加上：外觀是簽名標記與截斷地址，截斷沿用 `BurnerKeys.short`。本版不實作電話框的 button 與複製圖示，不可點、不導頁、不複製。

##### M4 — 沒有 Jupiter 時的列身分，以及原生 SOL 的盾和 USD — **仍開**（初審時）

Devnet 不跑 Jupiter。Mainnet 在 Jupiter 失敗時要留下餘額列。`HoldingRow.name` 與 `symbol` 是必填字串。已定案只寫了 native SOL（Solana／SOL）與 Wrapped SOL（Wrapped SOL／wSOL）。其他 mint 在沒有非空 name、symbol 時沒有字串。手測 1 的 Devnet 非零代幣會落到這個空洞。

`home-assets` 電話框的 Solana 列有盾，也有 USD。已定案是只有 Jupiter `isVerified` 為真才畫盾，且不改 native SOL 的名稱與 `wSOL` 符號。原生 SOL 的 `id` 是 `native-sol`，不是 wrapped mint。盾和 USD 會不會寫上 native 列，契約沒寫。沒有 https icon 時，design-principles 不允許用手畫字母圓充當代幣圖。

建議寫進 INDEX「Rows」與「Jupiter」：

Jupiter 沒有給非空 name 或 symbol 時，name 與 symbol 都用該 mint 的 `BurnerKeys.short`。icon 只接受 https；沒有 icon 就不畫圖，不畫字母圓。native SOL 與 Wrapped SOL 不畫盾。原生 SOL 的 `usdLabel` 擇一寫死：用 wrapped SOL mint 的 Jupiter 價格計算，或維持 `—`。

（若原生 SOL 的盾或 USD 要跟電話框一樣出現，改寫上面兩句，不要讓電話框與 INDEX 各說各的。）

##### M5 — USD 的計算、零價格與排序鍵 — **仍開**（初審時）

已定案：USD 只用 `BigDecimal`，由 Jupiter 價格字串與 base-unit `BigInteger` 組成；缺失或非有限顯示 `—`，不用 `$0` 當缺失。列型別只有 `usdLabel: String`。有限的 0、小數位數、`$` 前綴、以及「有價格的列和 `—` 的列如何排」都沒寫。若實作去解析 `usdLabel` 再排序，千分位或 `$` 會排錯。

建議寫進 INDEX「USD」：

USD = 價格字串的 `BigDecimal` × raw ÷ 10^decimals。顯示四捨五入到小數兩位，前綴 `$`。缺失、空字串或非有限值顯示 `—`。有限的 0 顯示 `$0.00`。排序在組成 snapshot 時用該 `BigDecimal`，不解析 `usdLabel`。native SOL 與 Wrapped SOL 固定在前兩位。其餘先排有有限 USD 的列（金額由大到小，再依 symbol），再排 `—` 的列（依 symbol）。Jupiter 尚未回來或整次失敗時，其餘列只依 symbol。

##### M6 — `formatBaseUnits` 的期望字串與列規則單測 — **仍開**（初審時）

驗收要求單測 1 lamport、9 decimals，以及整數除以 2，且不用 `double`。HOW 寫向零截斷，沒有寫 1 lamport 的輸出字串，也沒有寫小數尾端的 0 是否保留。列規則裡的「同 mint 同 program 相加」和「畸形帳戶略過」不在單測清單。

建議寫進 HOW「Types」與 INDEX「Unit tests」：

`formatBaseUnits` 去掉小數部分尾端的 0；整數不帶小數點。1 lamport、9 decimals 的結果是 `0.000000001`。`raw` 為 1 時整數除以 2 的結果是 0。單測另含：同 mint 且同 program 的兩筆 base units 以 `BigInteger` 相加；畸形帳戶略過，且不讓整輪失敗。

##### M7 — Skip 的壽命，以及從 RPC 回到資產時是否開新輪 — **仍開**（初審時）

Skip 不抓取，並留下可再開 Settings → RPC 的空狀態。契約沒寫這份狀態是否過 `lock`、是否寫進 `airwave.settings.v1`。寫進 settings 會變成第二種持久語意。手測 4 要求設好 Mainnet RPC 回到資產就看到 Solana 列。Track A 的觸發是 Home 出現、cluster 變更、或重新整理。現碼 RPC 畫面是 `homeStack` 上的一層，pop 之後底下的 Home composable 可能沒有重跑。

建議寫進 INDEX「Mainnet without RPC」與「Refresh」：

Skip 只存在 `HoldingsModel` 的記憶體，不寫入 settings 或 vault。`lock` 或行程死亡之後，Mainnet 仍沒有 endpoint 就再顯示引導。Skip 的空狀態沒有 native SOL 列。從 RPC 畫面回到 Home，以及 active endpoint 從 null 變成有 URL，都開始新的一輪；endpoint 仍是 null 時不抓取。

##### M8 — 手測沒有鎖住 0.2.0 的 sign-and-send 與重新整理冷卻 — **仍開**（初審時）

INDEX「Chrome」寫 MWA 維持 0.2.0。手測 5 只要求 authorize 仍打開既有批准。0.2.0 的提交規則是請求的 chain 決定 RPC，Mainnet 沒有 active RPC 時在簽名前失敗。本版會碰到同一個 `RpcResolver`。成功後 3 秒冷卻與失敗可立刻重試寫在已定案，編號手測沒有這一步。

建議在 INDEX 手測加兩步：成功的一輪之後 3 秒內再按重新整理不會發第二輪；失敗的一輪可以立刻再試。另加一步：Devnet 的一筆 sign-and-send 仍用該請求的 chain；Mainnet 沒有 active RPC 時仍在簽名前失敗。Authorize 維持手測 5。

##### M9 — `WalletModel.kt` 超過 400 行且沒有豁免 — **仍開**（初審時）

`code-design.md`：超過 400 行（含空白）的檔，本版要嘛拆開，要嘛 INDEX 點名該檔與原因，並指出之後哪一次 pass 再拆。INDEX 只豁免 `ShellScreens.kt`（現況 809 行，含空白），並把拆分放到本版之後的 structure pass。`WalletModel.kt` 現況 404 行，沒有豁免。HOW 仍要在 `lock` 加上 `holdings.clear()`。兩個檔目前都以 `package` 開頭，沒有英文職責註解；`code-design.md` 要求改到無註解的檔時補上註解。HOW 又寫 ShellScreens 的分支「只加呼叫、不加別的」。

建議寫進 INDEX「File shape」：

本版不拆 `WalletModel.kt`。該檔只增加對 `HoldingsModel` 的參考，以及 `lock` 裡的 `clear()`，不做餘額解析。拆分與 `ShellScreens.kt` 同一輪 structure pass（本版實作審查接受之後、0.4.0 reconcile 之前）。本版改到的既有 `.kt` 補上不超過 100 字的英文職責註解。`ShellScreens.kt` 的註解寫明它是既有殼，本版只加資產分頁分支。

#### LOW（初審時均仍開）

| ID | 本輪狀態 | 依據 |
|----|----------|------|
| L1 | 仍開（初審時） | Devnet 橫幅的文案已指向既有 Devnet 標籤（`network.devnet`）。位置未寫。電話框裡它只出現在 `menu-sheet` 變暗 Home，不在 `home-assets` 本體。請在 HOW Track D 用一句寫位置。 |
| L2 | 仍開（初審時） | 代幣列是否可點未寫。明細與送出在 0.5.0。請寫：本版列不可點、不開新 route。 |
| L3 | 仍開（初審時） | `BaseUnits.decimals` 是 0…18。鏈上小數位超出這個範圍時，請寫明視為畸形帳戶並略過，不讓整輪失敗。 |
| L4 | 仍開（初審時） | 整數除以 2 的輔助函式沒有名字。請在 `BaseUnits.kt` 給一個名字，或寫明單測直接斷言 `BigInteger.divide`。 |

### 驗收對照（初審）

| 驗收句 | 設計層是否可測 | 缺口 |
|--------|----------------|------|
| `gradlew.bat :app:assembleDebug` | 可測 | 無 |
| `gradlew.bat :app:testDebugUnitTest` | 可測 | M6：1 lamport 的期望字串未寫；相加與畸形略過不在清單 |
| `gradlew.bat :app:lintDebug` | 可測 | 無 |
| 手測 1：Devnet 解鎖後看到 Solana 與非零代幣；重新整理；殺掉行程再解鎖會再抓，磁碟沒有持倉檔 | 可測 | M4：非 SOL 列的 name／symbol 未寫。持久儲存只有 vault 與 settings，這一點可測 |
| 手測 2：上鎖後再解鎖，新輪完成前不顯示舊列 | 有缺口 | H2 |
| 手測 3：Mainnet 無 active RPC 時顯示引導；logcat 沒有 `api.mainnet-beta.solana.com` 的持倉呼叫；Skip 留下空狀態；Set RPC 打開既有 RPC 畫面 | 可測 | M2：引導的殼層與三語原文。公開 mainnet 不作為 fallback 與現碼 `endpointForCluster` 一致 |
| 手測 4：設好允許的 Mainnet RPC 回到資產看到 Solana 列；`api.helius.xyz` 且 query 含 `api-key` 時 SPL 仍載入，原生 SOL 自成一列 | 有缺口 | H1、M1、M7 |
| 手測 5：已驗證代幣的盾在圖示左側；活動仍無活動；MWA authorize 仍打開既有批准 | 部分可測 | 盾的位置已定案。M4 影響原生 SOL 會不會有盾。M8：sign-and-send 不在本步 |
| 單測：`formatBaseUnits`、Mainnet 空 active 不選 `PUBLIC_MAINNET`、Helius query、丟棄 decimals 0 與零餘額、原生 SOL 在 0 lamports 仍在、Wrapped SOL 不併入原生 SOL | 部分可測 | M6。`PUBLIC_MAINNET` 與 Helius 的 host／query 規則已足夠寫單測 |

### 與現碼抽樣（初審）

現碼沒有本版持倉，這不是設計 HIGH。抽樣裡沒有和提案互斥的現行行為。

| 錨點 | 與提案的關係 |
|------|----------------|
| `RpcResolver.endpointForCluster` | Devnet：允許的 active 自訂 URL，否則 `PUBLIC_DEVNET`。Mainnet：只有允許的 active 自訂 URL，否則 `null`。`PUBLIC_MAINNET` 只是常數，這個函式不回它。提案沿用此函式，與「禁止公開 mainnet 當持倉 fallback」一致 |
| `SettingsModels.AirwaveSettings` | 持久欄位已有 `heliusApiUrl`、`jupiterApiKey`。提案不新增 store。`heliusConfigured` 比 Helius 路徑寬，見 M1 |
| `SolanaJsonRpc` | 沒有 `getBalance` 或 `getTokenAccountsByOwner`。`post` 是 private。`estimateFeeLamports` 用 `optLong`。提案要求持倉整數由十進位字串建成 `BigInteger`，新檔用 `HttpURLConnection`，不沿用 `optLong` |
| `WalletModel.lock` | 已清 session、失敗化 in-memory `pending`，並把 route 設回 Unlock。`maybeIdleLock` 呼叫同一個 `lock()`。提案把 `holdings.clear()` 放在這裡，閒置上鎖會一併清掉。物件本身在上鎖後仍在，所以需要 H2 的 round id |
| `ShellScreens.HomeTabs` | 資產空文案是 `No assets yet.`，活動是 `No activity yet.`。提案換掉資產空狀態，活動文案維持。頂列是 hamburger 選單與短地址，不是 pill。INDEX 寫選單、Settings、Network、上鎖、MWA 維持 0.2.0，因此不採用 overall demo 的 kebab |
| 待批與儲存 | `pending` 只在 process 記憶體。持久狀態是 `airwave.vault.v1` 與 `airwave.settings.v1`。提案不把持倉寫入任一 store，也不新增待批廣播 |

整體 v1 程式檔寫「不要把功能邏輯追加進 `WalletModel.kt`」。INDEX 把這個檔的改動限在 `clear()`，與「不在 WalletModel 解析餘額」一致。行數豁免見 M9。

### 修復追蹤表（初審結束時）

| ID | 級 | 狀態 | 關閉位置（INDEX 條／HOW 節） |
|----|----|------|------------------------------|
| H1 | HIGH | 仍開 | — |
| H2 | HIGH | 仍開 | — |
| M1 | MEDIUM | 仍開 | — |
| M2 | MEDIUM | 仍開 | — |
| M3 | MEDIUM | 仍開 | — |
| M4 | MEDIUM | 仍開 | — |
| M5 | MEDIUM | 仍開 | — |
| M6 | MEDIUM | 仍開 | — |
| M7 | MEDIUM | 仍開 | — |
| M8 | MEDIUM | 仍開 | — |
| M9 | MEDIUM | 仍開 | — |
| L1 | LOW | 仍開 | — |
| L2 | LOW | 仍開 | — |
| L3 | LOW | 仍開 | — |
| L4 | LOW | 仍開 | — |

### 歷審摘要（初審結束時）

| 輪次 | 日期 | 結論 |
|------|------|------|
| 初審 | 2026-10-25 | 有未關閉 HIGH（H1、H2）。審查門檻未通過。提案可行，不是整份不可行 |
