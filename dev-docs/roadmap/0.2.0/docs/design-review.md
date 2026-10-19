# Design review — 0.2.0 Airwave Wallet App

- 日期：2026-10-19（Asia/Hong_Kong）
- 輪次：**第 2 輪複審**
- 角色：設計審查（不改 INDEX／HOW／reasoning／HANDOFF／程式；不以本檔當已定案）
- 對照基準：[`../INDEX.md`](../INDEX.md) 已定案＋驗收；[`how.md`](./how.md)／[`reasoning.md`](./reasoning.md)／[`../HANDOFF.md`](../HANDOFF.md)；視覺契約 [`../../../design-demos/wallet-020.html`](../../../design-demos/wallet-020.html)
- 架構禁區：[`../../GUIDELINES.md`](../../GUIDELINES.md)
- 現行程式抽樣：`android/app/src/main/kotlin/com/airwave/wallet/WalletApp.kt`（Home／Lead／Approval 仍為 Lock 文字鈕）、`WalletModel.kt`（`passwordChars`／pending 在記憶體）、`LocalAssociation.kt`（`rpcFor` 依 request chain 對公開 RPC；sign-and-send 仍 decline）、`vault/VaultStore.kt`（`airwave.vault.v1`）
- **總評：無未關閉 HIGH；同意的 MEDIUM（M1–M4）已寫進契約。審查門檻通過。** 提案可行，非不可行。

## Findings（本輪）

關閉＝已寫進 INDEX／HOW／HANDOFF；仍開＝契約仍分叉；非阻擋＝記錄即可。

### HIGH

#### H1 — MWA sign-and-send 的 RPC 與 request.chain／Network `cluster` 關係未寫死 — **關閉**

**本輪核對：** 契約已採方案 A（request chain wins），三處一致、無分叉。

| 位置 | 完整句子／內容 |
|------|----------------|
| INDEX Product §7 | MWA sign-and-send submit still targets the **request’s chain**；Network `cluster` does not override a mismatched request |
| INDEX Decided「MWA submit RPC」 | **Request chain wins**；map → custom／public；mainnet empty → fail before signing；testnet → public only；unknown → fail；Network `cluster` must **not** redirect submit |
| `how.md`「RPC resolution」 | 同規則＋chain→cluster 對照表＋允許 URL 規則；Network `cluster` 僅摘要／badge |
| INDEX 手驗 B9／D16 | Devnet＋自訂 URL；mainnet-unset 簽署前失敗；允許規則；0.1.0 級目的地／金額／手續費／指令名；Devnet sign-and-send smoke |
| `reasoning.md` | H1 → scheme A |
| `HANDOFF.md` | paste-ready：MWA submit RPC = request chain then settings endpoints |

初審所列三問（衝突時、testnet、未知 chain）均有完整句子；不以現碼未接線為 HIGH。

### MEDIUM

| ID | 本輪狀態 | 依據 |
|----|----------|------|
| M1 | **關閉** | `how.md` Navigation shell：Home／shell 擁有 `menuOpen`＋overlay＋**dropdown**（非 drawer）；選目的地／Lock／scrim 先 dismiss 再導航；Back 回上一層（首推回 Home）。與 demo 一致。 |
| M2 | **關閉** | `how.md` English copy 後附 Android key → extension `ui-messages` 對照表；無對應鍵者註明留英文或括號內規範 zh 後備。手驗 B8 可對照同一 keys。 |
| M3 | **關閉** | INDEX 手驗 B9 已含：存活、Devnet 自訂 URL 送出、mainnet-unset 簽署前失敗、URL 允許規則、目的地／金額／手續費／指令名；D16 補 Devnet sign-and-send smoke。與 H1 方案 A 對齊。 |
| M4 | **關閉** | `how.md` Wallet password：成功後更新記憶體錢包密碼並**保持已解鎖**；後續本行程 vault 寫入用新密碼。擇一已寫死（非強制 Lock）。 |

### LOW

| ID | 本輪狀態 | 依據 |
|----|----------|------|
| L1 | **關閉／非阻擋** | HOW 明寫子頁頂欄 Optional Menu 允許；空頁 body 仍以 title＋一句 empty＋Back 為準。demo 頂欄 Menu 與 HOW 不再衝突。 |
| L2 | **非阻擋** | Address book／Connected sites 仍屬 DOMAIN Not adopted；INDEX 空位 placeholder 可接受。實作勿引入清單／CRUD／連線模型。 |
| L3 | **關閉** | JSON 示意改為 `"defaultCuPrice": integer` 並註 Kotlin `Long`／never double。 |
| L4 | **關閉** | HOW 定為 modal **dropdown**（phone），不再「dropdown or drawer」。 |

本輪無新增 HIGH／MEDIUM。未另開 finding：手驗未強制走「Network `cluster` 與 request chain 不一致」觀測路徑——規則已在 Decided／HOW 寫死，B9 已覆蓋一致路徑＋mainnet-unset，不擋門檻。

## 驗收對照

| 驗收句 | 設計層是否可測 | 缺口 |
|--------|----------------|------|
| A1–A4 漢堡、順序、空頁、無頂欄 Lock 文字 | 可測 | 無 |
| A5 Network 改 cluster 存活；不編輯 RPC URL | 可測 | 無 |
| A6 Settings 六列 | 可測 | 無 |
| A7 Lock → Unlock → 同地址 | 可測 | 無 |
| B8 Language 三語＋殺進程存活 | 可測 | M2 已關：對照表可抽樣 |
| B9 RPC＋request-chain 送出／mainnet-unset／允許規則／簽核摘要 | 可測 | H1／M3 已關 |
| B10 API keys 相位 | 可測 | 無 |
| B11 AI Settings 持久、不 assess、不簽名 | 可測 | 無 |
| B12 CU 非負整數存活 | 可測 | L3 已關 |
| B13 改密碼 | 可測 | M4 語意在 HOW；手驗「new password unlocks after」覆蓋殺進程後路徑 |
| C14 主題色 | 可測 | 無 |
| D15 空 Assets／Activity | 可測 | 無 |
| D16 burner＋authorize＋Devnet sign-and-send smoke | 可測 | 與 Track C 負荷相稱 |
| E17 無密文進 log／追蹤文件 | 可測 | 無 |
| Gradle assemble／test／lint | 可測 | Track D 已列 |

## 與現碼抽樣

現碼未做本版 ≠ 設計 HIGH。

| 現況 | 與提案關係 |
|------|------------|
| Home／Lead／Approval 頂欄為 Lock **文字**鈕 | 0.2.0 預期改為 Menu＋選單 Lock；非互斥錯誤 |
| `pending`／`passwordChars` 僅記憶體；vault `airwave.vault.v1` | 與 GUIDELINES／Lock／M4 相容 |
| `LocalAssociation.rpcFor` 依 request chain 對公開 RPC；sign-and-send 仍 decline | 與方案 A 方向一致；Track C 接線 settings 端點即可，勿改成 Network 覆寫 |
| 無 `airwave.settings.v1` | 預期新增；單一 durable store |
| 未見 settings 金額用 `double` | CU 維持 `Long` |

## 修復追蹤表

| ID | 級 | 狀態 | 關閉位置（INDEX 條／HOW 節） |
|----|----|------|------------------------------|
| H1 | HIGH | **關閉** | INDEX Product §7、Decided「MWA submit RPC」、手驗 B9／D16；`how.md` RPC resolution；`reasoning.md`；`HANDOFF.md` |
| M1 | MEDIUM | **關閉** | `how.md` Navigation shell／Hamburger ownership |
| M2 | MEDIUM | **關閉** | `how.md` English copy + `ui-messages` 對照表 |
| M3 | MEDIUM | **關閉** | INDEX Hand-test B9、D16 |
| M4 | MEDIUM | **關閉** | `how.md` Wallet password（記憶體密碼＋保持解鎖） |
| L1 | LOW | **關閉／非阻擋** | `how.md` Back／Optional Menu |
| L2 | LOW | **非阻擋** | INDEX 空位；DOMAIN Not adopted（不實作產品模型） |
| L3 | LOW | **關閉** | `how.md` JSON `defaultCuPrice` integer／Long |
| L4 | LOW | **關閉** | `how.md` Hamburger = dropdown only |

## 歷審摘要

| 輪次 | 日期 | 結論 |
|------|------|------|
| 初審 | 2026-10-19 | 有未關閉 HIGH（H1）；提案可行，修契約後再實作；門檻未通過 |
| 第 2 輪複審 | 2026-10-19 | H1／M1–M4／L1／L3／L4 關閉；L2 非阻擋；**審查門檻通過**；提案可行 |

---

## 初審全文（保留）

- 日期：2026-10-19（Asia/Hong_Kong）
- 輪次：**初審**
- 角色：設計審查（不改 INDEX／HOW／reasoning／HANDOFF／程式；不以本檔當已定案）
- 對照基準：[`../INDEX.md`](../INDEX.md) 已定案＋驗收；[`how.md`](./how.md)／[`reasoning.md`](./reasoning.md)／[`../HANDOFF.md`](../HANDOFF.md)；視覺契約 [`../../../design-demos/wallet-020.html`](../../../design-demos/wallet-020.html)
- 架構禁區：[`../../GUIDELINES.md`](../../GUIDELINES.md)
- 現行程式抽樣：`android/app/src/main/kotlin/com/airwave/wallet/WalletApp.kt`（Home／Lead 仍為 Lock 文字鈕）、`WalletModel.kt`（session／pending 在記憶體）、`LocalAssociation.kt`（`rpcFor` 依 request chain 對公開 RPC）、`vault/VaultStore.kt`（`airwave.vault.v1`）
- **總評：有未關閉 HIGH。** 審查門檻未通過；修完 H1（並建議一併收斂 MEDIUM）後方可進實作。提案本身可行，非不可行。

### Findings（初審）

#### H1 — MWA sign-and-send 的 RPC 與 request.chain／Network `cluster` 關係未寫死 — **仍開**（初審時）

`how.md`「RPC resolution」只讀 settings 的 `cluster`＋`rpcByCluster`，INDEX 手驗 B9 亦寫「MWA sign-and-send uses Network’s `cluster`＋that cluster’s active RPC」。0.1.0 已定案／已 shipped 的路徑則是：依 **request 的 chain／cluster** 對公開 RPC；未知 chain 簽署前失敗；手驗 C 明示「submits via the public RPC for **that chain**」。0.2.0 Decided 又要求「Burner, vault, empty Assets/Activity tabs, **MWA must keep working**」。

三句並陳時，實作者無法判斷：

1. 請求 chain 與 Network `cluster` 不一致時，是拒絕、仍依請求 chain 解析、還是強制送往 Network 所指端點；
2. 0.1.0 已支援的 `solana:testnet`（及 legacy 別名）在本版無 Network UI 時，送出是繼續公開 testnet RPC、還是一律失敗；
3. 「未知 chain 簽署前失敗」是否仍成立。

這不是「現碼尚未做 0.2.0」——而是契約自相矛盾，且會讓簽署後送出可能打到錯誤叢集，屬主路徑安全／驗收不可測。

**建議寫進已定案（擇一寫死，並同步手驗）：**

- **方案 A（延續 0.1.0 語意，較不易誤送）：** `sign_and_send` 仍以 request 的 chain／cluster 決定目標叢集；該叢集在 settings 有允許的 active 自訂 URL 則用之，否則用該叢集公開預設（mainnet 無 active 則 unset、簽署前失敗）；request chain 無法對應 `devnet`／`mainnet`／`testnet`（或 INDEX 點名的集合）則簽署前失敗。Network 畫面的 `cluster` 只影響應用內預設／摘要，**不覆寫**與請求不符的送出目標。  
- **方案 B（若產品要 Network 主導送出）：** 明寫「送出 URL 只依 Network `cluster`＋該叢集 RPC」；並另寫 request chain 與 `cluster` 不一致時的處理（必須拒絕或必須提示後仍送——擇一），以及 testnet 請求的命運；並改寫「MWA must keep working」與 0.1.0 手驗對齊語句，避免默示延續 chain→RPC。

無論 A／B，手驗須能走一條可觀測路徑（含 mainnet-unset 不送出，以及 chain／cluster 一致或衝突的一種）。

#### MEDIUM（初審時均仍開）

| ID | 本輪狀態 | 依據 |
|----|----------|------|
| M1 | 仍開 | `how.md` Navigation shell 未寫 Compose 擁有者：誰建立／關閉 hamburger overlay、從選單 push Network／Settings／placeholder 時選單是否先 dismiss、子頁 Back 回到何處。`design-principles.md` widget ownership 要求新畫面塊寫清父子生命週期；demo 為 modal dropdown，HOW 仍寫「dropdown **or** drawer」。 |
| M2 | 仍開 | INDEX／HOW 要求 `zh-Hant`／`zh-Hans` 目錄與手驗 B8「UI strings update」，但規範字串表只有 English。實作者只能自造譯文，手驗無法對照「同一 keys」是否正確。至少應在 HOW 附上與 English keys 對齊的兩份表，或寫明「本版 zh 暫鏡射 extension `ui-messages` 某路徑／某 commit 的對應鍵」並可抽樣核對。 |
| M3 | 仍開 | 手驗對 RPC 改線後的送出覆蓋不足：B9 寫存活與「uses Network’s cluster」，但未要求驗證 mainnet-unset 簽署前失敗、自訂 URL 允許規則（https／localhost http）、以及 0.1.0 級 sign-and-send（目的地／金額／手續費／指令名）在改線後仍成立。D16 僅 authorize smoke，與 Track C「Wire MWA sign-and-send submit」負荷不成比例。應在 INDEX 手驗補最短可執行步驟（與 H1 定案一併改）。 |
| M4 | 仍開 | Wallet password：HOW 寫 re-box vault、成功後新密碼可 unlock；未寫成功當下行程內 `passwordChars`／開鎖態是否立即換成新密碼。若仍持舊密碼字元，後續需寫 vault 的路徑會失敗或行為不定。建議一句：成功後更新記憶體中的錢包密碼並保持已解鎖，或強制 lock 再以新密碼 unlock（擇一）。 |

#### LOW（初審時）

| ID | 本輪狀態 | 依據 |
|----|----------|------|
| L1 | 仍開 | `wallet-020.html` 空頁／Network 頂欄同時有 Back 與 Menu；HOW 空頁只規範 title＋一句 empty＋Back。非衝突級，實作以 HOW／INDEX 為準時註明 demo 頂欄 Menu 可省略。 |
| L2 | 仍開 | 選單列「Address book」「Connected sites」屬 DOMAIN **Not adopted** 語意；INDEX 已定空位 placeholder，可接受。實作勿引入清單／CRUD／連線模型；真做產品時再寫進 DOMAIN。 |
| L3 | 仍開 | HOW JSON 示意 `"defaultCuPrice": number` 易誤讀成 `double`；同節已寫 Kotlin `Long`／非負整數。示意改為 integer／Long 註記即可。 |
| L4 | 仍開 | HOW「modal dropdown or drawer」二擇一；Android 視覺契約 demo 已是右上 dropdown。建議 HOW 與 demo 對齊為 dropdown（phone），避免實作分叉。 |
