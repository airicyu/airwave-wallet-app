# 0.8.0 — how

Phone frames: `add-combined`, `add-combined-add`, `add-combined-paste`, `add-combined-local`, `account-manage`, `account-combined-manage` in `dev-docs/design-demos/wallet-overall-v1.html`.

## Track A — vault row and screens

`WalletModel` holds the create draft. Back from `add-combined` and process death discard it. The member sheet reads the vault row, not the draft.

Confirm on the local picker or the paste field appends addresses and pops. From create, that updates the draft. From the member sheet, that writes the vault row and returns to `account-combined-manage`.

Paste splits on newlines, trims, keeps first-seen order, and skips blanks, invalid public keys, and addresses already in `members`.

A combined vault row is JSON `kind: "combined"`, `address` (current wallet), and `members` (string array). `VaultAccounts` reads and writes those fields. `canSign()` is false for this kind.

The account-list overflow for `combined` navigates to the manage menu. Remove calls the existing delete path with no confirm dialog. Manage opens the member sheet and writes member changes through the same vault save as other account edits.
