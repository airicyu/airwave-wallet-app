# 0.3.0 — how

Paths are repo-root relative. Implement under `android/`. Visual contract: phone frame of `dev-docs/design-demos/wallet-overall-v1.html` (`home-assets`, `rpc-guide`). The guide stays inside Home. It is not a route without tabs.

## File map

| File | Responsibility |
|------|----------------|
| `android/.../holdings/HoldingTypes.kt` | Row, phase, and round-result types. Does not fetch or format |
| `android/.../holdings/BaseUnits.kt` | `BigInteger` amounts, `formatBaseUnits`, and `halfTowardZero`. Does not fetch |
| `android/.../holdings/RpcHoldings.kt` | `getBalance` and token-program scans for one address. Does not call Jupiter or Helius |
| `android/.../holdings/HeliusHoldings.kt` | Decide the wallet-API path and map SPL rows. Does not draw UI |
| `android/.../holdings/JupiterMintMeta.kt` | Mainnet token search and row patch. Does not change base units |
| `android/.../holdings/HoldingsModel.kt` | Round ids, in-memory snapshot, refresh, cooldown, skip flag, clear on lock. Does not own the vault |
| `android/.../ui/HomeAssetsScreen.kt` | Assets list, guide phase, refresh. Does not parse RPC JSON and does not own the pill |
| `android/.../ui/ShellScreens.kt` | Existing shell. This version adds the Assets branch and the shared pill. It does not gain a new screen body |
| `android/.../WalletModel.kt` | Holds `HoldingsModel` and calls `clear()` from `lock`. Does not parse balances |

Package prefix: `com.airwave.wallet`. Each new file starts with an English responsibility comment and stays at or under 400 lines. `ShellScreens.kt` and `WalletModel.kt` are not split. Editing them adds the English header. The header on `ShellScreens.kt` says it is the existing shell and this version only adds the Assets branch and the shared pill.

## Types

```text
BaseUnits
  raw: BigInteger
  decimals: Int          // 0…18; native SOL is 9

HoldingRow
  id: "native-sol" | mint
  symbol, name: String
  amount: BaseUnits
  tokenProgram: "spl-token" | "token-2022" | null
  iconUrl: String?
  verified: Boolean
  organicScore: String?
  usdLabel: String

HoldingsSnapshot
  rows: List<HoldingRow>
  phase: idle | loading | ready | failed | guide | skipped
  error: String?
```

`formatBaseUnits` writes a decimal string from `raw` and `decimals` using only `BigInteger`. It strips trailing zeros in the fraction. A whole amount has no decimal point. `formatBaseUnits` of raw 1 and decimals 9 is `0.000000001`.

`halfTowardZero(raw)` is `raw / 2` toward zero. `halfTowardZero(1)` is 0. Send does not call it in this version. The unit test does.

## Numbers in JSON

Lamports and token amounts are taken from decimal digit text. Do not call `optLong`, `optDouble`, or `toDouble`. A JSON string is used as that text. A JSON number is accepted only by reading the characters of that token. If those characters are not a plain decimal (`^\d+(\.\d+)?$`), the field is unusable. `getBalance` with an unusable value fails the round. A token account with an unusable amount is malformed and is skipped.

## RPC wire

Commitment is `confirmed`.

`getBalance` params: `[address, {"commitment":"confirmed"}]`. Lamports are `result.value`.

`getTokenAccountsByOwner` params: `[address, {"programId": "<id>"}, {"encoding":"jsonParsed","commitment":"confirmed"}]`. For the wrapped-SOL lookup the second object is `{"mint":"So11111111111111111111111111111111111111112"}` instead of `programId`.

Each list element uses `account.data.parsed.info.mint`, `account.data.parsed.info.tokenAmount.amount` (string digits), and `account.data.parsed.info.tokenAmount.decimals`. `amount` must be `^\d+$`. Decimals outside 0…18, or any missing field, skips that element.

## Helius wire

The switch is the INDEX host and query rule, not `heliusConfigured`.

Request: `GET https://api.helius.xyz/v1/wallet/{address}/balances` with query `api-key` set to the extracted key, plus `page`, `limit=100`, `showNfts=false`, `showZeroBalance=false`, `showNative=false`. The same key is also sent as header `X-Api-Key`. Wait 500 ms between pages.

`page` starts at 1. Stop when `pagination.hasMore` is not true, when `balances` is missing or empty, or when `balances.length` is less than 100. At most 20 pages. If page 20 is full and `hasMore` is true, the round fails.

Each element: `mint` (string), `balance` (decimal text, as in Numbers in JSON), `decimals` (integer 0…18), `tokenProgram` (string, optional). Base units are `BigDecimal(balanceText)` moved right by `decimals`. If that value is not an integer, skip the element. Discard mint `So11111111111111111111111111111111111111112`. `tokenProgram` equal to `TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb` sets `token-2022`. Any other value, including a missing field, sets `spl-token`.

## Jupiter wire

`GET https://api.jup.ag/tokens/v2/search?query=` plus comma-separated mints, at most 100 per request. The native-SOL row looks up mint `So11111111111111111111111111111111111111112`. The response is a JSON array. Each object uses `id` (mint), `name`, `symbol`, `icon`, `isVerified`, `organicScore`, `usdPrice`. `usdPrice` and `organicScore` are decimal text under the Numbers rule. `isVerified` counts only when the JSON boolean is true. `icon` must start with `https:`.

Track B in this file is the Jupiter behavior. Track C is the Helius behavior. At run time the balance round, including Helius when that path is on, finishes before Jupiter starts.

## Track A — RPC list and round ids

1. Add the types, `BaseUnits`, and `RpcHoldings` for the non-Helius path.
2. `HoldingsModel` starts a round when Home is shown, the cluster changes, refresh is allowed, or the endpoint changes from null to a URL. It does not start a round while the endpoint is null.
3. Round ids follow the INDEX Session row. `WalletModel.lock` calls `clear()`, which retires the id and empties the snapshot.
4. `HomeAssetsScreen` renders loading, the row list, and the failure sentence. Native SOL is first. Wrapped SOL, if any, is second.

**Acceptance:** Devnet home shows Solana. Unit tests for format, half, sums, malformed skip, decimals range, and "Mainnet empty active does not use `PUBLIC_MAINNET`".

## Track B — Jupiter, shield, USD

1. After a successful balance round on Mainnet, `JupiterMintMeta` patches the rows as the INDEX Jupiter, Trust shield, and USD rows say.
2. A Jupiter HTTP failure keeps the balance rows and sorts the non-native rows by symbol.

**Acceptance:** Native symbol stays `SOL`. Wrapped symbol stays `wSOL`. A missing price stays `—`. A finite 0 price renders `$0.00`.

## Track C — Helius path

1. `HeliusHoldings` parses the key as the INDEX says.
2. On that path the only RPC reads are `getBalance` and the single wrapped-SOL `getTokenAccountsByOwner`. Other SPL rows come from the wallet API. One of those three failing fails the round.
3. Devnet does not call the wallet API.

**Acceptance:** Unit test for host and query parsing. A Mainnet URL without a key stays on the RPC path.

## Track D — Guide, pill, banner, cooldown

1. Phase `guide` is the Mainnet-without-RPC screen inside Assets. Bottom tabs stay. No Back.
2. English copy: title `Set Mainnet RPC`; body `Mainnet needs your own RPC before this wallet can load balances or send.`; link `Request an RPC endpoint from Helius`; buttons `Skip` and `Set RPC`.
3. zh-Hant copy: title `設定 Mainnet RPC`; body `Mainnet 必須填你自己的 RPC，才能查餘額與送出。`; link `向 Helius 申請 RPC endpoint`; buttons `略過` and `設定 RPC`. zh-Hans translates the English sentences. The body does not say the endpoint is free and does not say `API key`.
4. Skip sets phase `skipped` in memory only. `lock` clears it.
5. The shared Home top bar shows the pill and removes the old wallet label and address line. The pill is not clickable.
6. When `settings.cluster` is Devnet, `network.devnet` is shown under that top bar on Assets and Activity.
7. Refresh cooldown is 3 seconds after success.

**Acceptance:** Hand-test steps in `INDEX.md`. `assembleDebug`, `testDebugUnitTest`, and `lintDebug` pass.

## Privacy

Do not log the RPC URL query, the Helius key, the Jupiter key, the password, or the seed. Tests use the placeholder URL `https://api.helius.xyz/?api-key=REPLACE_ME`.
