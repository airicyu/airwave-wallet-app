# 0.11.0 how

## Tracks

1. **Map.** Pure functions in `com.airwave.wallet.home` turn one enhanced object, or one signature-list entry, into a row. Unit tests in this track.
2. **Fetch.** One page function calls Helius or `getSignaturesForAddress`, then attaches icons. No screen yet.
3. **Screen.** Replace the Activity tab placeholder in `shell` with the new composable. Entering the Activity tab, and changing the active account, cluster, RPC URL, or Helius URL, drops the list and loads the first page.

## Address and gate

Read the active account the assets tab already uses. If there is none, the phase is empty and no request runs. If the cluster is Mainnet, `activityHeliusKey(settings.heliusApiUrl)` is null, and `RpcResolver` has no allowed custom URL for Mainnet, the phase is the holdings guide or the skipped block, matching `HoldingPhase`, and no request runs. `activityHeliusKey` accepts a bare key and an `api-key` or `apiKey` query on any http(s) URL.

## Enhanced body

`rowFromEnhanced` drops an item that is not an object or has an empty `signature`.

`type` is compared in upper case.

- `SWAP`. Kind is `swap`. Read `events.swap`. The spent side is the first `tokenInputs` entry that has an amount, preferring one whose `account` or `userAccount` is this owner, otherwise the first amount, otherwise `nativeInput` with the same preference. The received side is `tokenOutputs`, then `nativeOutput`, with the same preference. If either side is missing, fill it from the owner's transfer legs: the first outgoing leg, then the first incoming leg. Detail is `{spent} → {received}` when both exist, otherwise the signature shortened to 4 characters, `…`, and the last 4. Mints are those two sides, de-duplicated, at most two.
- `TRANSFER`. Collect legs where `fromUserAccount` or `toUserAccount` is the owner. Token legs come from `tokenTransfers`. If any token leg exists, ignore `nativeTransfers`. Otherwise use native legs. Only outgoing is `send`. Only incoming is `receive`. Both or neither is `tx`. A send detail is `−{amount} {symbol}` plus ` → {counterparty}` when the counterparty is non-empty. The counterparty is shortened the same way. A receive detail is `+{amount} {symbol}`. A `tx` detail is the shortened signature. Mints follow the leg that supplied the detail, or every leg for `tx`, still at most two unique mints.
- Any other type. Kind is `tx`. Detail is the shortened signature. Mints follow the same owner legs, token legs first.

`transactionError` that is not JSON null sets lead `fail`. Otherwise the lead is none. `timestamp` is a finite number of unix seconds, or none.

A token amount prefers `rawTokenAmount.tokenAmount` when that value is a decimal integer, either quoted or JSON integer digits, and `rawTokenAmount.decimals` is an integer from 0 to 18. Format that pair with `BigInteger`. If the raw pair is missing, use the digit text of the sibling `tokenAmount` JSON number as the display amount. Do not parse that text as `double` or `float`. Native `amount` is lamports with 9 decimals and the symbol `SOL`, and it must be a decimal integer in the same way, quoted or as JSON integer digits. A fractional native amount is not a lamport count and that side has no amount. Symbols: the wrapped-SOL mint and native SOL are `SOL`, `EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v` is `USDC`, `Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB` is `USDT`. Every other mint is shortened until a Jupiter symbol replaces that short text.

`rowsFromEnhanced` walks the array in order and keeps each row that survives the mapper, at most 20. Page length for the next-page rule is the array length before mapping. `before` for the next request is the signature of the last array element that has a non-empty signature, including an element that did not become a row.

## Signature list

Each entry's `signature` is required. Kind is `tx`. Lead is `fail` when `err` is not null, otherwise `ok`. Detail is the shortened signature. `blockTime` is the timestamp when it is a number, otherwise none. Mints and icons are empty. Cap at 20.

## Orb

`https://orb.helius.dev/tx/{signature}?cluster=mainnet-beta` on Mainnet and `cluster=devnet` on Devnet. The signature is percent-encoded. The click handler refuses a URL that does not start with `https://orb.helius.dev/tx/`.

## Fetch

On Mainnet, when `activityHeliusKey` is non-null, call the enhanced endpoint even if `RpcResolver.endpointForCluster` returns null. Query `limit=20`, `token-accounts=balanceChanged`, `sort-order=desc`, `commitment=confirmed`, and `api-key`. Connect timeout and read timeout are 20 seconds. A non-2xx response, a timeout, a body that is not a JSON array, or an RPC error is the error sentence and does not fall back to `getSignaturesForAddress`.

Build the Helius URL with `URI` or `URL` setters, not string concatenation of the owner into a query that also holds the key. Encode the owner in the path. Pass `before` only when this load is a later page. That value is the signature named in the Page row of the INDEX, not the last row currently on screen when those two differ. Drop a mapped signature that is already on screen. When the raw array length is 20 and none of its mapped signatures are new, do not request another page. Parse with the same bounded scan the holdings code uses, or with `org.json` if that class is already on the classpath. Do not add a dependency either way. The API key stays in the request URL and is not copied into the row or the error text.

`getSignaturesForAddress` posts `{"jsonrpc":"2.0","id":1,"method":"getSignaturesForAddress","params":[owner,{"limit":20,"commitment":"confirmed"}]}` and adds `"before"` on a later page. Read `result` as an array of objects. A `error` member is a failed page.

After a successful enhanced page on Mainnet, call the existing Jupiter mint lookup for mints that still need an icon or a symbol. Apply `activityDetailWithSymbols`: replace each mint's short form in the detail when the symbol is usable. Icons are `{ mint, url }` for https URLs only, in mint order, dropping mints with no URL.

## Screen state

Hold the generation, the rows, whether another page exists, and whether a page is in flight on the session object the home shell already owns, not in a second store. The composable reads that state. The heading is `common.activity`. Leave the old `home.activity` key unused. Entering the Activity tab drops any rows already held and loads the first page, the same as changing the active account, cluster, RPC URL, or Helius URL. A load increments the generation. Apply a result only when the generation still matches. The end of the list is a zero-height item. When it becomes visible and another page exists and nothing is in flight, load with `before` set to the cursor from that page, which is the signature of the last raw array element that had a signature. If that page fails, keep the rows, remember that the user must scroll away before another attempt, and show `error.activityOlderLoad`.

## Glyph

The external glyph is the existing `res/drawable/ic_stroke_external.xml`, the same square-and-arrow path as the phone frame. `activity-ext` is 32dp. Token images are 22dp circles, the second shifted 6dp left over the first.
