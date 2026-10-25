# 0.4.0 — reasoning

0.3.0 能為一個 burner 顯示代幣。Demo 的帳戶清單是下一刀，因為後面畫面（send、activity、combined）都需要作用中帳戶 id。

## 做法

Vault 檔仍是唯一密鑰存放處。既有密碼箱內的 JSON 長出帳戶清單。設定記住哪個 id 是作用中，因為那個 id 不是密鑰。舊 vault 在解鎖時遷移，讓 0.1.0 burner 繼續可用。

衍生用 classpath 上已有的 Bouncy Castle，本版不加 BIP39 套件。詞表檔是資料，不是第二套演算法。

Watch 帳戶是一等公民，不能簽名。在 pending approval 之前就讓 MWA 失敗，避免出現沒有金鑰卻有確認鈕的畫面。

Combined 列在 demo 的新增畫面裡。真正建立 combined 帳戶是 0.8.0。一列若寫入假帳戶，等於第二套產品。

## 捨棄

- **每個帳戶一個 vault 檔。** 兩個檔就是兩條 rehydrate 路徑。
- **把助記詞存進設定或 log。** 密文是唯一複本。
- **12 詞畫面上的複製鈕。** Web 產品省略複製，避免助記詞停在剪貼簿。
- **自訂衍生路徑文字。** 三個具名路徑涵蓋 Phantom、Solana CLI、change path。自由路徑以後再決定。
- **預覽 20 個索引。** 五個索引（0 到 4）塞得進一屏。更多索引不在本長任務。

## 依賴

不加新套件。
