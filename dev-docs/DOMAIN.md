# Airwave Wallet App domain language

Shared across versions. INDEX, how-to docs, comments, and UI copy follow this file.
When a version's INDEX **explicitly overrides** a term, that version wins, and this file is updated to match.

Each entry has a **Chinese name** (normative), an **English name** (normative), and a **definition**.
**Aliases** (including short forms) are allowed but should be avoided. New docs use the full normative name. After the first mention, an alias listed on that entry may be used.

The Chrome extension has a larger DOMAIN file. Terms listed under **Not adopted** are not this app's model until an INDEX writes them here.

---

## Entry shape

```text
Chinese: …
English: …
Definition: …
Code: …            (storage, type, or field, when there is one)
Alias: …           (optional; short form or UI wording)
Do not use: …      (easy to confuse, or retired)
```

---

## Product

### Airwave

- **Chinese:** Airwave
- **English:** Airwave
- **Definition:** This product: the Airwave Solana wallet app. What an account list switches is a **錢包帳戶** (wallet account).
- **Alias:** 錢包產品, wallet app
- **Do not use:** the Chrome extension; "wallet" alone for an address

---

## Address

- **Chinese:** 地址
- **English:** address
- **Definition:** A Solana **public key**, base58. The industry terms **wallet address** and **錢包地址** mean this.
- **Code:** not a durable field name in this doc until an INDEX names storage.
- **Alias:** pubkey, 公鑰, 錢包地址, wallet address
- **Do not use:** as a wallet-account row

### Balance

- **Chinese:** 餘額
- **English:** balance
- **Definition:** A chain integer amount for one mint on one address, including native SOL. Not a `double`.
- **Code:** `BigInt` built from a string. The INDEX names the unit (lamports or token base units).

---

## Custody

### Vault

- **Chinese:** 保險庫
- **English:** vault
- **Definition:** Key material encrypted with the wallet password. It stays on the device. The app does not upload it.
- **Do not use:** a hardcoded password; plaintext keys in preferences, logs, or docs

### Wallet password

- **Chinese:** 錢包密碼
- **English:** wallet password
- **Definition:** The password that encrypts the app vault. It is not an on-chain password and not a per-account password. Forgetting it means the vault cannot be decrypted.
- **Do not use:** as a recovery phrase

### Wallet account

- **Chinese:** 錢包帳戶
- **English:** wallet account
- **Definition:** One on-device account the user treats as theirs. From 0.4.0 the vault holds a list of accounts; settings remember which id is **active**. Home assets and signing follow the active account. Kinds: **seed** (BIP39 mnemonic), **burner** (random 32-byte), **secret** (imported key), **watch** (address only, cannot sign).
- **Code:** Plaintext `accounts` array inside `airwave.vault.v1`; `activeAccountId` in `airwave.settings.v1`.
- **Alias:** account
- **Do not use:** combined wallet, member address, main wallet, connected site

### Pending approval

- **Chinese:** 待批
- **English:** pending approval
- **Definition:** An in-flight approve request held in memory. It is not written to disk. If the process dies, it fails.
- **Do not use:** a row in the durable store that the UI reloads in order to find the payload

---

## Not adopted

These extension terms are not this app's model. Do not implement them because the extension DOMAIN defines them. An INDEX that wants one must write the entry here first.

- Combined wallet account, member address, main wallet
- Connected site, Wallet Standard account
- Extension package (`wallet/` in the other repo)
