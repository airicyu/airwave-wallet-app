# Unversioned changes

When the latest version is `shipped` and the next version is not opened yet, write **code** changes here and add one sentence under `Next` at the top of the root [`changelog.md`](../../changelog.md). Both places are the same batch.

Docs, privacy policy, and terms are not tracked here.

After the user assigns the batch, move the items into that version's INDEX and changelog heading, and set this file back to none.

## Current

MWA approval screens moved from `ui/MwaScreens.kt` to `ui/mwa`: connect, sign message, sign-and-send, and the shared chrome. Behavior is unchanged.

`WalletModel` moved to `com.airwave.wallet.session`. Shell routes and the approval request types are their own files. Offer, approve, and reject live in `WalletApproval.kt`. Unlock and the shell route stay on `WalletModel`. Behavior is unchanged.
