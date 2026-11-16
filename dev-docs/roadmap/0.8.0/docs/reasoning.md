# 0.8.0 — reasoning

The web wallet's uncommitted combined-account edit is the source for this UI. Creating a combined account stages local wallets until Confirm, then adds them and returns to the create screen. Leaving without Confirm does not change the draft. A combined account's overflow opens the same manage page shape as other accounts: open the member screen, or remove that account with no confirmation.

The app's 0.4.0 chooser still lands on a "not available" line. This version replaces that line with the screens above and stores the row in the vault. Home does not sum member balances. The combined row cannot sign.
