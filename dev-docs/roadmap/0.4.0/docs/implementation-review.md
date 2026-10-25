# Implementation review — 0.4.0 Airwave Wallet App

- 日期：2026-10-25（Asia/Hong_Kong）
- 輪次：**初審**
- 角色：實作審查（不改程式、不加功能、不 commit）
- 對照基準：[`../INDEX.md`](../INDEX.md) 已定案＋驗收；[`how.md`](./how.md)、[`reasoning.md`](./reasoning.md)
- 架構禁區：[`../../GUIDELINES.md`](../../GUIDELINES.md)。檔案形狀見 [`../../../code-design.md`](../../../code-design.md)
- 對照程式：working tree（`android/` 未提交變更；含 structure pass 拆檔與 0.4.0 帳戶功能）
- 本檔的 H／M／L 是實作審查自己的編號，與 [`design-review.md`](./design-review.md) 的編號不是同一批
- **總評：** 有未關閉 **HIGH**。Track A／B 多帳 vault、遷移 re-seal、列表／pill／切帳與 holdings round 作廢、合併列占位、watch 在 `offer` 前拒批等大方向與 INDEX／how 一致，但助記詞匯入 UI、12 詞生成預覽、MWA watch 文案接線，以及 INDEX 明示的單元測試尚未達標。父代理回報 `:app:testDebugUnitTest` 與 `:app:assembleDebug` 通過（17 tests）；**未回報** `:app:lintDebug`。模擬器 INDEX 五步手驗未跑。

## 架構禁區（本版帳戶與簽署路徑）

抽樣 `WalletModel`、`WalletAccountOps`、`VaultAccounts`、`holdings/HoldingsModel`、`LocalAssociation`、`SettingsStore`。

- 待批仍是 `WalletModel.pending` 的記憶體物件。`lock` 呼叫 `failPending()`。vault／settings 沒有寫入待批 payload。
- 耐久狀態：vault `airwave.vault.v1` 內 `accounts` 陣列；`activeAccountId` 在 `airwave.settings.v1`。`setActiveAccountId`／`removeAccount` 透過 `persistVault()`／`updateSettings` 寫回。
- 密碼箱：無硬編碼密碼；`signingSeed` 僅 unlock 期間在記憶體，`lock` 經 `clearSigningMaterial()` 清零。助記詞草稿欄位在 `lock` 時清空。
- `setActiveAccountId` 先 `holdings.clear()` 再 `syncHoldings(refresh = false)`；`HoldingsModel.sync` 在 endpoint 為 null 時不進入 `loading`、不抓取，與 0.3.0 延伸一致。
- 持倉數字路徑仍用 `BigInteger`／`BigDecimal`（沿用 0.3.0 `holdings/`）。本版帳戶路徑未引入 `double` 算餘額。
- 衍生用既有 Bouncy Castle；`Bip39English` 讀 `res/raw`，未新增 Gradle 套件。
- `changePassword` 在 `accountRows` 非空時 re-seal `VaultAccounts.toJsonBytes(accountRows)`，不是 legacy `{seed}`。

## Findings

### HIGH

#### H1 — 匯入助記詞缺少路徑種類與索引 0…4 預覽 — **仍開**

INDEX Decided：12 或 24 英文詞、checksum；路徑種類 `phantom`／`cli`／`change`（預設 phantom）；**預覽索引 0 到 4 供使用者選一**；不得自訂路徑字串。

現況：`ImportMnemonicScreen` 只有多行輸入與 Commit；`importPathKind`／`importPathIndex` 維持預設（Phantom、0），沒有 UI 切換路徑種類，也沒有五個地址預覽讓使用者挑選。後端 `Bip39.deriveSolanaSeed`／`Slip10.pathFor` 已具備能力，但契約要求的互動未實作。

#### H2 — 12 詞生成畫面未顯示草稿地址 — **仍開**

INDEX Decided：Generate 12-word 須**同時顯示草稿地址與 12 個詞**；reroll 兩者皆換。

現況：`GenerateSeedScreen` 只顯示 `seedGenerateDraft` 字串，Commit 前沒有依詞組衍生並顯示對應 Solana 地址（`deriveDefaultSeedAccount`／`BurnerKeys.addressOf` 可在 UI 層呼叫）。手驗 2 與 demo 契約無法僅靠讀碼判定通過。

#### H3 — Watch 帳戶 MWA 失敗未使用 `mwa.watchAccount` — **仍開**

INDEX Signing：`watch` 為 active 時，`offer` 在進待批前失敗，並使用 `mwa.watchAccount`（`AppStrings` 三語已存在）。

現況：`WalletModel.offer` 在 `activeAccount()?.kind == Watch` 時只呼叫 `request.finish(ApprovalResult.Failed)`，**沒有** `model.error = t("mwa.watchAccount")` 或任何讓使用者看到三語文案的路徑；`grep` 顯示 `mwa.watchAccount` 僅定義於 `AppStrings.kt`。dApp 會收到 decline，但 INDEX 手驗 3「authorize 失敗並顯示 watch 錯誤、不開批核」在 app 內無對應呈現。

#### H4 — INDEX 明示的單元測試未達標 — **仍開**

INDEX Acceptance 單元測試段要求：

1. 刻意無效的 **12 詞** 列表須被 BIP39 checksum **拒絕**；
2. **secret import 錯誤長度** 須被拒絕；
3. （可選）公知 BIP39 測試向量下 phantom index 0 的穩定地址字串。

現況：

- `Bip39ChecksumTest` 只斷言 9 個詞的個數不是 12，**未呼叫** `Bip39.validateChecksum` 對一組 12 詞但 checksum 錯的助記詞。
- `SecretLengthTest` 只斷言 `ByteArray` 長度常數，**未測** `ImportSecretScreen`／`commitImportedSecret` 對非 32／64 解碼長度的拒絕行為。
- 無 `VaultAccounts` legacy `{seed}` → `accounts`、移除 `seed` 鍵的單測（how Track A acceptance 亦要求）。

父代理 17 tests 通過，但上述契約測試缺口仍屬驗收未滿。

### MEDIUM

| ID | 狀態 | 說明 |
|----|------|------|
| M1 | 仍開 | INDEX Acceptance 三任務含 `:app:lintDebug`。本輪僅確認 `assembleDebug` 與 `testDebugUnitTest`；lint 未在本審查輪次執行或回報。 |
| M2 | 仍開 | `ImportMnemonicScreen` checksum 失敗時 Commit 無反應（silent return），沒有短句錯誤提示；不違 GUIDELINES，但手驗時難辨是按鈕失效還是詞不合法。 |
| M3 | 記錄 | `AccountScreens.kt` 重複 `import renameAccount`（編譯仍過）；`commitGenerate` 在非空 `accountRows` 分支用硬編碼 label `"Burner"` 而非 `defaultLabel`。 |
| M4 | 記錄 | Home pill 圖示固定 `VpnKey`；active 為 watch 時列表用 eye，pill 未跟 kind——INDEX 未強制 pill 圖示，列為 LOW 亦可，此處記錄即可。 |

### LOW

| ID | 狀態 | 說明 |
|----|------|------|
| L1 | 記錄 | 12 詞生成無 copy 控制，符合 INDEX。`WalletApp` 的 `CopyButton` 未接在生成畫面。 |
| L2 | 記錄 | 新／拆出 `.kt` 最長 `SettingsScreens.kt` 369 行、`WalletModel.kt` 347 行，皆 ≤400；`WalletModel` 衍生邏輯已外移到 `vault/` 與 `WalletAccountOps.kt`。 |

## 與 how.md

| Track | 結果 |
|-------|------|
| A Vault、`VaultAccounts`、遷移、`activeAccountId`、`applyPlain` 失敗走 `password.wrong`、遷移後 `persistVault` re-seal | **大部分有**。缺 Track A 單測（見 H4）。 |
| B 列表、rename／delete、active 樣式、pill／menu `push(Accounts)`、選列 `setActiveAccountId`＋`popToHome`＋`syncHoldings(false)` | **有**。刪最後一個 signing 帳戶 `removeAccount` 回 false。 |
| C 生成／匯入／burner／watch／`CombinedLater`、draft 在 `lock` 清掉、`offer` watch 拒批 | **部分**。burner／watch／combined 占位、checksum 匯入、secret 長度在 UI 層有 32／64 處理；**缺** H1／H2／H3。 |
| File shape | **符合**（見 L2）。`DOMAIN.md` 已含四種 kind。 |

## 驗收對照

| 驗收句 | 結果 | 證據 |
|--------|------|------|
| `assembleDebug` | 通過（父代理回報） | 17 unit tests |
| `testDebugUnitTest` | 通過（父代理回報） | 含 `Bip39ChecksumTest`、`SecretLengthTest`，但見 H4 內容不足 |
| `lintDebug` | **未確認** | 見 M1 |
| 手測 1：0.3.0 vault unlock 為一 burner、Assets 仍載入 | 手驗未走 | `VaultAccounts.parse` legacy `{seed}`→burner、`migrated`+`persistVault` 讀碼符合 |
| 手測 2：12 詞生成 reroll commit；lock 後列表有址；logcat 無詞 | 手驗未走；**H2** | 無 log 語句輸出助記詞；生成缺地址預覽 |
| 手測 3：watch 匯入；MWA authorize watch 錯誤、不開批核 | 手驗未走；**H3** | `offer` 不設 `pending`；無 `mwa.watchAccount` UI |
| 手測 4：切回 burner；資產跟址；合併列不寫入 | 手驗未走 | `setActiveAccountId`+`holdings.clear`；`CombinedLaterScreen` 僅文案 |
| 手測 5：殺 process 後列表與 active id；待批不復活 | 手驗未走 | 帳戶在 vault；待批無持久化 |
| BIP39 checksum 拒絕無效 12 詞 | **未達** | H4 |
| Secret 錯誤長度拒絕 | **未達** | H4 |
| 可選公知 phantom 地址向量 | 無 | 可選，不單獨開 HIGH |

## 測試結果（本輪記錄）

父代理回報（工作目錄 `android/`）：

- `:app:testDebugUnitTest` — 通過，**17** tests
- `:app:assembleDebug` — 通過
- `:app:lintDebug` — **未回報**
- 模擬器 INDEX 五步 — **未跑**

## 修復追蹤

| ID | 級 | 狀態 | 說明 |
|----|----|------|------|
| H1 | HIGH | 仍開 | 匯入助記詞：路徑種類 + 索引 0…4 預覽與選取 |
| H2 | HIGH | 仍開 | 12 詞生成畫面顯示草稿地址 |
| H3 | HIGH | 仍開 | watch active 時 MWA 失敗呈現 `mwa.watchAccount` |
| H4 | HIGH | 仍開 | 補齊 INDEX／how 要求的單元測試（checksum、secret 長度、遷移） |
| M1 | MEDIUM | 仍開 | 跑並記錄 `lintDebug` |
| M2 | MEDIUM | 仍開 | 匯入 checksum 失敗可選短句提示 |
| M3 | MEDIUM | 記錄 | 重複 import、硬編碼 Burner label |
| M4 | MEDIUM | 記錄 | pill 圖示未隨 watch |
| L1 | LOW | 記錄 | 生成無 clipboard |
| L2 | LOW | 記錄 | 行數與檔案形狀 |

## 歷審摘要

| 輪次 | 日期 | 結論 |
|------|------|------|
| 初審 | 2026-10-25 | **審查門檻未通過**。未關閉 HIGH：H1–H4。Gradle 兩任務通過；lint 與手驗未完成或未回報。 |

## Verdict

**reject** — 契約缺口（H1–H4）須在 follow-up Track 修復並複審後，才可標為 accept。
