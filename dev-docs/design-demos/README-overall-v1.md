# Airwave mobile design demos (overall pack)

Static **phone sketches** for the native Android wallet. This folder holds the **overall v1** pack aligned to web wallet **0.52.0** (add-account chooser rows, RPC Helius copy, Debug mode settings), derived from Eric’s live web UI demo + notes, plus a copy of the 0.2.0 chrome pattern for Settings depth.

## Files

| File | Role |
|------|------|
| [`wallet-overall-v1.html`](./wallet-overall-v1.html) | **Primary** multi-screen mobile-native demo (繁中 labels from Eric’s demo). |
| `../wallet-020.html` | 0.2.0 Settings / Unlock / menu chrome contract (English). Keep for Settings depth. |

## How this pack differs from `wallet-020.html`

| | **wallet-020** | **wallet-overall-v1** |
|--|----------------|------------------------|
| Source of truth | Extension IA + early empty Home | Eric ORIGINAL demo captures + `DEMO-UI-NOTES.md` |
| Labels | Mostly English | **繁體中文** (demo UI strings) |
| Home | Empty assets placeholder | Rich token lists (demo / combined + expandable shares) |
| Coverage | Settings-heavy | Home / Activity / Token / Send / Accounts subtree / Aggregate / Approval / Onboarding |
| Settings | Full hub + children | Hub only (children stay in 020) |

**Do not** treat extension `dev-docs/design-demos/` as authoritative layout.

## How to open

1. Open `wallet-overall-v1.html` in a desktop browser (Chrome/Firefox).
2. Use the **left nav** to switch screens; the **phone frame** (~390×780) updates.
3. Right panel = **Designer notes** (Ship / Do not ship / Mobile) — **not** shipped copy.

Authoritative inputs used to build this pack:

- `../demo/DEMO-UI-NOTES.md`
- `../captures/` (+ INDEX)
- `../design-principles.md`
- `../web-0.51-screen-inventory.md` (baseline inventory; product deltas through **0.52.0**)
- Web `dev-docs/design-demos/0.52.0/add-account-chooser-ux.html` + `wallet/src` i18n / settings
- `../wallet-020.html` (chrome pattern / Settings depth)

## Screen checklist (`wallet-overall-v1.html`)

| id | Screen |
|----|--------|
| `onboarding-language` | Choose your language |
| `onboarding-password` | 設定鎖定密碼 |
| `rpc-guide` | 設定 Mainnet RPC |
| `home-assets` | Home 資產 (demo) |
| `home-assets-combined` | Home 資產 (combined + expand) |
| `home-activity` | Home 活動 |
| `menu-sheet` | Menu top sheet（7 項） |
| `token-detail` | Token 詳情 Solana |
| `send` | 送出 SOL（50%／全部） |
| `send-pick-address` | 選擇地址（錢包｜地址簿） |
| `accounts` | 帳戶列表 |
| `add-account` | 新增帳戶 chooser（5 種） |
| `add-generate-seed` | 建立助記詞錢包（draft＋骰子） |
| `add-generate` | 建立拋棄式錢包 |
| `add-import` | 匯入子選單（標題＋chevron） |
| `add-import-seed` | 匯入助記詞 |
| `add-import-secret` | 匯入密鑰 |
| `add-watch` | 觀察帳戶 |
| `add-combined` | 建立聚合（chooser 入口） |
| `add-combined-add` | 聚合 — 加入成員 |
| `add-combined-paste` | 聚合 — 貼上地址 |
| `add-combined-local` | 聚合 — 本機帳戶 |
| `account-rename` | 重新命名帳戶 |
| `account-switch` | 切換主錢包 |
| `account-manage` | 管理聚合 |
| `account-manage-add` | 管理 — 加入成員 |
| `account-manage-paste` | 管理 — 貼上地址 |
| `account-manage-local` | 管理 — 本機帳戶 |
| `account-member-rename` | 成員重新命名 |
| `account-reveal-key` | 顯示私鑰 |
| `aggregate-create` | 建立聚合（與 add-combined 同布局） |
| `network` | 網路 Devnet／Mainnet |
| `settings-hub` | 設定 hub（含 Debug mode 列） |
| `settings-debug` | Debug mode 設定（Jupiter／OpenRouter URL） |
| `connect-dapp` | 連線 jup.ag |
| `approve-sign-tx` | 簽署交易（預期變動優先） |
| `approve-sign-tx-fail` | 簽署交易 + BlockhashNotFound |
| `confirm-reclaim` | 確認收回 |

## 0.52.0 alignment

- **Add-account chooser:** five icon＋title＋subtitle rows; **Accounts subtree** now includes generate／import／watch／combined create＋member flows, rename／switch／manage／reveal (web route ids). Import *sub*-chooser = title＋chevron only.
- **RPC guide:** Helius link = 「向 Helius 申請 RPC endpoint」— no 「免費」／「申請 API key」.
- **Debug mode settings:** hub row + detail screen; default off in product; demo shows on so URL override fields are visible.

## Visual / UX contract (summary)

- Tokens: `#0F1419` / `#161C24` / `#1E2630` / `#2A3542` / `#E8EEF4` / `#9AA7B4` / accent `#3D9CF0` / danger / ok / warn.
- Mobile chrome: status safe area, **bottom tabs**, full-width **menu top sheet**, Back ≥44dp on secondary.
- Approval = full-screen; expected deltas first; AI collapsed; sticky 拒絕｜批准.
- Account pill = primary switcher; combined assets expandable; Devnet banner + Mainnet RPC guide kept.
- **No** real seed phrases or API key plaintext in demos.

## Workflow

See parent [`../design-demos-README.md`](../design-demos-README.md) and [`../design-principles.md`](../design-principles.md). Update this HTML before Compose when chrome or IA for these screens changes.

## Design constraints
- **Trust shield (Jupiter / verified):** always **left of the token icon** (match web). Never beside the token name — scam tokens can put a shield emoji in the symbol/name to look verified （Eric 2026-10-24）。
