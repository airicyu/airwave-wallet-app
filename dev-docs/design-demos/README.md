# Airwave mobile design demos (overall pack)

Static **phone sketches** for the native Android wallet. This folder on the box holds the **overall v1** pack derived from Eric’s live **0.51 web UI** demo (screen captures + notes), plus a copy of the 0.2.0 chrome pattern for reference.

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
| Coverage | Settings-heavy | Home / Activity / Token / Send / Accounts / Aggregate / Approval / Onboarding |
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
- `../web-0.51-screen-inventory.md`
- `../wallet-020.html` (chrome pattern)

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
| `add-account` | 新增帳戶（5 種） |
| `aggregate-create` | 建立聚合錢包帳戶 |
| `network` | 網路 Devnet／Mainnet |
| `settings-hub` | 設定 hub |
| `connect-dapp` | 連線 jup.ag |
| `approve-sign-tx` | 簽署交易（預期變動優先） |
| `approve-sign-tx-fail` | 簽署交易 + BlockhashNotFound |
| `confirm-reclaim` | 確認收回 |

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

## Icons (web SSOT)

Chrome stroke icons are copied from extension `StrokeIcon.tsx` (kebab menu, copy, refresh, recycle, back, accounts, book, network, settings, globe, info, lock, external). Verified green shield matches `TokenTrust` in `HomeTokenList.tsx` (shield fill + check stroke `#0f1419`; score badge for non-native). Token logos are real PNGs under `icons/` (Solana token-list / Jupiter CDN / ISC meta / PYUSD hubspot) — **not** hand-drawn letter circles. Shield stays **left of** the token icon (anti-scam).
