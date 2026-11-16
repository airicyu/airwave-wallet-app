# 0.8.0 — Combined wallet account

- **Status:** `shipped` (2026-11-16). Matches the roadmap table. Changelog: [`changelog.md`](../../../changelog.md) heading `0.8.0 — 2026-11-16`.
- **Previous:** [0.7.0](../0.7.0/INDEX.md) (`shipped`)
- **Design demo:** [`wallet-overall-v1.html`](../../design-demos/wallet-overall-v1.html) frames `add-combined`, `add-combined-add`, `add-combined-paste`, `add-combined-local`, `account-manage`, `account-combined-manage`. The manage menu matches the web wallet manage page. The local picker and the paste field match the web wallet after 0.59.0: a checkbox list, and a paste field that splits on whitespace, commas, and line breaks.

## Product

A user can create a **combined wallet account**: a vault row of member addresses plus one current-wallet address among them. It does not hold a key and cannot sign. Home keeps showing the active account's own assets.

The create and manage screens follow the web popup after its local combined-account edit: local members stay staged until Confirm, and the account overflow opens a manage menu instead of the member sheet.

## Read order

1. This file
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)

## Decided

| Topic | Decision |
|-------|----------|
| Domain | This version removes "Combined wallet account" and "member address" from DOMAIN Not adopted. The visible card label is Main wallet (`accounts.currentWallet`): English Main wallet, Traditional Chinese 主錢包, Simplified Chinese 主钱包. That label is not a new domain term |
| Row | Vault `kind` is `combined`. `address` is the current wallet. `members` is the ordered address list and includes `address`. No seed and no mnemonic. `canSign()` is false. Deleting the row does not delete member accounts |
| Current wallet | The first member appended to the create draft becomes `address`. Tapping the check on another member sets `address` to that member. On create this updates the draft only. On the member sheet it writes the vault row. Trash of the current-wallet member, while another member remains, sets `address` to the first remaining member. On create, trash of the last member clears `address` and leaves Create disabled. On the member sheet, trash that would leave `members` empty is refused and the row stays |
| Draft | `WalletModel` holds the create draft in memory. Back from the create screen discards it. Process death discards it and writes no row. The member sheet does not use this draft |
| Create | The chooser opens `add-combined`, not the unavailable line. Order: back chevron, title `accounts.screen.combined`, name label `accounts.label` with placeholder `common.optional`, the current-wallet card when a member exists, heading `accounts.members` plus a plus control, member rows, primary `accounts.combinedCreate`. The name is the vault `label`. A blank name is stored as `Combined`. The card label is `accounts.currentWallet`. The card has an accent border, then the member name, then one line with the short address and the copy control immediately after it. Members sit in one bordered sheet. The heading row is `accounts.members` and a filled accent plus. A member row is the account-list 2×6 dot grip, which does not reorder, a 34dp round check, a key mark when that address is a signing account on this device and an empty slot otherwise, the name, then one line with the short address and the copy control immediately after it, then a danger-filled rounded trash. The selected check is an accent fill with a light check mark. An unselected check keeps the same mark dimmed on a dark fill. There is no rename pencil. Trash removes that member immediately, with no confirmation. Create is enabled when `members` is not empty. Create writes the row, clears the draft, and returns to the account list |
| Add member | The plus opens `add-combined-add`: back, title `common.add`, row `common.address`, row `common.localAccounts`. Address opens `add-combined-paste`: back, title `common.address`, field label `common.address`, placeholder `accounts.pasteMulti`, primary `common.confirm`. Confirm splits on whitespace, commas, and line breaks, trims each part, keeps first-seen order, skips blanks, skips parts that are not valid public keys, skips addresses already in `members`, appends the rest, and returns to create. Local opens `add-combined-local`: back, title `common.localAccounts`, one line `accounts.pickLocalToAdd`, then one checkbox row per signing or watch account with the name on the left and the short address on the right. A row already in the target list is disabled, its checkbox stays off, and the name is followed by `accounts.alreadyMember`. The primary is `common.confirm`. On create the target list is the draft. On the member sheet the target list is the vault row's `members`. Checking a row stages that address and does not write the target list. Confirm appends staged addresses in first-seen order and returns to the screen that opened the add flow. Back leaves the target list unchanged. The member sheet uses these same three screens. There Confirm appends to the vault row and returns to `account-combined-manage`. Back does not write |
| Manage menu | The overflow on a combined row opens `account-manage`: back, title `accounts.manageTitle`, the account name, the current-wallet short address, then `accounts.manageCombined`, then `accounts.removeCombined` in the danger color. Remove deletes that combined row with no second confirmation and returns to the account list. A signing or watch row's overflow opens the same manage page: title `accounts.manage`, the account name, the full address, `accounts.revealPrivateKey` only when the row can sign, and `accounts.removeWalletAccount` in the danger color. The reveal screen title is `accounts.revealTitle`. It shows the name and short address, a masked card, the password label above the field, and an eye button whose label is `accounts.revealPrivateKey`. After the password opens the vault, that card shows the Base58 secret and a copy control, then `accounts.revealWarn`. Remove opens a dialog whose text is `accounts.removeConfirm`, with `common.cancel` and `accounts.removeMember`. Cancel or dismissing the dialog leaves the row. Confirm deletes that row and returns to the account list |
| Member sheet | `accounts.manageCombined` opens `account-combined-manage`: back, title `accounts.manage`, the current-wallet card, heading `accounts.members` plus the same plus, and the same member rows. No name field, no create button, no rename pencil. The grip is visible and does not reorder. Add, trash, and the check write the vault row. Back returns to the manage menu |
| Home | When the active account is `combined`, Home shows the assets of that row's current-wallet address only, using the existing single-account Home. It does not query or sum the other members. The row still cannot sign |
| Copy | `accounts.screen.combined` Combined wallet account / 聚合錢包帳戶 / 聚合钱包账户. `accounts.combinedCreate` Create combined wallet account / 建立聚合錢包帳戶 / 建立聚合钱包账户. `accounts.manageTitle` Manage account / 管理帳戶 / 管理账户. `accounts.manage` Manage / 管理 / 管理. `accounts.manageCombined` Manage combined wallet account / 管理聚合錢包帳戶 / 管理聚合钱包账户. `accounts.removeCombined` Remove combined wallet account / 移除聚合錢包帳戶 / 移除聚合钱包账户. `accounts.currentWallet` Main wallet / 主錢包 / 主钱包. `accounts.members` Members / 成員 / 成员. `common.add` Add / 加入 / 加入. `common.address` Address / 地址 / 地址. `common.localAccounts` Local accounts / 本機帳戶 / 本机账户. `common.confirm` Confirm / 確認 / 确认. `common.optional` Optional / 選填 / 选填. `accounts.label` Name / 名稱 / 名称. `accounts.pasteMulti` Address, or paste several lines / 地址，或貼上多行 / 地址，或贴上多行. `accounts.alreadyMember` Already added / 已加入 / 已加入. `accounts.pickLocalToAdd` Select the wallet accounts that you want to add / 選擇要加入的錢包帳戶 / 选择要加入的钱包账户. `accounts.removeMember` Remove / 移除 / 移除. Glyphs are the frame strokes: back chevron, copy, plus, grip, check, key, trash. The frame's eye on the current-wallet card is not shipped. The grip uses the account-list 2×6 dots because the web grip is too small to grab on a phone |
| Not this version | A merged home balance. Drag reorder. Renaming a member. Using the combined row to sign or to answer MWA |

## Still to decide before implementation

(empty)

## Not this version

| Item | Where it goes |
|------|----------------|
| Home assets summed across members | Unscheduled. Home shows the current-wallet address only |
| Member drag reorder | Unscheduled. The grip is visible and does not reorder |
| Renaming a member | Not this version. The frame pencil is not drawn |
| Combined row as a signer | Not this version |

## Acceptance

From `android/`, with `JAVA_HOME` set to the Android Studio JBR:

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest
```

On one Android emulator. Killing the process while the create screen is open writes no combined row.

1. Add account → Create combined opens `add-combined` titled 聚合錢包帳戶. Local accounts can be toggled. Back does not add them. Confirm adds them and returns to create. The first member is the current wallet. Create writes a combined row whose address is that current wallet. `canSign()` is false and `members` includes `address`.
2. That row's overflow opens 管理帳戶 with Manage combined wallet account and Remove combined wallet account. Remove deletes the row with no second dialog and leaves the member accounts. Manage opens 管理. Add uses the same address and local screens. Confirm writes the vault row. Back does not. Trash of the last member is refused. A local row already on the combined account stays disabled.
3. With that combined row active, Home shows the assets of its current-wallet address only and does not list the other members.
