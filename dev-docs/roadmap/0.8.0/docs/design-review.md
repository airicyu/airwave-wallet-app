# Design review — 0.8.0 Airwave Wallet App

- Date: 2026-11-11 (Asia/Hong_Kong)
- Round: **2** (initial round kept below; statuses in the fix tracker are current)
- Role: design review (does not edit INDEX / HOW / reasoning / HANDOFF / code; this file is not the contract)
- Baseline: decided sentences and acceptance in [`../INDEX.md`](../INDEX.md); [`how.md`](how.md); [`reasoning.md`](reasoning.md); [`../HANDOFF.md`](../HANDOFF.md)
- Architecture bans: [`../../GUIDELINES.md`](../../GUIDELINES.md) section 4. Pending approvals, request results, the password-boxed vault, and `BigInt` amounts are untouched. The create draft stays in memory on `WalletModel`. The durable row is the existing vault. No second rehydrate path is specified.
- Domain: [`../../../DOMAIN.md`](../../../DOMAIN.md). Combined wallet account is an entry. "Main wallet" stays under Not adopted.
- Previous: [`../../0.7.0/INDEX.md`](../../0.7.0/INDEX.md) (`shipped`, MWA sign transaction). No overlap with the combined-account row. The combined row is not an MWA signer.
- Phone frames in [`../../../design-demos/wallet-overall-v1.html`](../../../design-demos/wallet-overall-v1.html): `add-combined`, `add-combined-add`, `add-combined-paste`, `add-combined-local`, `account-manage`, `account-combined-manage`. Frames the INDEX does not name (`account-manage-add`, `account-manage-paste`, `account-manage-local`, `account-switch`, `account-member-rename`) are not the contract. The INDEX explicitly does not ship the frame pencil, the frame word 主錢包, or the eye on the current-wallet card.
- **Verdict:** no open HIGH. The design gate passes. The proposal is feasible. M7 and M8 should still be written into the contract before Track A so the account name and the manage-path local picker are not guessed.

## Round 2

Compared the current INDEX, HOW, reasoning, HANDOFF, and DOMAIN with the initial findings. An old ID is closed only where that lock is now a sentence in one of those files.

### Closed this round

| ID | Where the lock is written |
|----|---------------------------|
| H1 | INDEX Create, Add member, Manage menu, Member sheet, and Copy: block order, three-locale titles and control labels, and the glyph list (back chevron, copy, plus, grip, check, key, trash). The eye is named as not shipped. Acceptance looks for 聚合錢包帳戶, 管理帳戶, and 管理 |
| H2 | INDEX Create and Member sheet: no rename pencil. Not this version: the frame pencil is not drawn. HANDOFF Track A repeats it |
| H3 | INDEX Domain and Copy: card label is `accounts.currentWallet` (Current wallet / 目前錢包 / 当前钱包). The frame word 主錢包 is not shipped. DOMAIN keeps "Main wallet" under Not adopted |
| H4 | INDEX Current wallet: first appended member, check tap, trash reassignment, empty create draft, refused empty manage trash |
| H5 | INDEX Home and HANDOFF Product: active `combined` Home shows that row's current-wallet address only, on the existing single-account Home, with no query or sum of other members, and the row still cannot sign |
| H6 | INDEX Domain, and DOMAIN: combined wallet account is an entry; "Combined wallet account" and "member address" are gone from Not adopted; "Main wallet" stays |
| M1 | INDEX Add member and HOW Track A: the member sheet uses `add-combined-add`, `add-combined-paste`, and `add-combined-local`; Confirm appends to the vault row and returns to `account-combined-manage`; Back does not write |
| M2 | INDEX Add member and HOW: split on newlines, trim, first-seen order, skip blanks, invalid public keys, and addresses already in `members` |
| M3 | INDEX Acceptance: one Android emulator, the three titles above, and process death on the create screen writes no row |
| M4 | INDEX Draft and HOW: `WalletModel` holds the draft; Back and process death discard it and write no row; the member sheet does not use the draft |
| M5 | HOW heading `Track A — vault row and screens`. HANDOFF has a done check and a paste-ready starter |
| M6 | INDEX Create and Copy: `accounts.removeMemberTitle`, `accounts.removeMemberBody`, `accounts.removeMemberConfirm` in en / zh-Hant / zh-Hans. Account-row Remove still has no second dialog |
| L1 | INDEX Create, Member sheet, and Not this version: the grip is visible and does not reorder |
| L3 | INDEX Row and Add member: `members` is ordered; paste and local Confirm append in first-seen order |

### Still open

#### L2 — INDEX status is `in progress` during design review — **open**

Unchanged. Status is still `in progress` while this gate is the design review and the chooser still has the unavailable line until Track A. Non-blocking.

### New findings

#### M7 — The create name is on screen and never stored — **open**

`add-combined` has a name field (`accounts.label`, placeholder `common.optional`). `account-manage` shows "the account name". HOW's vault JSON is only `kind`, `address`, and `members`. Nothing says the name is written on the row, or what the list and the manage menu show when the field is left blank.

Lock: Create stores the trimmed name on the same vault row as other accounts. A blank name stores an empty name and the row still lists. The manage menu shows that stored name.

#### M8 — The local picker disables against the draft only — **open**

Paste skips addresses already in `members` on both paths. The local row is "a disabled row already in the draft". On the member sheet there is no draft, so that sentence does not disable addresses already on the vault row.

Lock: a local row is disabled with `accounts.alreadyMember` when the address is already in the create draft, or, on the member sheet, already in that vault row's `members`.

#### M9 — Acceptance never opens Home — **open**

Decided Home is current-wallet assets only. Items 1 and 2 never make the combined row active or look at Home.

Lock: one acceptance step switches to the new row and checks that Home shows that current-wallet address's assets and does not list another member's balance.

### Load-bearing decisions

Reasoning names the web combined-account edit as the UI source: local members stay staged until Confirm, overflow opens manage or remove, Home does not sum, the row cannot sign. Dropped behavior is in INDEX Not this version (merged Home balance, drag reorder, rename, signing / MWA). Alternatives are not argued at length; the dropped set is written.

### Architecture

No GUIDELINES section 4 violation. The create draft is memory on `WalletModel` and dies with the process. Member edits write the vault row. Custody stays the password-boxed vault with `canSign()` false and no seed. No request broadcast, no new package, no `double` amounts, no AI.

### Acceptance cross-check

| Acceptance sentence | Testable | Gap |
|---------------------|----------|-----|
| Gradle assemble and unit test from `android/` with Android Studio JBR | Yes | No named unit test for `kind: combined` |
| Emulator; process death on create writes no row | Yes | — |
| Create screen title, local toggle, Back, Confirm, first member is current wallet, `canSign()` false, `members` includes `address` | Yes | Name persistence is M7 |
| Manage title, remove without a second dialog, member accounts remain, manage Add Confirm writes, Back does not, last-member trash refused | Yes | Local disable set on that path is M8 |
| Home shows only the current-wallet assets | Not in the list | M9 |

### Waiver

None.

### Verdict: proceed

No open HIGH. M7 and M8 should be one sentence each in INDEX and HOW before implementation. M9 should be one acceptance step. The proposal is feasible either way: a non-signing vault row, an in-memory create draft, Home limited to the current-wallet address, and no MWA answer from this row.

## Findings (initial round)

Closed = written into INDEX / HOW / HANDOFF. Open = the contract still diverges. Non-blocking = record only.

The **open** marks in this section are the initial-round text. Current status is the fix tracker and Round 2.

### HIGH

#### H1 — Six phone frames are not copied into Decided — **open**

GUIDELINES Screen spec: linking the HTML is not a spec. Decided must state block order, titles and subtitles in the three locales, and glyphs (vector XML from the frame paths, not Material icons). Decided names row order for create and two menu labels. It does not state the screen titles, the create CTA, the add-chooser labels, the paste field, the local-picker row text, or any glyph.

The frames show:

| Frame | Blocks in order |
|-------|-----------------|
| `add-combined` | Back, title 聚合錢包帳戶, name field 名稱 (placeholder 選填), current-wallet card, heading 成員 plus add, member rows, primary 建立聚合錢包帳戶 |
| `add-combined-add` | Back, title 加入, row 地址, row 本機帳戶 |
| `add-combined-paste` | Back, title 地址, field 地址 (placeholder 地址，或貼上多行), primary 確認 |
| `add-combined-local` | Back, title 本機帳戶, rows `name · short address`, disabled row suffix 已加入, primary 確認 |
| `account-manage` | Back, title 管理帳戶, account name, current-wallet short address, row 管理聚合錢包帳戶, danger row 移除聚合錢包帳戶 |
| `account-combined-manage` | Back, title 管理, current-wallet card, heading 成員 plus add, member rows. No name field and no create button |

Glyphs the frames draw and Decided does not name: back chevron, eye on the current-wallet card, copy, plus, grip, check, key mark on a signing member, empty kind slot on a non-signing member, trash.

Lock in Decided, in en / zh-Hant / zh-Hans, the titles and control labels above, and name each glyph as a StrokeIcon path copied from that frame. Acceptance must look for those titles on a device.

#### H2 — Rename pencil is on the frame and forbidden by Not this version — **open**

`add-combined` and `account-combined-manage` draw a rename control on the non-local member (the watch row). Decided says renaming a member that is not a local account is not this version, and the member-row sentence lists grip, check, kind mark, name, short address, copy, and trash, with no rename control.

An implementer cannot both copy the frame and obey the non-goal.

Lock: the rename control is not drawn. The frame pencil is not shipped. Renaming any member is not this version.

#### H3 — Current-wallet card label contradicts the frame — **open**

Decided sets `accounts.currentWallet` to Current wallet / 目前錢包 / 当前钱包. Both named frames label that card 主錢包. DOMAIN still lists "main wallet" under Not adopted.

Lock: the card label is `accounts.currentWallet` (Current wallet / 目前錢包 / 当前钱包). The frame text 主錢包 is not shipped. English "main wallet" stays out of the model.

#### H4 — Current wallet and trash break the row invariant — **open**

Decided: `members` includes `address`. Create may remove the last member, and Create is enabled only when at least one member exists. The check is drawn. Nothing says which member is the current wallet when the first address is appended, what a check tap writes, or what `address` becomes when that member is trashed. On the manage sheet, trash writes the vault immediately, including a trash that would leave `members` empty or leave `address` outside `members`.

Acceptance requires the created row's address to be the current wallet. That address is not defined.

Lock:

- The first member appended to the create draft becomes the current wallet (`address`).
- Tapping the check on another member sets `address` to that member. On create this updates the draft only. On manage it writes the vault row in the same save.
- Trash of the current-wallet member, while another member remains, sets `address` to the first remaining member.
- On create, trash of the last member clears `address` and leaves Create disabled.
- On manage, trash that would leave `members` empty is refused and the row stays.

#### H5 — Home when the combined row is active is undefined — **open**

The row is an account in the list, so the user can make it active. Decided: Home keeps showing the active account's own assets, and a merged Home balance is not this version. A combined row has no assets of its own. "Own assets" and "do not sum members" do not say what Home draws.

Lock: when the active account is `combined`, Home shows the assets of that row's current-wallet address only, using the existing single-account Home. It does not query or sum the other members. The row still cannot sign.

#### H6 — DOMAIN still lists the term under Not adopted — **open**

DOMAIN defines combined wallet account for 0.8.0 and also lists "Combined wallet account, member address, main wallet" under Not adopted. The standing rule is that a Not adopted term is not this app's model. The INDEX uses the term and does not say to remove that bullet.

Lock in the INDEX read order: this version removes "Combined wallet account" and "member address" from Not adopted. "Main wallet" stays Not adopted; the field name is current wallet (H3).

### MEDIUM

| ID | Status | Basis |
|----|--------|-------|
| M1 | **open** | Manage Add is decided ("Add, trash, and the current-wallet check write the vault row") but the destination screen is not. The INDEX does not name `account-manage-*`. Those frames also differ from the create frames (manage local has no Confirm). Lock: manage Add opens the same `add-combined-add`, `add-combined-paste`, and `add-combined-local` screens; Confirm appends to the vault row and returns to `account-combined-manage`; Back does not write. |
| M2 | **open** | "Parsed addresses" has no rule for a blank field, a line that is not a Solana address, or a duplicate inside the paste. Lock: split on newlines, trim, keep first-seen order, skip blanks, skip lines that are not valid public keys, skip addresses already in `members`, then append the rest. |
| M3 | **open** | Acceptance can be performed, and it never names the device, the titles in H1, or that killing the process drops the create draft and does not write a row. GUIDELINES hand-test requires those. |
| M4 | **open** | HOW says the draft lives in memory on the home-stack session. It does not say which object creates it, that leaving `add-combined` by Back discards it, or that process death discards it. The manage sheet must not reuse that draft (M1). |
| M5 | **open** | `how.md` has no Track heading. `HANDOFF.md` has no paste-ready starter and no done check. One Track is enough. This blocks the design gate's HANDOFF check after the HIGHs close; it does not make the product infeasible. |
| M6 | **open** | Member trash "asks before removing". The dialog title, body, and confirm label are not in the three locales. Account-row Remove does not ask, which matches `account-manage`. |

### LOW

| ID | Status | Basis |
|----|--------|------|
| L1 | **open** | The member row includes a grip, and drag reorder is not this version. Say the grip is visible and does not start a reorder. |
| L2 | **open** | INDEX status is `in progress` while the contract is still in design review and Compose still shows the 0.4.0 unavailable line. |
| L3 | **open** | Decided does not say that appended members keep the order they were confirmed in. M2's first-seen order should be the same rule for local Confirm. |

## Acceptance cross-check (initial)

| Acceptance sentence | Testable at design level | Gap |
|---------------------|--------------------------|-----|
| Gradle `:app:assembleDebug :app:testDebugUnitTest` from `android/` with Android Studio JBR | Yes, as a build command | No unit-test case is named for `kind: combined`, `canSign() == false`, or members-include-address |
| Add account → Create combined opens the create screen | Yes | Screen title and chrome are H1. Current code still opens `CombinedLaterScreen` (sample, not a design HIGH) |
| Local accounts can be toggled, and Back does not add them. Confirm adds them and returns to create | Yes for the create draft | Disabled-row copy and accent border are only in the frame, not in acceptance (H1, M3) |
| Create writes a combined row whose address is the current wallet | Not yet | Current wallet is undefined (H4) |
| Overflow shows Manage combined wallet account and Remove combined wallet account | Yes, labels are decided | Menu title 管理帳戶 is not decided (H1) |
| Remove deletes the row with no second dialog | Yes | Does not say member accounts remain (the Row sentence does) |
| Manage opens the member sheet | Yes | Sheet title, Add destination, and trash of the last member are open (H1, H4, M1) |

## Current-code sample (initial)

Current code not implementing 0.8.0 is not a design HIGH.

- `CombinedLaterScreen` shows `accounts.combinedLater` ("Combined accounts are not available yet." / 合併帳戶尚未開放。 / 合并账户尚未开放。). The chooser row still uses `ic_stroke_layers`. The proposal replaces this screen. No conflict with a behavior the proposal must keep.
- `AccountRecord.canSign()` is `kind != Watch`. A new `combined` kind that forgets this function would sign. The proposal says `canSign()` is false. HOW names the function. Not a second product path.
- No `members` field and no `kind: combined` reader exist yet. Vault JSON for other kinds is unchanged.
- 0.7.0 sign-transaction approval stays in memory and is not part of this flow. The combined row must not become an MWA signer (Decided, Not this version). Nothing in HOW routes MWA at this row.

## Fix tracker

| ID | Grade | Status | Where it closed |
|----|-------|--------|-----------------|
| H1 | HIGH | closed | INDEX Create, Add member, Manage menu, Member sheet, Copy; Acceptance titles |
| H2 | HIGH | closed | INDEX Create, Member sheet, Not this version; HANDOFF Track A |
| H3 | HIGH | closed | INDEX Domain and Copy; DOMAIN Not adopted keeps Main wallet |
| H4 | HIGH | closed | INDEX Current wallet |
| H5 | HIGH | closed | INDEX Home; HANDOFF Product |
| H6 | HIGH | closed | INDEX Domain; DOMAIN combined wallet account entry |
| M1 | MEDIUM | closed | INDEX Add member; HOW Track A |
| M2 | MEDIUM | closed | INDEX Add member; HOW Track A |
| M3 | MEDIUM | closed | INDEX Acceptance |
| M4 | MEDIUM | closed | INDEX Draft; HOW Track A |
| M5 | MEDIUM | closed | HOW Track A; HANDOFF Done check and Paste-ready |
| M6 | MEDIUM | closed | INDEX Create and Copy |
| M7 | MEDIUM | open | — |
| M8 | MEDIUM | open | — |
| M9 | MEDIUM | open | — |
| L1 | LOW | closed | INDEX Create, Member sheet, Not this version |
| L2 | LOW | open | — |
| L3 | LOW | closed | INDEX Row and Add member |

## Prior rounds

| Round | Date | Conclusion |
|-------|------|------------|
| initial | 2026-11-11 | Gate not passed. Open H1–H6. Proposal feasible. |
| 2 | 2026-11-11 | H1–H6 and M1–M6, L1, L3 closed in the contract. Gate passes. Open M7, M8, M9, L2. Proposal feasible. |
