# Design review — 0.1.0 Airwave Wallet App

- 日期：2026-10-18（Asia/Hong_Kong）
- 輪次：**第 2 輪複審**（初審同日，見文末）
- 角色：設計審查（不改 INDEX／HOW／reasoning／HANDOFF／程式；不以本檔當已定案）
- 對照基準：[`../INDEX.md`](../INDEX.md) 已定案＋驗收；[`how.md`](./how.md)、[`reasoning.md`](./reasoning.md)、[`workflow.md`](./workflow.md)、[`../HANDOFF.md`](../HANDOFF.md)、[`../../../design-demos/wallet-010.html`](../../../design-demos/wallet-010.html)
- 架構禁區：[`../../GUIDELINES.md`](../../GUIDELINES.md) 第 4 節。本版 INDEX 已把 Mobile Wallet Adapter 訂為主路徑，並把鏈上整數單位訂為 `BigInteger`。
- 領域：[`../../../DOMAIN.md`](../../../DOMAIN.md)。未列入「Not adopted」的詞才使用。無上一版。backlog 為空，不當作已定案。
- 現行程式抽樣：無。倉庫沒有 `android/` 應用程式碼。
- **總評：** 無未關閉 HIGH。M1–M6 已寫進 INDEX／how／reasoning，關閉。提案可行。LOW 不擋主路徑。審查門檻通過。

## 承重決定

`reasoning.md` 寫明本版做什麼、以及捨棄的路徑：只做 Android、用 Mobile Wallet Adapter 而不是自訂 scheme、不實作已淘汰的 `sign_transactions`、送出用公開叢集 RPC 而不是設定畫面。同檔已有套件理由，並寫明 AndroidX 只包檔、不取代密碼箱。INDEX「Still to decide」是空的。待批只在記憶體、持久狀態只有 `airwave.vault.v1`、結果回到該次請求、密碼箱、金額不用 `double`，與 GUIDELINES 一致，INDEX 沒有推翻這些禁區。

## Findings

關閉＝已寫進 INDEX／HOW／reasoning；仍開＝契約仍分叉；非阻擋＝記錄即可。ID 不重編。

### HIGH

無。

### MEDIUM

#### M1 — 產生畫面的複製地址三處不一致 — **關閉**

初審：`how.md` Generate 列沒有複製；手驗 A 的 Copy 讀成只測 Home。

第 2 輪：`how.md` Generate 列寫 `Copy address` 把完整預覽地址放上剪貼簿。INDEX 手驗 A 寫預覽複製與 Home 複製都放上完整地址。`workflow.md` 兩處仍寫縮寫顯示、複製為完整地址。

#### M2 — 沒有保險庫或沒有錢包帳戶時，MWA 請求沒有結果 — **關閉**

初審：冷啟動可能還沒設密碼或還沒有 burner，該次請求沒有失敗句。

第 2 輪：`how.md` 的 `authorize`、`sign_messages`、`sign_and_send_transactions` 都寫沒有保險庫或沒有 burner 就失敗該次、不留到之後。INDEX 手驗 C 寫冷啟動無保險庫、或已解鎖但無帳戶時，該次失敗，之後建立帳戶也不補完成。待批仍不寫入 `airwave.vault.v1`。

#### M3 — 簽並送出只描述一筆轉帳 — **關閉**

初審：畫面與手驗只覆蓋一筆系統轉帳，沒寫多指令、非轉帳、手續費來源與解析失敗。

第 2 輪：`how.md` 方法列寫每個指令名稱；只有系統轉帳才顯示目的地與金額；手續費是由字串建成的 `BigInteger`，取自公開 RPC 的模擬或費用欄位；畫面 SOL 文字可用 `BigDecimal`，簽署與送出不用該十進位值；依該規則需要的目的地、金額或手續費解析失敗則簽署前失敗；未知 chain 簽署前失敗。手驗 C 仍以一筆系統轉帳為可執行路徑，並另寫未知 chain 不送出。Screens 列的 To／Send／單一 Instruction 對齊 demo 的那一筆轉帳；一般形狀以方法列為準。

#### M4 — 待批畫面誰建立、誰丟掉沒寫 — **關閉**

初審：沒寫 Compose 上誰持有這次待批。

第 2 輪：`how.md` Session 寫擁有 session 的那一層建立核准畫面，並在 Approve 完成、Reject 完成、Lock、或該次請求失敗時丟掉；核准 composable 不留在該擁有者之上跨到之後的畫面。

#### M5 — 第三方套件與密碼箱分層沒在 reasoning 寫死 — **關閉**

初審：套件只在 `how.md`，reasoning 沒有句子；AndroidX Keystore 與密碼箱可能被做成同一層。

第 2 輪：`reasoning.md` Dependencies 為 `mobile-wallet-adapter-walletlib`、`web3-solana`、`multimult`、AndroidX Security、`com.lambdapioneer.argon2kt:argon2kt` 與 JDK AES-GCM 各有一句，並寫 AndroidX 不取代密碼箱。`how.md` 寫 JSON 內容才是密碼箱（Argon2id 與 AES-GCM，密碼不入檔），AndroidX 只包這一顆檔。版本在建立 `android/` 時於 Gradle 釘死。

#### M6 — 手驗沒蓋到契約已寫的失敗句 — **關閉**

初審：密碼不一致、短於 8 字、未知 chain、`deauthorize` 沒有手驗。

第 2 輪：手驗 A 含 `Passwords do not match.` 與 `Password is too short.`。手驗 C 含未知 chain 簽署前失敗且不送出，以及 `deauthorize` 無額外畫面、該 `auth_token` 之後不能略過核准。

### LOW

| ID | 狀態 | 依據 |
|----|------|------|
| L1 | 仍開，非阻擋 | demo Unlock 欄位是 `Wallet password`。`how.md` Unlock 列有標題、lead、按鈕與錯誤句，沒有欄位文案。 |
| L2 | 仍開，非阻擋 | `workflow.md` 仍寫 `sign_transactions` 僅在明確要相容時才支援。INDEX 與 `how.md` 已定不實作並立即失敗。以 INDEX 為準。 |
| L3 | 仍開，非阻擋 | Home 的 wallet widget 在 demo 標成 `Wallet`。`how.md` 只規定 Generate 的 `Solana address`，Home 列沒有這個標籤。 |
| L4 | 第 2 輪新增，非阻擋 | `how.md` Session 列舉解鎖旗標、密鑰位元組、地址、至多一個待批，沒有 `auth_token`。`deauthorize` 寫丟掉該 token。手驗只要求 deauthorize 之後不能略過；不略過即滿足。若實作略過，token 只能在該行程記憶體：保險庫只放零或一個 burner，不能另開 store。 |
| L5 | 第 2 輪新增，非阻擋 | DOMAIN 餘額的 Code 仍寫 `BigInt`。本版 INDEX 已把 lamports 與手續費訂為 `BigInteger`。實作以 INDEX 為準。 |
| L6 | 第 2 輪新增，非阻擋 | `how.md` Screens 的 Sign and send 仍以一筆轉帳的欄位（to、send amount、一個 instruction name）描述，與方法列的「每個指令、僅系統轉帳才顯示目的地與金額」並列。手驗 C 與 demo 都是那一筆轉帳。一般形狀以方法列為準。 |

## 驗收對照

| 驗收句 | 設計層是否可測 | 缺口 |
|--------|----------------|------|
| A 兩次密碼不同、短於 8 字、合法密碼、產生 burner、預覽複製完整地址、Roll again、Home 同一地址、Home 複製完整地址、上鎖／解鎖、錯誤密碼、殺掉行程後同一地址 | 可測 | 無 |
| B 兩個空分頁文案，widget 兩邊都在 | 可測 | 無帳戶的 Home 在 demo 沒有分頁；手驗 B 是已有帳戶之後。不衝突。 |
| C 無保險庫或無帳戶的請求失敗且事後不補完成；核准；拒絕只失敗該次；簽訊息；一筆系統轉帳顯示目的地／金額／手續費／指令名；未知 chain 簽署前失敗且不送出；`deauthorize` 無額外畫面且該 token 不能再略過；`sign_transactions` 不掛起；核准中 Lock 失敗該次且核准畫面消失；殺掉行程後該次不在；第二個請求只失敗第二個 | 可測 | 非轉帳與多指令的解析失敗在 `how.md`，手驗不另走。見 L6。 |
| D 紀錄與受追蹤文件沒有密鑰與密碼 | 可檢 | 無 |
| `assembleDebug`、`testDebugUnitTest`、`lintDebug` | 有指令 | 現無 `android/`，不構成設計 HIGH。 |

## 與現碼抽樣

無 `android/` 程式，沒有與本提案互斥的現行行為。未實作本版不算設計 HIGH。未把 backlog、未排程構想或 Chrome extension 行為當成契約。INDEX 已排程 MWA，不因協定本身判 HIGH。workflow 只借用「burner 預覽」這個已寫進本版的行為。

## 修復追蹤表

| ID | 級 | 狀態 | 關閉位置（INDEX 條／HOW 節） |
|----|----|------|------------------------------|
| M1 | MEDIUM | 關閉 | `how.md` Screens Generate；INDEX 手驗 A |
| M2 | MEDIUM | 關閉 | `how.md` MWA `authorize`／`sign_messages`／`sign_and_send_transactions`；INDEX 手驗 C |
| M3 | MEDIUM | 關閉 | `how.md` MWA `sign_and_send_transactions`；INDEX 手驗 C 未知 chain |
| M4 | MEDIUM | 關閉 | `how.md` Session |
| M5 | MEDIUM | 關閉 | `reasoning.md` Dependencies；`how.md` Dependencies 與 Durable store |
| M6 | MEDIUM | 關閉 | INDEX 手驗 A、C |
| L1 | LOW | 仍開，非阻擋 | `how.md` Unlock 列 |
| L2 | LOW | 仍開，非阻擋 | `workflow.md` `sign_transactions` 句 |
| L3 | LOW | 仍開，非阻擋 | `how.md` Home 列 |
| L4 | LOW | 仍開，非阻擋 | `how.md` Session 與 `deauthorize` |
| L5 | LOW | 仍開，非阻擋 | `dev-docs/DOMAIN.md` Balance |
| L6 | LOW | 仍開，非阻擋 | `how.md` Screens Sign and send 與方法列 |

## 歷審摘要

| 輪次 | 日期 | 結論 |
|------|------|------|
| 初審 | 2026-10-18 | 可行。無 HIGH。M1–M6 仍開，門檻未通過。 |
| 第 2 輪 | 2026-10-18 | 可行。M1–M6 已寫入契約並關閉。無未關 HIGH。LOW 非阻擋。門檻通過。 |

## 初審紀錄（2026-10-18）

初審總評曾為：無未關閉 HIGH，M1–M6 尚未寫回契約，門檻未通過。問題原文保留於上列各 ID 的「初審」句。初審沒有 HIGH。
