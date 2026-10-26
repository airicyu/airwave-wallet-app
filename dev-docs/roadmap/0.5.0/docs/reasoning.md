# 0.5.0 — reasoning

0.1.0 已把 MWA wallet endpoint 接上。之後幾版改了帳戶與資產，批准畫面仍是鍵值清單，和 overall-v1 的連線／簽署交易電話框不一致。手測用官方 Solana Mobile playground 驗連線、簽訊息、簽交易，必須先把這三條使用者路徑寫成可對照的畫面契約。

## 做法

協定與待批規則不重開。本版只換 UI 組裝。手測客戶端用官方 playground（`authorize`、`signMessage`、`sendTransaction`／`sign_and_send_transactions`），不在倉庫內放第二套測試 dApp。

「簽交易」對齊測試頁與 spec 2.x 的強制能力 `solana:signAndSendTransaction`，而不是 deprecated 的 `sign_transactions`。後者繼續 decline，避免假裝有第二條產品流。

預期變動若要涵蓋任意指令，需要模擬與多 mint 解碼，那是另一版。本版系統 Transfer 已能從既有 `LegacyTransaction.summarize` 拿出目的地與 lamports，足夠對齊電話框的一列 SOL。

AI 卡若真的去打模型，會把可選、唯讀的 AI 做成主路徑。本版只擺與電話框同款的一列「未知」，報告鈕不開 overlay。

電話框預期區有 refresh 與 USDC 列。本版沒有模擬，故寫死不出貨，避免實作抄 HTML 偷渡。簽署交易頂列加 Lock，覆寫 demo 空頂列，讓三個核准面都能完成手驗 5。

## 捨棄

- **實作 `sign_transactions`。** Spec 標 deprecated；測試頁沒有只簽不送的按鈕。
- **出貨 `approve-sign-tx-fail`。** 簽名前失敗維持無畫面 decline，與 0.1.0／0.2.0 一致。
- **Live Jupiter／LLM 評估。** 違反「可選 AI 不進主路徑」，也還沒有評估契約。
- **在批准畫面切帳戶。** Pill 只展示。切帳會讓正在簽的密鑰與畫面上的地址分叉。
- **把 pending 寫進磁碟。** 架構禁區。
- **用桌面 Chrome 當 MWA 客戶端。** 本機 associate 過不了模擬器邊界。
- **本倉庫再養一套 Vite 測試頁。** playground 已涵蓋連線／簽訊息／簽並送出。

## 依賴

不加新套件。測試頁已在未歸版引入 Vite／wallet-adapter；本版只把它算進 0.5.0，不改 Android classpath。
