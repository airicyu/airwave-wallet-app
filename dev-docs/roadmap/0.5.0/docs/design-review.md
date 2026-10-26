# Design review — 0.5.0 Airwave Wallet App

- 日期：2026-10-26（Asia/Hong_Kong）
- 輪次：**第 2 輪複審**（本檔保留初審題旨）
- 角色：設計審查（不改 INDEX／HOW／reasoning／HANDOFF／程式；不以本檔當已定案）
- 對照基準：[`../INDEX.md`](../INDEX.md) 已定案＋驗收（**現檔**）；[`reasoning.md`](./reasoning.md)；[`how.md`](./how.md)；[`../HANDOFF.md`](../HANDOFF.md)（流程提示，非產品契約）
- 上一版：[`../../0.4.0/INDEX.md`](../../0.4.0/INDEX.md)（`shipped`）
- 架構禁區：[`../../GUIDELINES.md`](../../GUIDELINES.md) §4、§8
- 畫面契約：[`../../../design-demos/wallet-overall-v1.html`](../../../design-demos/wallet-overall-v1.html) 電話框 `connect-dapp`、`approve-sign-tx`（不對照 `approve-sign-tx-fail` 出貨）
- 現行程式抽樣：`WalletModel.kt`（`offer`／`Pending`）、`WalletApp.kt`（`ApprovalScreen`）、`LocalAssociation.kt`、`solana/LegacyTransaction.kt`、`solana/SolanaJsonRpc.kt`、`holdings/BaseUnits.kt`、`i18n/AppStrings.kt`、`mwa-test-web/src/main.ts`
- **總評：** 無未關閉 HIGH。H1、H2 與 M1–M5 已寫進 INDEX／how／reasoning（非僅本審查檔）。M6 標非阻擋。INDEX 待拍板為空。HANDOFF 有 paste-ready。**審查門檻通過。** 提案**並非整份不可行**。

## Load-bearing decisions

| 決策 | 來源 | 判斷 |
|------|------|------|
| 協定沿用 0.1.0 MWA local associate；`sign_transactions` 立刻 decline | INDEX／reasoning | 與現碼 `onSignTransactionsRequest` 一致；未把 extension Wallet Standard 當主路徑 |
| 「簽交易」＝ `sign_and_send_transactions` | INDEX／reasoning | 與 `mwa-test-web` 的 Sign and send／`sendTransaction` 一致 |
| 待批最多一筆、只在行程記憶體；第二筆失敗不取代；結果只回該次 `finish` | INDEX | 符合 GUIDELINES；現碼 `offer`／`failPending` 已是此模型 |
| 持久狀態不另開 store；本版只換 MWA 畫面 | INDEX | 未發明第二條 rehydrate |
| 即時 AI 不做主路徑；卡面固定未知、報告鈕不發網路 | INDEX／reasoning | 符合可選 AI 禁區；demo 報告 overlay 明確不出貨 |
| 預期變動只保證系統 Transfer 一列 SOL；不出貨 refresh／USDC 列 | INDEX／reasoning | 刻意子集電話框；已寫死以免抄 HTML 偷渡 |
| 三個核准面右側必有 Lock（簽交易覆寫 demo 空頂列） | INDEX／reasoning／驗收 5 | 與 `design-principles` MWA Reject／Lock 一致 |
| 手測客戶端歸版 `mwa-test-web/` | INDEX／how Track C | 不是 extension `test-web` |
| 不加新套件 | INDEX／reasoning | 可行 |

## Findings（第 2 輪）

關閉＝已寫進 INDEX／HOW／reasoning（HANDOFF 可複述，不以本檔當契約）；仍開＝契約仍分叉；非阻擋＝不擋主路徑／驗收。

核對父代理聲稱寫入者：H1、H2、M1–M5 **均在現 INDEX／how／reasoning 可讀到完整句子**，不是只留在初審報告。

### HIGH

#### H1 — 出貨畫面缺 GUIDELINES 要求的 glyph 與三語列文案 — **關閉**

現 INDEX：連線／簽訊息／簽交易列順序；glyph 檔名 `ic_stroke_back`／`lock`／`key`／`ai`／`report`，並禁止 Material／`Canvas`／PNG；返回與鑰匙 `d` 已貼；盾／SOL 沿用既有；文案鍵表含 en／zh-Hant／zh-Hans（含拒絕、連線、批准、站點、AI、預期、手續費、明細、hex）；明寫**不出貨** refresh 與第二 mint／USDC 列。reasoning 亦記電話框有 refresh／USDC 故寫死不出貨。

殘差見 L5（lock／ai／report 未在 INDEX 貼完整 `d` 字面）。不足再開 HIGH。

#### H2 — Lock 在簽署交易畫面與驗收 5 互相矛盾 — **關閉**

現 INDEX：連線與簽訊息頂列右側 **必有** Lock（不是可選）；簽交易「另加頂列右側 Lock」覆寫 demo 空頂列。驗收 5：三面右上都有 Lock，並指定在**簽署並送出**按 Lock。how：三面右側 Lock＝`lock()`。

### MEDIUM

| ID | 狀態 | 說明 |
|----|------|------|
| M1 | **關閉** | how Track A 已列 `Pending` 具名 `kind` 與欄位（identity、UTF-8／hex 二擇一、`amountLamports`／`feeLamports` 為 `BigInteger`、指令名）；禁止只塞 `Pair` 再浮點解析。 |
| M2 | **關閉** | how Track A：`WalletApp` 只在 `Route.Approval` 建立核准 composable；`pending == null` 或離開該 route 後不得留在樹上；Approve／Reject／Lock／失敗經 `WalletModel` 清 `pending`。 |
| M3 | **關閉** | INDEX 簽訊息：bytes 保存；UTF-8 或 `mwa.message.hex`＋小寫 hex，不要 `decodeToString()` 當唯一路徑。how Track B 組裝同句。現碼仍 `decodeToString()`＝預期未實作，非設計分叉。 |
| M4 | **關閉** | INDEX 簽名前失敗：sign-and-send payload 空；**sign_messages 的 payloads 空或全空 bytes 不 `offer`**。how Track B 兩條都 decline。細微分叉見 M6。 |
| M5 | **關閉** | how Track B：有 pending 且 `address == null` 且 `route == Unlock` 時先 Unlock，解鎖後進 `Route.Approval`；明寫現碼已如此、不要拆掉。 |
| M6 | **非阻擋**（本輪新增） | INDEX「payloads 空或**全空 bytes**」與 how「空或**第一筆空**」不完全同句；how 只組第一筆 UTF-8／hex，INDEX 簽訊息畫面未寫「多筆只取 `payloads[0]`」。手驗 2 與測試頁只有一筆 UTF-8，不擋主路徑／驗收。規劃可補一句對齊，不擋開工。 |

### LOW

| ID | 狀態 | 說明 |
|----|------|------|
| L1 | **關閉** | 連線電話框右側為空 span；INDEX 已改為必有 Lock，屬已定案覆寫 demo。 |
| L2 | 記錄 | `DOMAIN.md` 無 MWA 畫面專名；待批定義已足夠。Connected site 仍 Not adopted。 |
| L3 | 記錄 | 現碼 `ApprovalScreen` 仍是鍵值列；本版要換的現況，不是設計 HIGH。 |
| L4 | 記錄 | 手測頁 identity 為 `Airwave MWA test`，demo 稿用 `jup.ag`。驗收以測試頁為準。 |
| L5 | 記錄（本輪） | `ic_stroke_lock`／`ai`／`report` 的 path `d` 未貼進 INDEX 字面；來源已點名（StrokeIcon lock、電話框 bot／文件 path）。實作從具名來源抄即可。 |

## 驗收對照

| INDEX 驗收句 | 設計層是否可測 | 缺口 |
|--------------|----------------|------|
| Gradle wrapper 三指令（JBR classpath） | 可測 | 無 |
| 1. Connect 電話框（字母卡、名稱、URI、帳戶列、拒絕｜連線）→ log connected | 可測 | Lock 改由驗收 5 覆蓋三面 |
| 2. Sign message 顯示 `airwave mwa test`；拒只敗該次 | 可測 | hex 路徑無手驗（本版 UTF-8 即可）；M6 非阻擋 |
| 3. Sign and send：站點、SOL 預期、手續費、拒絕｜批准 → 簽名；未知 chain／無 RPC 用單元測試 | 可測 | refresh／USDC 已定不出貨 |
| 4. watch：Connect 失敗、不開 approval、可見 `mwa.watchAccount` | 可測 | 無 |
| 5. 三面右上 Lock；在簽署並送出按 Lock；殺行程無殘留 | 可測 | H2 已關 |
| 6. `sign_transactions` 立刻失敗 | 可測 | 無 |
| 單元測試：第二筆 pending、watch、`sign_transactions` | 可測 | 覆蓋率留給實作審查 |

## 與現碼抽樣

- **非衝突、預期未實作：** `Pending` 仍 `title`＋`lines`；`ApprovalScreen` 鍵值表；`sign_messages` 仍 `decodeToString()`；核准主按鈕仍 Approve；drawable 尚無本版 `ic_stroke_*`。
- **與提案一致、可沿用：** `offer` 第二筆失敗不取代；watch 在 pending 前失敗；`lock`→`failPending`；解鎖中 pending 進 Unlock；`sign_transactions` decline；submit `rpcForSubmit`；Transfer／fee 用 `BigInteger`；`formatBaseUnits`；vault 仍 `airwave.vault.v1`。
- **抽樣不是 HIGH：** 現碼尚未換成 overall-v1 chrome。
- **重疊：** 本版替換 0.1.0 核准視覺，不平行第二套 Approval。

## 修復追蹤表

| ID | 級 | 狀態 | 關閉位置 |
|----|-----|------|----------|
| H1 | HIGH | 關閉 | INDEX 連線／簽訊息／簽交易列、文案鍵表、Glyph 列；reasoning 不出貨 refresh／USDC |
| H2 | HIGH | 關閉 | INDEX 三面必有 Lock；簽交易頂列覆寫；驗收 5；how Track A |
| M1 | MEDIUM | 關閉 | how Track A `Pending` 欄位 |
| M2 | MEDIUM | 關閉 | how Track A composable 生命週期 |
| M3 | MEDIUM | 關閉 | INDEX 簽訊息 hex；how Track B |
| M4 | MEDIUM | 關閉 | INDEX 簽名前失敗（含 sign_messages）；how Track B |
| M5 | MEDIUM | 關閉 | how Track B 解鎖中 pending |
| M6 | MEDIUM | 非阻擋 | （可選）INDEX／how 對齊「只取第一筆」vs「全空 bytes」 |
| L1 | LOW | 關閉 | 隨 H2 |
| L5 | LOW | 記錄 | INDEX Glyph 列已點名來源 |

## Waiver

（無。）

## Verdict

**proceed** — 無未關閉 HIGH；應修 MEDIUM 已進契約或標非阻擋。**審查門檻通過。** **不可行：否。**

---

## 初審全文（2026-10-26，歷史題旨保留）

當時總評：有未關閉 HIGH（H1 glyph／三語／refresh 與 USDC；H2 簽交易無 Lock 對不上驗收 5）；M1–M5 應修；提案可行、門檻未通過。題旨見上表；第 2 輪已核對寫入 INDEX／how／reasoning。

## 歷審摘要

| 輪次 | 日期 | 結論 |
|------|------|------|
| 初審 | 2026-10-26 | 門檻未通過；H1、H2 仍開；提案可行 |
| 第 2 輪複審 | 2026-10-26 | H1、H2、M1–M5 關閉（已在契約檔）；M6 非阻擋；**審查門檻通過**；不可行：否 |
