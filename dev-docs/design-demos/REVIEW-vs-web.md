# wallet-overall-v1 vs web wallet — review (2026-10-24; 0.57.0 pass 2026-11-06)

Sources: `captures/` + `DEMO-UI-NOTES.md` + `airwave-ext/.../popup/` (StrokeIcon, TokenTrust, RpcGuide, close-empty `#3ddc84`, base.css tokens).  
Design: `wallet-overall-v1.html` + `icons/`.

Method: fixed `class="screen on"`; headless Chrome phone crops; visual compare to captures 02–08, 12–16, 20, 22.

---

## A. Drift / should match web (not mobile justification)

### Fixed this pass

| # | Screen | Was wrong | Web does | Sev | Status |
|---|--------|-----------|----------|-----|--------|
| A1 | home / approval / accounts | Account pill used letter avatar (`D`/`C`) | `AccountKindMark` key / layers / eye + kind colors (`#D08B5B` / `#c084fc` / `#6CB4E8`) | high | **fixed** |
| A2 | home-assets | Hand-drawn / broken token art earlier; trust position once wrong | Real token PNGs; TokenTrust shield **left** of token icon (anti-scam) | high | **fixed** (logos + shield left) |
| A3 | home-assets-combined | ISC had empty trust; INF row missing | ISC verified shield (no score); INF + score 76 | med | **fixed** |
| A4 | home-assets-combined | Reclaim icon blue accent | `#3ddc84` (`.close-empty-btn.active`) | high | **fixed** (prior) |
| A5 | approve-sign-tx | Expected-change rows lacked TokenTrust | Shield left of token on deltas | high | **fixed** |
| A6 | approve / accounts / add / aggregate | Emoji chrome (`📄` `✎` `🗑` `🌱`…) | StrokeIcon paths | high | **fixed** |
| A7 | rpc-guide | Softened body copy; link once missing | Exact i18n `rpcGuide.body` + Helius link `https://www.helius.dev/` (no underline) | high | **fixed** |
| A8 | token-detail | Invented hero amt +「原生代幣 · Mainnet」+ primary full-width 送出 | Web: icon → name → ghost 送出; meta 數量 / 鑄造=原生 | high | **fixed** |
| A9 | home-activity | Kind/time stacked right column; oversized icons; no「活動」head | Web: kind+when same row; 22px overlapping token icons; section head | med | **fixed** |
| A10 | accounts | No address copy; active card no accent border | Copy next to pk; `.account-card.active` accent border | med | **fixed** |
| A11 | home | Account pill text contrast (button UA color) | `--text` on `.bar-wallet` | high | **fixed** (prior) |
| A12 | token rows | Icon 36px | Web token-icon 40px | low | **fixed** |

### Still open (A)

| # | Screen | What’s wrong | Web does | Sev |
|---|--------|--------------|----------|-----|
| A13 | approve-sign-tx | Site shown as plain「站點 `url`」line; AI denser disclosure is simplified | Site chip + copy; richer AI / CU / fee split sometimes visible | med |
| A14 | approve-sign-tx-fail | Layout OK but not pixel-matched to capture 13 density | Blockhash fail box + AI low + fee | med |
| A15 | connect-dapp | App mark is letter「J」card (OK-ish); rail simplified | Similar in demo; fine-tune spacing vs capture 03 | low |
| A16 | menu-sheet | Menu icon set close to StrokeIcon but not byte-identical weights/sizes | IconAccounts/Book/Network/Settings/Globe/Info/Lock at web sizes | low |
| A17 | settings-hub | Hub list once lagged web; **0.52.0** added Debug mode row | Web `SettingsHub` has locale/RPC/keys/txAi/CU/lock/**debug** | low — hub IA confirmed on web; summaries still mobile-flavored |
| A18 | send / send-pick | Mostly aligned; pick-row dots vs web radio cards | Capture 07/15 | low |
| A19 | network | Simple radio list vs web selectable cards with blue border | Capture 22 | low |
| A20 | onboarding | No FirstRunBrand wave logo block | Web RpcGuide/onboarding brand header | med |
| A21 | home-assets | No sidebar brand row「Airwave Wallet」(popup/sidebar chrome) | Extension-only; mobile has status bar instead | low — arguably B |
| A22 | accounts | Watch/signing icons sit without filled circle chrome sometimes used on web avatar | `paintAccountKindMark` uses `--fill` circle — we match; fine-tune size | low |
| A23 | combined expand | Share rows indent/`JCLh…` style approximate | Web nested share under expanded SOL | low |
| A24 | accounts subtree | Was only list＋chooser＋aggregate-create | Web full add-* / account-* stack | high | **fixed** (2026-10-25) |

---

## B. Intentional mobile differences (OK if Eric agrees)

| # | Diff | Why mobile |
|---|------|------------|
| B1 | Bottom tabs 資產｜活動 + home indicator | Already in web home; keep as native chrome |
| B2 | Overflow as **top sheet** (scrim + sheet) instead of extension dropdown | Thumb reach / full-bleed phone |
| B3 | Full-width 390 phone frame (not 422×600 popup) | Native target |
| B4 | Account pill as primary top nav (no extension「open sidebar / collapse」) | Web sidebar/popup shell icons don’t apply |
| B5 | ~~**Scan** icon~~ | **Dropped 2026-10-24** — Eric: mobile has no sidebar/popup; remove shell chrome |
| B6 | Approval as full-screen stack with sticky 拒絕｜批准 | Web sidebar height; mobile needs large sheet |
| B7 | Designer notes column + left screen nav | Design-demo chrome only, not product |
| B8 | Settings as hub list (if Eric wants) | Faster mobile IA — **needs confirm** (also C1) |

---

## C. Unclear / need Eric call

| # | Question |
|---|----------|
| C1 | Keep **settings-hub** as mobile IA, or mirror web’s flat overflow → each settings screen? |
| C2 | ~~Keep **scan** on Home?~~ → **drop** (Eric 2026-10-24) |
| C3 | Onboarding / rpc-guide: include **FirstRunBrand** logo block on mobile? |
| C4 | Approval: how much of AI summary / CU / fee breakdown must show above the fold vs folded? |
| C5 | Activity timestamps: full `YYYY-MM-DD HH:MM:SS` (now) vs compact — preference? |

---

## Top 8 A items to fix first

*(At review start; most are now fixed — remaining priority below.)*

1. ~~Account pill contrast / kind mark~~ → **done**
2. ~~TokenTrust left of token + real logos~~ → **done**
3. ~~Reclaim `#3ddc84`~~ → **done**
4. ~~RPC Helius link + exact body~~ → **done**
5. ~~Kill emoji chrome → StrokeIcon~~ → **done**
6. ~~Approval deltas TokenTrust~~ → **done**
7. ~~Token detail layout vs capture 14~~ → **done**
8. **Next:** A17/C1 settings IA; A13–A14 approval density; A20 onboarding brand; A16 menu icon size polish

---

## Counts

| Bucket | Fixed this pass | Still open |
|--------|-----------------|------------|
| A | 12 | ~11 (many low) |
| B | — | 8 listed |
| C | — | 5 calls |

Windows sync target (this pass):  
`C:\Users\airic\airwave\airwave-wallet-app\dev-docs\design-demos\wallet-overall-v1.html` + `icons\` + this `REVIEW-vs-web.md`.

---

## Changelog

- 2026-10-24：AI 分析對齊 web `tx-ai` 單列卡（bot＋標籤＋風險指數＋藍圓報告鈕；去掉卡內展開／雙行標題）。
- 2026-10-24：Eric 決定拿掉 Home 頂欄 scan／展開 icon（無 sidebar／popup）；`wallet-overall-v1.html` 已刪 3 處。
- **2026-10-24:** `home-assets-combined` expand／breakdown 對齊 web `HomeTokenList`（`row-expand-btn` + shortAddr＋％＋bar；非帳戶名橫列）。
- **2026-10-25:** Align overall-v1 to web **0.52.0** — add-account chooser confirmed icon＋subtitle; RPC Helius copy confirmed; added `settings-debug` + hub「Debug mode 設定」; READMEs note 0.52.0.
- **2026-11-06:** Align overall-v1 to web **0.57.0** — hub「預設優先費」; v1 fee card; Devnet strip + network mismatch; failure details 320px + copy; sign message and sign in; debug Mainnet blockhash RPC.
- **2026-10-25 (later):** Accounts subtree screens added to overall-v1 (`add-generate-seed` … `account-reveal-key`); aggregate create aligned to web manage layout (main-pick／grip／trash／+); accounts list shows combined card with switch.
  - Skipped: import-seed **pick-index** step (only words step); address-book (`address-cards*`) — not under Accounts priority; non-combined manage delete-confirm as its own screen (reveal covered).
