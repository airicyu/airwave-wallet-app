# 0.5.0 — MWA 連線、簽訊息、簽交易

- **狀態：** `in progress`
- **Changelog：** [`changelog.md`](../../../changelog.md) 標題 `0.5.0 (unreleased)`
- **上一版：** [0.4.0](../0.4.0/INDEX.md)
- **對齊現況：** 對準 0.4.0 之後的樹。`LocalAssociation` 已接 `authorize`／`sign_messages`／`sign_and_send_transactions`；`sign_transactions` 立刻 decline。核准畫面已換 `ui/MwaScreens.kt`。手測客戶端是官方 Solana Mobile playground，本倉庫不放測試 dApp。
- **Design demo：** [`wallet-overall-v1.html`](../../design-demos/wallet-overall-v1.html) 畫面 `connect-dapp`、`approve-sign-tx`。本版**不**出貨 `approve-sign-tx-fail` 的模擬失敗框，也**不**出貨即時 AI 評估。簽署訊息沒有獨立 demo 畫面，版面寫在已定案。

## 產品

把 Mobile Wallet Adapter 的使用者畫面換成 overall-v1 電話框。dApp 可 **連線**、**簽署訊息**、**簽署並送出一筆交易**。手測客戶端是官方 Solana Mobile playground（https://docs.solanamobile.com/cli/playground）。主機 `bunx solana-mobile@latest playground`（`adb` 須在 PATH）。CLI 做 reverse，模擬器 Chrome 開 `http://localhost:4747`。不要用 `10.0.2.2`。協定仍是 0.1.0 的 MWA wallet endpoint，不是 Chrome extension Wallet Standard。

## 閱讀順序

1. 本檔
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)
4. [0.1.0 workflow MWA 節](../0.1.0/docs/workflow.md)（方法集合與待批生命週期；畫面以本檔為準）
5. [0.2.0 INDEX MWA submit RPC](../0.2.0/INDEX.md)
6. 程式：`LocalAssociation.kt`、`WalletModel` 的 `offer`／`approve`／`reject`／`lock`

## 已定案

| 主題 | 決定 |
|------|------|
| 協定 | 延續 0.1.0：`solana-wallet:` 本機 associate + localhost WebSocket。方法：`authorize`、`deauthorize`、`get_capabilities`、`sign_messages`、`sign_and_send_transactions`。`sign_transactions` 仍立刻 decline，且不列入 `get_capabilities`。不做 SIWS、remote／QR、`clone_authorization`、自訂 deeplink |
| 簽署交易 | 產品語「簽交易」＝ **`sign_and_send_transactions`**（錢包適配器 `sendTransaction`）。不是 deprecated 的 `sign_transactions`。playground 的 Sign and send 就是這條 |
| 帳戶 | 簽名與連線回傳的地址是 `activeAccountId` 對應的可簽名帳戶。`watch`：`offer` 前失敗，字串 `mwa.watchAccount`，不開 approval（0.4.0） |
| 待批 | 最多一筆，只在行程記憶體。第二筆立刻失敗且不取代第一筆。`lock` 與行程死亡讓進行中的請求失敗。結果只回該次請求。解鎖中若有 pending，先 Unlock 再進 approval |
| Submit RPC | 延續 0.2.0：**request chain 決定叢集**。該叢集允許的自訂 RPC，否則公開預設。Mainnet 無 active → 簽名前失敗。未知 chain → 簽名前失敗。Network `cluster` 不得改送錯鏈 |
| 手測客戶端 | 官方 playground，不進本倉庫。主機：倉庫根 `bun run playground`。裝置瀏覽器 `http://localhost:4747`。Connect、Sign message、Sign and send 進 Airwave。SIWS／Sign in 本版不實作，playground 若按該鈕應失敗且不開錯誤核准面。`http://10.0.2.2:…` 不是 secure context。不是 extension 的 `test-web`。本版刪除 `mwa-test-web/` |
| 連線畫面 | 電話框 `connect-dapp`。全螢幕。頂列三格：左 44dp **返回**（glyph `ic_stroke_back`，`d` 抄 demo `M15 18l-6-6 6-6`，accessible `menu.back`），按下＝`reject()`，不開返回堆疊；中標題 `mwa.connect.title`；右 44dp **Lock**（必有，不是可選）。Lock glyph `ic_stroke_lock`，`d` 抄 extension `StrokeIcon` 的 `lock`；accessible 與可見字用既有 `menu.lock`。行為同 0.1.0：清 session 並失敗該請求。中段順序：dApp 字母卡（identity 名稱第一個可見字元，空則 `A`）、名稱、URI、中間軌道、作用中帳戶列。帳戶列：36dp 圓 + `ic_stroke_key`（demo 鑰匙：`circle cx=9 cy=15 r=4` 與 `M12 13l7-7`、`M16 6h3v3`）+ 帳戶 label（空則只顯示短地址）+ `BurnerKeys.short`。底欄 sticky：次鈕 `mwa.reject`、主鈕 `mwa.connect.action`（連線，不是批准） |
| 簽署訊息畫面 | 無獨立 demo。全螢幕。頂列與連線相同（返回＝拒絕；右側必有 Lock），標題改 `mwa.signMessage.title`。中段：站點列（`mwa.site` + identity URI，URI 空則名稱）、帳戶短地址、訊息本文。只處理 `payloads` 的第一筆。payload 以 bytes 保存；能解成 UTF-8 則顯示該字串；否則顯示 `mwa.message.hex` 與小寫 hex，不要 `decodeToString()` 當唯一路徑。無 AI 卡。底欄 sticky：`mwa.reject`｜`mwa.approve` |
| 簽署並送出畫面 | 電話框 `approve-sign-tx` 骨架，另加頂列右側 Lock（覆寫 demo 空頂列，讓三個核准面都能鎖）。不含即時 AI、不含模擬失敗框。**不出貨**預期區重新整理鈕。**不出貨**第二 mint／USDC 列與任意多列模擬。頂列：左帳戶 pill（`ic_stroke_key` + label／短地址，**不可**切帳戶）＋右 Lock（同連線）。站點列 `mwa.site` + URI。標題 `mwa.signTx.title`。AI 卡一列：`ic_stroke_ai`（demo bot path）、`mwa.ai.label`、`mwa.ai.unknown`、右 `ic_stroke_report`（demo 文件 path）。報告鈕可見，按下無 overlay、不發網路。預期區：標題 `mwa.expected`，**沒有** refresh。系統 Transfer **只一列**原生 SOL：盾僅當 Jupiter 已驗證（沿 0.3.0 `ic_token_trust_verified`）；圖用既有 SOL 點陣，不要字母圓；符號 `SOL`；金額 `formatBaseUnits`，lamports `BigInteger`，支出紅、收入綠。無法解析成單筆 Transfer：一句 `mwa.expected.unavailable`，指令名在摺疊列 `mwa.details`。交易費列 `mwa.fee` + SOL 顯示，來源 `estimateFeeLamports`。底欄 sticky：`mwa.reject`｜`mwa.approve`。批准後才簽名並送出 |
| 文案鍵（en／zh-Hant／zh-Hans） | `mwa.connect.title` Connect／連線／连线。`mwa.connect.action` Connect／連線／连线。`mwa.signMessage.title` Sign message／簽署訊息／签署讯息。`mwa.signTx.title` Sign transaction／簽署交易／签署交易。`mwa.reject` Reject／拒絕／拒绝。`mwa.approve` Approve／批准／批准。`mwa.site` Site／站點／站点。`mwa.ai.label` AI analysis／AI 分析／AI 分析。`mwa.ai.unknown` Risk: unknown／風險指數: 未知／风险指数: 未知。`mwa.expected` Expected changes／預期變動／预期变动。`mwa.expected.unavailable` Balance changes unavailable／無法預覽餘額變動／无法预览余额变动。`mwa.details` Transaction details · instructions／交易明細 · 指令／交易明细 · 指令。`mwa.fee` Network fee／交易費／交易费。`mwa.message.hex` Binary message／二進位訊息／二进制讯息。`menu.back`／`menu.lock` 沿用 0.2.0 |
| Glyph | 本版新增 vector XML，`d` 抄電話框或 web `StrokeIcon`，禁止 Material Icons、禁止 Compose `Canvas` 發明、禁止 PNG 當這些 stroke：`ic_stroke_back`、`ic_stroke_lock`、`ic_stroke_key`、`ic_stroke_ai`、`ic_stroke_report`。盾沿用 `ic_token_trust_verified`。SOL 圖沿用 0.3.0 點陣 |
| 簽名前失敗 | 未知 chain、Mainnet 無 RPC、**sign-and-send 的 payload 空**、Transfer 摘要缺目的地／金額／手續費：不開 approval，dApp 收到 decline。**sign_messages 的 payloads 空或全空 bytes：同樣不 `offer`，立刻 decline。** 不畫 `approve-sign-tx-fail` |
| 數字 | 金額與手續費不用 `double`。顯示走 0.3.0 的 `formatBaseUnits` |
| 依賴 | 不加新套件。MWA 仍用既有 walletlib |
| MWA Activity | 對齊官方 FakeWallet 整段 session（見 how Track B 表）：獨立 `MwaActivity`、`singleTask`、`taskAffinity=com.airwave.wallet.mwa`、不透明。等待時品牌畫面，不是空白黑底。批准後 chrome 留到 teardown。`completeWith*` 不立刻 `finish()`。serving complete 在 main 關 Scenario；teardown 才回 dApp。靜默 reauthorize 不 `finish()`。不要抬 `MainActivity` Home。`WalletModel` 經 `AirwaveApp` 共用 |
| 檔案形狀 | 新畫面檔在 `ui/`（例如 `MwaScreens.kt`）。`WalletApp.kt` 的 Approval 仍呼叫新檔。`LocalAssociation` 可拆 pending 組裝，但待批仍只經 `WalletModel.offer`。新 `.kt` 程式列 ≤ 400（空行與註解不算）。`AppStrings.kt` 可超過 |
| 0.4.0 功能 | 多帳、holdings、選單、設定維持。本版只換 MWA 畫面與手測頁 |

## 實作前仍待決定

（空）

## 本版不做

| 項目 | 去處 |
|------|------|
| `sign_transactions`（只簽不送） | 維持 0.1.0 decline。playground 若有只簽不送，同樣 decline |
| 即時 AI 評估、報告 overlay | 可選 AI；本版卡面固定「未知」 |
| `approve-sign-tx-fail` 模擬失敗框、BlockhashNotFound 文案 | 未開版 |
| 任意交易的餘額模擬／多 mint 預期變動 | 未開版。本版只保證系統 Transfer 一列 |
| Connected sites 清單、遠端 associate、SIWS | 選單列仍是 0.2.0 空殼 |
| Chrome extension test-web | 另一個協定 |

## 驗收

在 `android/`，`JAVA_HOME` 設為 Android Studio JBR（本機 `C:\Program Files\Android\Android Studio\jbr`）：

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

裝置：一台 Android 模擬器。倉庫根執行 `bun run playground`（會把 `platform-tools` 加進 PATH 再跑官方 playground）。模擬器開 `http://localhost:4747`（不要用 `10.0.2.2`；主機 Windows Chrome 不能完成本機 associate）。

1. 解鎖一個可簽名帳戶（非 watch）。Connect → 出現連線電話框（字母卡、名稱、URI、帳戶列、拒絕｜連線）。批准後頁面 log 出現 connected 與公鑰。
2. Playground Sign message → 簽署訊息畫面顯示該頁送來的訊息本文。批准後 playground／CLI 顯示成功。拒絕則只有該次失敗，帳戶不變。
3. 帳戶有少量 Devnet SOL。Playground Sign and send → 簽署交易畫面有站點、（系統 Transfer 時）SOL 預期變動、手續費、拒絕｜批准。批准後 playground／CLI 顯示交易簽名。未知 chain／無 RPC 的路徑維持簽名前失敗（可用既有單元測試覆蓋 RPC 解析，不必在頁面上造假 chain）。
4. 作用中改為 watch。Connect 失敗、不開 approval、畫面可見 `mwa.watchAccount`。
5. 三個核准面（連線、簽署訊息、簽署並送出）右上都有 Lock。在**簽署並送出**畫面按 Lock：該請求失敗，需重新解鎖，pending 不在。殺掉 app 後重新解鎖，沒有殘留 approval。
6. `sign_transactions` 仍立刻失敗（單元測試或 walletlib 路徑；測試頁不按此方法）。

單元測試：`offer` 在已有 pending 時拒絕第二筆；watch 在 pending 前失敗；`sign_transactions` decline。不要把助記詞或私鑰寫進測試。
