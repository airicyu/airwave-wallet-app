# Implementation review — 0.8.0

Reviewed at: working tree
Tracks: A
Date: 2026-11-11 (Asia/Hong_Kong)
Round: 1
Role: implementation review (does not change code)
Baseline: decided sentences and acceptance in [`../INDEX.md`](../INDEX.md); [`how.md`](./how.md), [`../HANDOFF.md`](../HANDOFF.md)
Architecture bans: [`../../GUIDELINES.md`](../../GUIDELINES.md)

- **Verdict:** reject. Open HIGH: H1, H2, H3. `:app:assembleDebug` and `:app:testDebugUnitTest` passed (29 tests, 0 failures). Acceptance items 1–3 and the process-death check were not walked on a device.

## Match to how.md

| how.md step | Result |
|-------------|--------|
| `WalletModel` holds the create draft. Back from `add-combined` and process death discard it. The member sheet reads the vault row, not the draft. | done in code. `combinedDraft` is in-memory. `pop` clears it when leaving `CombinedCreate`. `clearSigningMaterial` clears it. The member sheet uses `accountRows`. Not walked on a device. |
| Confirm on the local picker or the paste field appends addresses and pops. From create, that updates the draft. From the member sheet, that writes the vault row and returns to `account-combined-manage`. | done in code. `appendCombinedMembers` branches on `combinedEditId`. Both confirms call `pop` twice. |
| Paste splits on newlines, trims, keeps first-seen order, and skips blanks, invalid public keys, and addresses already in `members`. | done. `parseCombinedPaste` plus `CombinedVaultTest.pasteSkipsBlankInvalidAndDuplicateLines`. |
| A combined vault row is JSON `kind: "combined"`, `address`, and `members`. `canSign()` is false. | done. `VaultAccounts` reads and writes those fields and rejects a row whose `address` is not in `members`. `signingSeed` is null. `AccountRecord.canSign()` is false. |
| Account-list overflow for `combined` navigates to the manage menu. Remove calls the existing delete path with no confirm dialog. | different. See H1. `removeAccount` deletes only that row and does not remove member accounts. The manage screen's Remove has no second dialog, but the overflow does not open that screen. |
| Manage opens the member sheet and writes member changes through the same vault save. | done in code once the manage screen is open. `writeCombined` calls `persistVault`. |
| Home shows the current-wallet address only and does not sum members. | done in code. `activateAccount` sets `address` to the row address. `syncHoldings` uses that one address. No member loop in holdings. Not walked. |
| Grip does not reorder. No rename pencil on create or the member sheet. | done. The grip is the character `⠿` and has no drag handler. Create and the member sheet do not draw `ic_stroke_rename`. |

Phone frames named in INDEX were not opened as a device walk. Static reading of `CombinedScreens.kt` is the layout evidence below.

## Findings

### HIGH

#### H1 — Combined overflow opens a dropdown, not `account-manage` — **fixed**

The combined kebab calls `onManageCombined`, which pushes `CombinedManage`. Other kinds still open the delete dropdown.

INDEX Manage menu: the overflow on a combined row opens `account-manage` (back, title `accounts.manageTitle`, the account name, the current-wallet short address, then Manage combined wallet account, then Remove combined wallet account). Other kinds keep the existing overflow.

`AccountListRow` sets `menuOpen` and shows a `DropdownMenu` with those two labels. Manage then pushes `CombinedManage`. Remove calls `removeAccount` from the dropdown and never shows `account-manage`. `CombinedManageScreen` matches the named screen, but the overflow does not open it.

#### H2 — Add-member copy keys are missing — **fixed**

`common.add`, `common.address`, and `common.localAccounts` are in en, zh-Hant, and zh-Hans.

INDEX Copy requires `common.add` (Add / 加入 / 加入), `common.address` (Address / 地址 / 地址), and `common.localAccounts` (Local accounts / 本機帳戶 / 本机账户).

Those keys are not in `AppStrings` for en, zh-Hant, or zh-Hans. `AppStrings.t` returns the key when it is missing. `CombinedAddScreen` and `CombinedPasteScreen` therefore show `common.add`, `common.address`, and `common.localAccounts` as titles and row labels. The paste field label and the add screen title are the same missing key.

#### H3 — Current-wallet card and member rows have no copy control — **fixed**

`CopyGlyph` uses `ic_stroke_copy` on the current-wallet card and on each member row.

INDEX Create: the current-wallet card shows the label, the member name, the short address, and copy. A member row is a grip, a check, a key mark or an empty slot, the name, the short address, copy, and trash. The glyph list includes copy.

`CurrentWalletCard` draws the label, an optional local name, and `BurnerKeys.short`. `MemberSheet` draws the grip, check, key or empty slot, name, short address, and trash. Neither calls `ic_stroke_copy`. The drawable exists and is used on the account list and on Home, not on these two surfaces.

### MEDIUM

#### M1 — zh-Hans has no `common.optional` — **fixed**

INDEX Copy: `common.optional` is Optional / 選填 / 选填. en and zh-Hant define it. zh-Hans does not, so `t` falls back to the English word Optional on the create name field.

### LOW

| ID | Status | Basis |
|----|--------|--------|
| L1 | open | A combined row on the account list still uses the key stroke (`ic_stroke_key`) unless the row is a watch account. `combined` only changes the dropdown. The row cannot sign. INDEX does not specify this list icon. |
| L2 | open | `accounts.combinedLater` remains in all three catalogs and has no caller. The chooser pushes `CombinedCreate`. |
| L3 | open | The member grip is the character `⠿`. There is no grip stroke drawable. It is visible and does not reorder. |

No architecture-ban break found in this diff. The create draft stays in `WalletModel` memory and is not written until `commitCombinedCreate`. Process death drops the ViewModel with the draft. The combined row has no seed and no mnemonic. `signingSeed` for this kind is null. Delete uses `removeAccount` on that id only. Holdings are not summed with `double`. No new dependency.

## Acceptance cross-check

| INDEX acceptance sentence | Result | Evidence |
|---------------------------|--------|----------|
| Gradle `:app:assembleDebug` and `:app:testDebugUnitTest` from `android/` | pass | Exit 0 on 2026-11-11. `BUILD SUCCESSFUL`. 29 tests, 0 failures, 0 errors. Suites: `HoldingRulesTest` 6, `SignTxPlanTest` 5, `RpcResolverTest` 4, `SettingsModelsTest` 3, `CombinedVaultTest` 2, `MwaPayloadsTest` 6, `SecretImportRulesTest` 1, `VaultCipherTest` 2. |
| Killing the process while the create screen is open writes no combined row. | not walked | Code: the draft is not in `toJson` until commit. Not exercised by killing a process. |
| 1. Add account → Create combined opens `add-combined` titled 聚合錢包帳戶. Local toggle, Back, Confirm, first member is current wallet, create writes the row, `canSign()` is false, `members` includes `address`. | not walked | Chooser calls `beginCombinedCreate` and pushes `CombinedCreate`. zh-Hant title string exists. Local staging is in memory until Confirm. `commitCombinedCreate` sets `address` to `current` and `members` includes it. `canSign()` is false in `CombinedVaultTest`. Device flow not observed. Add-member titles fail H2. |
| 2. Overflow opens 管理帳戶 with the two actions. Remove deletes the row with no second dialog and leaves member accounts. Manage opens 管理. Add uses the same screens. Confirm writes the vault row. Back does not. Trash of the last member is refused. A local row already on the account stays disabled. | not walked | Static: H1 fails the overflow entry. Member-sheet trash is hidden and `removeCombinedMember` returns when `members.size <= 1`. Local rows with `address in existing` are not clickable and append `accounts.alreadyMember`. Not observed on a device. |
| 3. With that combined row active, Home shows the assets of its current-wallet address only and does not list the other members. | not walked | `holdings.sync(address, …)` uses the active row address. No member list on `HomeAssetsScreen`. Not observed on a device. |

## Test results

Command (INDEX), from `android/`, `JAVA_HOME` binary `C:\Program Files\Android\Android Studio\jbr\bin\java.exe`:

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest
```

Exit code 0. `BUILD SUCCESSFUL` in about 3s. `testDebugUnitTest` ran. `CombinedVaultTest`: 2 tests, 0 failures. No mnemonic or private key in that test. Fixtures are a label `Combined`, the address string `addr`, and 32-byte arrays passed through base58.

Manual check: acceptance items 1–3 and the process-death sentence were **not walked** on a device. No emulator session was used. A static screenshot was not taken and would not count.

## Fix log

| ID | Grade | Status | Where it closed |
|----|-------|--------|-----------------|
| H1 | HIGH | open | — |
| H2 | HIGH | open | — |
| H3 | HIGH | open | — |
| M1 | MEDIUM | open | — |
| L1 | LOW | open | — |
| L2 | LOW | open | — |
| L3 | LOW | open | — |
