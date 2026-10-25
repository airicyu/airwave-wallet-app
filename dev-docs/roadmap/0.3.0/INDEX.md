# 0.3.0 — Home assets for the burner

- **Status:** `shipped`
- **Parent:** [0.2.0](../0.2.0/INDEX.md)
- **Changelog:** [`changelog.md`](../../../changelog.md) under `0.3.0 — 2026-10-26`
- **Design demo:** [`dev-docs/design-demos/wallet-overall-v1.html`](../../design-demos/wallet-overall-v1.html) screens `home-assets` and `rpc-guide`. Phone-frame content only. Designer notes are not shipped copy.

## Product

The unlocked home **Assets** tab shows the existing burner address's tokens on the active cluster. Native SOL is always a row. SPL Token and Token-2022 are listed when the balance is non-zero and the mint has decimals other than 0. Mainnet with no allowed active RPC does not call a public mainnet node. The account pill shows this one address and does not switch accounts. Activity stays the 0.2.0 empty tab.

## Read order

1. This file
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)
4. [`dev-docs/code-design.md`](../../code-design.md)
5. Code under `android/`

## Decided

| Topic | Decision |
|-------|----------|
| Address | The single address already decrypted from `airwave.vault.v1`. This version does not add accounts |
| Durable stores | Unchanged: vault `airwave.vault.v1`, settings `airwave.settings.v1`. Holdings are **not** written to either store |
| Session | One in-memory holdings snapshot owned by a new `HoldingsModel`, cleared on lock. `WalletModel` does not parse balances. `ShellScreens.kt` only branches the Assets tab to the new composable |
| RPC | `RpcResolver.endpointForCluster` for `settings.cluster`. Devnet uses the custom active URL when allowed, otherwise `PUBLIC_DEVNET`. Mainnet uses the custom active URL only. `PUBLIC_MAINNET` is not a holdings fallback |
| Mainnet without RPC | Assets shows the RPC guide (link `https://www.helius.dev/`, skip, set RPC). Skip does not start a fetch. It leaves an empty assets state that can open Settings → RPC. Set RPC opens the existing RPC screen |
| Fetch | One round for that address. Native lamports from `getBalance` even when 0. Then `getTokenAccountsByOwner` for Token program `TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA` and Token-2022 `TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb`. If either token program call fails, the round fails. Do not merge a half result |
| Rows | `native-sol` is always present, name `Solana`, symbol `SOL`, first. Wrapped SOL mint `So11111111111111111111111111111111111111112` is a separate row only when base units are greater than 0, name `Wrapped SOL`, symbol `wSOL`, second. Other mints with base units 0 or decimals 0 are dropped. A malformed token account is skipped. Two accounts of the same mint and the same program have their base units summed with `BigInteger` |
| Helius | Only when cluster is Mainnet and `heliusApiUrl` is an allowed URL on host `api.helius.xyz` whose query yields a non-empty `api-key`, else non-empty `apiKey`. Then SPL rows come from `GET /v1/wallet/{address}/balances` (`showNfts=false`, `showZeroBalance=false`, `limit` 100, serial pages, stop at 20 pages). Wallet rows whose mint is wrapped SOL are discarded. Native SOL and wrapped SOL still come from RPC on that same round. If the wallet call or that RPC fails, the round fails. Devnet ignores `heliusApiUrl` for holdings |
| Jupiter | Mainnet only, after a successful balance round. `GET https://api.jup.ag/tokens/v2/search` in batches of 100. Empty Jupiter key sends no key header. A non-empty `jupiterApiKey` sends header `x-api-key`. Jupiter failure leaves the balance rows in place. It does not rename `native-sol` or the wrapped-SOL symbol `wSOL`. Other mints take non-empty `name`, `symbol`, and https `icon` |
| Trust shield | Shield is shown only when Jupiter `isVerified` is true, and it is drawn to the **left** of the token icon. An `organicScore` greater than 0 is a small badge on that shield. The shield is never placed beside the token name |
| USD | A fiat label only. Built with `BigDecimal` from the Jupiter price string and the base-unit `BigInteger`. Missing or non-finite price renders `—`, never `$0`. Sort after native and wrapped SOL: finite USD descending, then symbol. No USD yet: symbol order |
| Numbers | Lamports and token base units are `BigInteger` built from decimal strings. No `double` and no `Float` for those units. Display goes through one function `formatBaseUnits` |
| Refresh | A refresh control on Assets. Ignore a second tap while a round is in flight. After success, ignore taps for 3 seconds. A failed round may be retried immediately. Cluster change starts a new round and drops the previous snapshot |
| Chrome | Devnet shows a Devnet banner. Account pill shows the signing mark and the truncated address and does not navigate. Activity copy stays `No activity yet.` Menu rows, Settings, Network, lock, and MWA stay 0.2.0 |
| Copy | New strings in `AppStrings.kt` for `en`, `zh-Hant`, and `zh-Hans`. English is the source wording |
| File shape | New files under `com.airwave.wallet.holdings` and one Assets composable. Each new `.kt` file has the English responsibility header and stays at or under 400 lines. This version does **not** split `ShellScreens.kt` |
| HTTP | `HttpURLConnection`, as `SolanaJsonRpc` already does. No new HTTP library |
| Dependencies | No new third-party package |

## Still to decide before implementation

*(empty)*

## Out of scope

| Item | Where |
|------|--------|
| Account list, switch, import, watch, 12-word generate | [0.4.0](../0.4.0/INDEX.md) |
| Token detail, send, activity, address book, combined account, simulated deltas, reclaim, sign AI | Not opened. No INDEX. Do not implement |
| Splitting `ShellScreens.kt` | Structure pass after this version, in the program |
| NFT gallery, Helius DAS `getAssetsByOwner`, persisting holdings | Not in this program |
| Public mainnet RPC as a silent fallback | Banned |

## Acceptance

From `android/`:

- `gradlew.bat :app:assembleDebug`
- `gradlew.bat :app:testDebugUnitTest`
- `gradlew.bat :app:lintDebug`

Device: Android Studio emulator.

### Hand-test

1. Unlock an existing vault on Devnet. Assets leaves the empty 0.2.0 copy and shows Solana plus any non-zero tokens. Pull or tap refresh. Kill the process, unlock, and the list loads again without a holdings file on disk.
2. Lock from the menu. The next unlock does not show the previous list before the new round finishes.
3. Switch Network to Mainnet with no active RPC. Assets shows the guide, and logcat shows no `api.mainnet-beta.solana.com` holdings call. Skip leaves an empty state. Set RPC opens the existing RPC screen.
4. Set an allowed Mainnet RPC, return to Assets, and see a Solana row. With a Helius URL that contains `api-key` on `api.helius.xyz`, SPL rows still load and native SOL stays its own row.
5. A verified token draws the shield to the left of the icon. Activity still says there is no activity. MWA authorize still opens the existing approval.

### Unit tests

- `formatBaseUnits` for 1 lamport at 9 decimals, and integer half-division, with no `double`.
- Mainnet with an empty active RPC does not select `PUBLIC_MAINNET`.
- Helius query parsing: `api-key` wins over `apiKey`; a non-Helius host is rejected; Devnet does not take the wallet-API path.
- Decimals 0 and zero SPL amounts are dropped. Native SOL remains when lamports are 0. Wrapped SOL is not merged into native SOL.
