# Design review — 0.4.0 Airwave Wallet App

- 日期：2026-10-25（Asia/Hong_Kong）
- 輪次：**初審**
- 角色：設計審查（不改 INDEX／HOW／reasoning／HANDOFF／程式；不以本檔當已定案）
- 對照基準：[`../INDEX.md`](../INDEX.md) 已定案＋驗收；[`reasoning.md`](./reasoning.md)；[`how.md`](./how.md)；[`../session-handover.md`](../session-handover.md)（reconcile 提示，非產品契約）
- 架構禁區：[`../../GUIDELINES.md`](../../GUIDELINES.md)
- 現行程式抽樣：`WalletModel.kt`、`WalletSettings.kt`、`ui/ShellScreens.kt`、`ui/SettingsScreens.kt`、`settings/SettingsModels.kt`、`holdings/HoldingsModel.kt`、`vault/VaultStore.kt`／`BurnerKeys.kt`
- **總評：** 有未關閉 HIGH；提案方向可行，但 INDEX 尚未收斂與 0.3.0  holdings／vault 互動及可執行驗收，**審查門檻未通過**。

## Load-bearing decisions

| 決策 | 來源 | 初審判斷 |
|------|------|----------|
| 單一 vault 檔、plaintext 內 `accounts` 陣列；`activeAccountId` 只在 settings | INDEX Decided | 與 GUIDELINES「一個 durable secret store」一致；與現碼 `{seed}` 為過渡，非互斥 |
| 舊 `{seed}` 在 unlock 時遷移為一筆 `burner`、移除 `seed` 鍵 | INDEX Migration | 邏輯清楚；**何時寫回 ciphertext**（首次 unlock 是否立即 re-seal）未寫死，見 M3 |
| 衍生用既有 Bouncy Castle、不新增套件 | reasoning／INDEX | 可行；實作風險在測試向量與 wordlist 檔，非契約矛盾 |
| `watch` 在 MWA 進待批前失敗 | INDEX Signing | 與架構「待批只在記憶體」相容；**入口與字串鍵** how 未點名，見 M4 |
| 合併帳戶列僅導向說明屏、不寫入 | INDEX Combined row | 與 demo 與 0.8.0 出界一致 |
| 不把 derivation 塞進 `WalletModel`、新檔在 `vault`／`ui` | INDEX File shape | 與現碼 header 及已完成的 structure pass 方向一致 |
| Home pill 改為可開帳戶列表 | INDEX Pill | 與 0.3.0「pill 不可點」為**刻意演進**；現碼 `HomeTabs` 地址列無 `clickable`，實作時替換即可 |

## Findings（本輪）

關閉＝已寫進 INDEX／HOW／HANDOFF；仍開＝契約仍分叉；非阻擋＝記錄即可。

### HIGH

#### H1 — INDEX 缺「節奏三句」與 0.3.0 holdings 互動延伸 — **仍開**

[`version-delivery` SKILL](../../../.agents/skills/version-delivery/SKILL.md)「節奏」第 3 條要求：送設計初審前，父代理須把三句寫進 INDEX，且與 how 同一套。現況 **0.4.0 INDEX 的 Decided 表與 Acceptance 均未收斂**，而 [`session-handover.md`](../session-handover.md) 僅在 handover 內列出（不構成契約）：

1. **Vault 開啟失敗：** `{seed}` 與 `accounts` 兩種 plaintext 若無法套用，共用**同一**失敗句，不得另造第二種「打不開保險庫」文案。
2. **Session 與 holdings round：** `lock` 與**切換 active account** 須作廢 holdings round id（晚到 HTTP 不得繪製）；`lock` 清零 session secret 與 holdings；generate／import **draft** 在 `lock` 後不得 commit；待批仍在記憶體、`lock` 失敗之、行程死亡不恢復。
3. **Null endpoint：** 不得進入 `loading`、不得發請求；切帳戶仍走既有 `HoldingsModel.sync`，不得在 endpoint 檢查前設 `loading`。

0.3.0 已在自家 INDEX 寫死 (2)(3) 的 holdings 行為（見 [0.3.0 INDEX](../../0.3.0/INDEX.md) Session 行）。0.4.0 在切帳戶、多帳戶 vault 失敗、以及 draft 生命週期上**必須明寫延伸**，否則 implementer 可能只改 `address` 字串而漏掉 round 作廢或 draft 邊界，與已定案 0.3.0 行為分叉。

**建議寫進 INDEX Decided（各一句，英文與現表一致）：** 合併上列三點；失敗句若沿用 0.3.0 餘額失敗句，需寫明是否與 `password.wrong` 分流（vault JSON 無效 vs 密碼錯誤）。

#### H2 — Acceptance 的 Gradle 指令與本倉庫環境不一致 — **仍開**

INDEX Acceptance 要求 `gradlew.bat :app:…`，但 handover 與 skill 已記錄：**`android/` 下無 `gradlew.bat`**，須以 `JAVA_HOME=…\Android Studio\jbr` 加 `gradle-wrapper.jar` 執行。驗收句若照 INDEX 字面執行會失敗，違反 GUIDELINES「hand-test 可跑」精神。

**建議：** 將 Acceptance 三條改為與 handover 相同的 wrapper 指令（或等價一句「從 `android/` 用 repo 記錄的 Gradle wrapper 指令」並連結 handover／setup）。

### MEDIUM

| ID | 狀態 | 說明 |
|----|------|------|
| M1 | 應修 | **Session 秘密來源：** 現碼 `WalletModel` 單一 `seed`／`address`（見 `applyPlain`、`approve`）。INDEX 要求「active 帳戶秘密僅在 unlock 期間在記憶體」。how Track A 只寫 unlock 呼叫遷移，未寫：由 `activeAccountId` 解析 signing material、`watch` 無 secret、`lock` 清零範圍、以及 MWA `offer`／`approve` 在 `watch` 上於待批前失敗（含錯誤字串須進 `AppStrings` 三語）。 |
| M2 | 應修 | **Settings 持久化：** `AirwaveSettings` 尚無 `activeAccountId`（`SettingsModels.kt`）。how 僅一句「string field on the existing settings JSON」，未寫 `toJson`／`fromJson` 鍵名、空字串與遷移後預設（與 INDEX「empty 時設為 migrator 的 id」）的寫入時機。 |
| M3 | 應修 | **遷移寫回：** INDEX 要求遷移後 plaintext **不得保留 `seed` 鍵**。how／INDEX 未寫首次 unlock 遷移後是否**立即 re-seal** vault，或僅在下次帳戶變更時寫入。手驗 5「列表與 active id 仍存在」在多帳戶情境下依賴 vault 內 `accounts` 已持久化；建議在 Track A 寫死「遷移成功後於同一次 unlock re-seal（或等價一句）」。 |
| M4 | 應修 | **導航與 enum：** how 寫 `AccountsScreen` 取代 `EmptyAccounts`，但未寫 `HomeDestination`、menu 與 **pill** 路由（INDEX：pill 開列表、選列後 **pop 至 Home** 並 refresh）。現碼 menu 仍 `push(EmptyAccounts)`，pill 不可點——屬預期差異，但 how Track B 應列出要改的呼叫點，避免實作漏改 pill。 |
| M5 | 應修 | **改密碼與 vault 形狀：** 現碼 `changePassword` re-seal 當前 plaintext。多帳戶後須保證 re-seal 的是 `accounts` 陣列而非舊 `{seed}`；how 未提及，易在 Track A 之後被遺漏。 |
| M6 | 應修 | **INDEX Reconcile 橫幅：** 仍寫「Written before 0.3.0 code exists」，與現有 `HoldingsModel`、`{seed}` vault 矛盾。應在 reconcile 時改為指向實際型別與檔名（handover 已列）；否則審查後 implementer 可能以為尚未對樹。 |
| M7 | 應修 | **驗收與測試敘述：** INDEX 單元測試段對 phantom 穩定地址／fixture 助記詞條件較長，設計上可測，但 how Track C 僅寫 checksum 與 secret 長度；建議 how 對齊 INDEX 允許的測試範圍（公知向量或僅 checksum／path 格式），避免實作審查爭議。 |

### LOW

| ID | 狀態 | 說明 |
|----|------|------|
| L1 | 記錄 | Design demo [`wallet-overall-v1.html`](../../../design-demos/wallet-overall-v1.html) 已連結 `accounts`／`add-account`；初審未逐像素對照 Compose，實作階段再對 chrome。 |
| L2 | 記錄 | `DOMAIN.md` 錢包帳戶仍描述 0.1.0 單一 burner；INDEX 已要求 Track A 先更新——順序正確，非阻擋。 |
| L3 | 記錄 | Structure pass 已部分完成（`ShellScreens.kt` 拆出 `RpcScreens`／`SettingsScreens`，`WalletModel` 約 334 行）；與 handover「先 structure 再 reconcile」一致，不構成 0.4.0 產品 HIGH。 |

## 驗收對照

| INDEX 驗收句 | 設計層是否可測 | 缺口 |
|--------------|----------------|------|
| 1. 舊 vault unlock 為一 burner、Assets 仍載入 | 可測，遷移＋`activeAccountId` 邏輯清楚 | M3 寫回時機；H1 holdings 切帳 |
| 2. 12 詞生成、reroll、commit；lock 後列表有址；Logcat 無詞 | 可測 | draft／lock 互動見 H1；字串與路由在 Track C |
| 3. watch 匯入；MWA authorize 失敗、不開待批 | 可測 | M1 `offer` 與錯誤 copy |
| 4. 切回 burner；資產跟址；合併列不寫入 | 可測 | M4 pill pop |
| 5. 殺 process 後列表與 active id；待批不復活 | 可測 | 架構已符合現碼 `failPending`／無待批持久化 |
| Gradle 三指令 | **不可照 INDEX 字面執行** | H2 |
| 單元測試（BIP39／secret） | 可測 | M7 how 對齊 |

## 與現碼抽樣

- **非衝突、預期差異：** `HomeDestination.EmptyAccounts`＋`EmptyScreen`；home 地址列無點擊；vault `commitGenerate` 仍寫 `seed`；`AirwaveSettings` 無 `activeAccountId`。
- **與提案一致、可沿用：** `airwave.vault.v1`／`airwave.settings.v1` 檔名；`BurnerKeys`／Bouncy Castle；`holdings.clear()` 於 `lock`；`HoldingsModel` 在 `endpoint == null` 時走 `absentEndpoint()`（phase `guide`／`skipped`，**不**設 `Loading`）；round id 作廢模式可延伸至切帳戶（見 H1）。
- **互斥若未改會炸：** 實作後仍單一 `seed` 欄位則違反 INDEX 多帳戶模型；在 `ShellScreens` 堆新帳戶屏違反 file shape（應獨立 `ui/*`）。
- **0.3.0 pill 行為：** 0.3.0 INDEX 明訂 pill 不可導航；0.4.0 INDEX 明訂改為開列表——屬版本演進，非現碼 bug。

## 修復追蹤表

| ID | 級 | 狀態 | 關閉位置（待父代理寫入） |
|----|-----|------|---------------------------|
| H1 | HIGH | 仍開 | INDEX Decided（三句互動） |
| H2 | HIGH | 仍開 | INDEX Acceptance（Gradle 指令） |
| M1 | MEDIUM | 應修 | INDEX 和／或 how Track A／C（session、MWA watch） |
| M2 | MEDIUM | 應修 | how Track A（settings JSON） |
| M3 | MEDIUM | 應修 | INDEX 或 how Track A（遷移 re-seal） |
| M4 | MEDIUM | 應修 | how Track B（路由／pill） |
| M5 | MEDIUM | 應修 | how Track A（changePassword） |
| M6 | MEDIUM | 應修 | INDEX Reconcile 行 |
| M7 | MEDIUM | 應修 | how Track C（單元測試範圍） |

## Waiver

（無。）

## Verdict

**proceed after fixes** — 修正 H1、H2 並建議收斂 M1–M7 後，可進 ID-only 設計複審；提案本身**不可行**：否。

## 歷審摘要

| 輪次 | 日期 | 結論 |
|------|------|------|
| 初審 | 2026-10-25 | 門檻未通過；H1、H2 仍開 |
