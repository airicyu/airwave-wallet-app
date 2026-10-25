# 0.3.0 — reasoning

Home is still an empty Assets tab after 0.2.0. The overall-v1 demo is a token list. This version fills that list for the one burner the vault already holds, and stops there.

## Approach

Balances live in memory for the unlocked process. The vault stays key material. Settings stay the cluster and the endpoint. A failed process starts from an empty list and fetches again. That keeps a single durable story: nothing on disk is a second copy of "what the wallet holds."

The RPC rules from 0.2.0 stay. Mainnet sign-and-send already refuses the public cluster when no custom URL is selected. Holdings use the same resolver so the home screen cannot quietly talk to a different cluster than send will later use.

Helius wallet balances are an optional Mainnet shortcut for SPL rows, matching the product shape of web wallet 0.7.0 without taking that repo's types or storage keys. Native SOL and wrapped SOL always come from RPC so those two rows stay distinct. Devnet never uses the Helius wallet API, because that API is a mainnet product and would paint mainnet tokens on a devnet home.

Jupiter is metadata and a fiat label after the chain round. If Jupiter fails, the user still sees amounts. The trust shield stays on the left of the icon so a shield glyph inside a token name cannot look like verification.

## Dropped

- **Helius DAS `getAssetsByOwner`.** Web wallet left this path. It mixes NFTs into the wallet list and does not separate native SOL from wrapped SOL cleanly.
- **Persisting the last token list.** A disk cache becomes a second store the UI can show after a cluster change or a lock.
- **Public mainnet as a fallback.** It hides a missing RPC and will not match the endpoint used for signing.
- **`double` for lamports or token base units.** Display formatting is integer base units plus a decimal string. Fiat uses `BigDecimal` only for the USD label.
- **Putting the parser on `WalletModel` or the row UI inside `ShellScreens.kt`.** Both files are already the shell. `ShellScreens.kt` is over 400 lines and is split in the following structure pass, not in this feature.
- **Account switching, send, activity, combined wallets.** Each is a later version in the program. Shipping them here makes one review cover several products.

## Dependencies

No new package. HTTP stays on `HttpURLConnection`.
