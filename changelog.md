# Changelog

## 0.10.0 — 2026-11-16

Transaction byte readers moved into `SignTxCodec`, the v1 message into `SignTxV1`, and the combined-account manage screens into `CombinedManageScreens`. Account, settings, shell, and home code moved into those packages. Behavior is unchanged.

## 0.9.0 — 2026-11-16

MWA `sign_and_send_transactions` uses the 0.7.0 approval screen, then submits and returns one signature. `sign_transactions` still does not submit.

The unlock screen focuses the password field and opens the keyboard.

## 0.8.0 — 2026-11-16

Combined wallet account UI follows the local web wallet. Creating one stages local accounts until Confirm, then returns to the create screen. Leaving that picker without Confirm does not change the draft. A combined account's overflow opens the same manage page shape as other accounts: manage the members, or remove the combined account with no second confirmation.

The reveal-key screen matches the web wallet: title Reveal key, a masked secret card, the password label above the field, and an eye button.

Removing a signing or watch account from its manage page asks "Remove this wallet account?" in a dialog. Cancel leaves the account.

The overflow on a signing or watch account opens the manage page: the full address, Reveal private key, and Remove wallet account. It does not open a small menu.

A combined wallet account uses the stacked-layers mark in the account list, the same mark as the web wallet.

Removing a member from a combined wallet account happens on the trash tap, with no confirmation.

Every copy icon turns into a green check for 1.6 seconds after a tap, matching the web wallet.

The main-wallet card label is Main wallet. Its copy control, and each member's copy control, sit on the address line. The member check is a 34dp circle, the trash is a filled danger square, and the grip uses the account-list 2×6 dots.

Adding members follows the web wallet after 0.59.0. The local list is a checkbox row with the name on the left and the short address on the right, under a line that asks which accounts to add. Pasted text splits on whitespace, commas, and line breaks.

## 0.7.0 — 2026-11-11

MWA `sign_transactions` approval for one legacy, v0, or v1 payload. The wallet simulates, can edit compute budget on an unsigned transaction, and returns signed bytes. It does not submit. No AI card. `sign_and_send_transactions` still declines. The approval chrome follows the web wallet sign-transaction popout: copy on the account and site, the simulation card, the fee editor, and decoded instruction details.

The connect screen uses the web connect popout: back, centered title, site letter tile, a rail to the account mark, then the account name and short address. Reject and Connect sit on one row.

The sign-message screen uses that same popout chrome: account pill, site chip, centered title, a message-payload card, and a collapsed raw-byte disclosure. It still signs the original bytes and does not parse an off-chain header.

MWA approval screens live under `ui/mwa`, split into connect, sign message, sign-and-send, and shared chrome. `WalletModel` lives in `session`, with shell routes, the approval request, and offer/approve/reject in their own files.

Home uses the web home chrome: account pill, overflow, a section title with a refresh icon, and bordered token cards.

The create-wallet die uses the web spacing. Burner: 22dp under the name field and 8dp above the public key. Seed phrase: the warning and the die share one row, 24dp under the name field and 28dp above the word grid. A tap shakes the icon for 210ms.

The account list uses the web account-list chrome: a vertical overflow icon, an outlined add control with no fill, a tight active pill, a solid stroke on the other rows, pencil and overflow aligned with the account name, a copy control on the short address, and a 2×6 dot grip. While dragging, the row stays put at reduced opacity and an accent line marks the insertion slot. The order changes when the drag ends.

## 0.6.0 — 2026-11-08

MWA sign message. A connected dApp can request `sign_messages`. `MwaActivity` shows the message bytes and returns one ed25519 signature. The vault stays on that activity's unlock screen when it is locked. `sign_transactions` and `sign_and_send_transactions` still decline.

## 0.5.0 — 2026-11-07

MWA connect. `solana-wallet:` opens `MwaActivity`. A locked vault stays on the unlock screen until the user unlocks, then Connect authorizes the active account. The walletlib auth database is kept so a later `auth_token` can reauthorize. `sign_messages` is 0.6.0. Sign-and-send stays unscheduled. `bun run playground` starts Solana Mobile playground (`http://localhost:4747` on the emulator). The repo no longer ships `mwa-test-web/`.

## 0.4.0 — 2026-10-26

Multi-account vault: 12-word seed, burner, import mnemonic or secret, watch-only; switch active account from the list or the home pill. Add-account chooser and inner screens match the overall-v1 phone frames. Combined row is visible and does not create an account. Displayed addresses are first 4 + `…` + last 4. Verified token shield uses the web TokenTrust vector on a rounded fill, with organic-score badge on the shield.

## 0.3.0 — 2026-10-26

Home Assets lists native SOL, SPL Token, and Token-2022 for the active address. Jupiter supplies name, icon, and verified shield to the left of the token icon. Mainnet with no RPC shows the Helius guide instead of a public node. Activity stays empty.

## 0.2.0 — 2026-10-20

Native Android shell aligned with the extension wallet IA: hamburger top sheet, Network cluster picker, Settings hub (language, RPC, API keys, AI, CU price, wallet lock with idle auto-lock), and empty menu placeholders. Mobile visual contract in `dev-docs/design-demos/wallet-020.html` (bottom tabs, unlock gradient + brand mark, aligned secret/RPC inline actions). Durable settings JSON `airwave.settings.v1` in encrypted preferences. Airwave blue dark theme. MWA sign-and-send still resolves RPC from the request chain with per-cluster custom endpoints from settings.

## 0.1.0 — 2026-10-18

First native Android build. Set an app password, generate one burner wallet, and use a home screen with a wallet widget. Assets and Activity are empty tabs. dApps connect and request signatures through Mobile Wallet Adapter.
