# 0.11.0 — Home activity

- **Status:** `shipped` (2026-11-17). Matches the roadmap table. Changelog: [`changelog.md`](../../../changelog.md) heading `0.11.0 — 2026-11-17`.
- **Previous:** [0.10.0](../0.10.0/INDEX.md) (source packages, shipped). Behavior of assets, accounts, and MWA stays as shipped.
- **Design demo:** [`wallet-overall-v1.html`](../../design-demos/wallet-overall-v1.html) phone frame `home-activity`. The section head, row order, overlapping token icons, kind and time on one line, detail line, and external-link glyph are the visual contract. The demo's sample rows are not the product data. Kind titles are not colored green or red. Only the failure lead uses the danger color.

## Product

The home **Activity** tab lists recent transactions for the active account's exposed address. Mainnet with a Helius API key uses the Helius enhanced transactions endpoint. Every other reachable cluster uses `getSignaturesForAddress`. Rows are not written to disk. The external control opens that row's Orb transaction URL.

## Read order

1. This file
2. [`docs/reasoning.md`](./docs/reasoning.md)
3. [`docs/how.md`](./docs/how.md)

## Decided

| Topic | Decision |
|-------|----------|
| Address | The list uses the same address Home assets already shows. A combined account uses its current-wallet address. No active account shows the empty sentence and does not fetch |
| Gate | On Mainnet, when `activityHeliusKey` is null and the active mainnet RPC is not an allowed custom URL, do not fetch. Show the same guide or skipped block the assets tab shows for that holdings phase. Devnet always fetches. A stored Helius key on Mainnet uses the enhanced endpoint even when the JSON-RPC URL is missing |
| Settings field | The label is Helius API key (Helius API 金鑰 / Helius API 密钥). Confirm stores `heliusApiKeyFromStored` of the draft: a bare key, or the `api-key` / `apiKey` query of an http(s) URL. A non-empty draft that is not a key is not saved and the row stays in edit. An empty draft clears the field. Reading settings rewrites a previously saved URL down to that key. `heliusConfigured` is true when that function returns a key. Holdings and activity both use this key |
| Source | Mainnet with a Helius key: `GET https://api.helius.xyz/v0/addresses/{owner}/transactions` with `limit=20`, `token-accounts=balanceChanged`, `sort-order=desc`, `commitment=confirmed`, and `api-key`. The key is `activityHeliusKey` on the stored Helius field: a trimmed string with no whitespace is the key; an `http` or `https` URL yields its `api-key` query, or `apiKey` when `api-key` is absent. The host is not required to be `api.helius.xyz`. This is the web wallet's stored-key rule. `HeliusHoldings.apiKey` is the same function. This call runs when the activity key is non-null even if `RpcResolver.endpointForCluster` is null for Mainnet. The key is not written into an error string or a log. Any other reachable case: JSON-RPC `getSignaturesForAddress` on the URL `RpcResolver` already uses for that cluster, `limit` 20, commitment `confirmed`. Connect and read timeout are 20 seconds. A non-2xx response, a timeout, a body that is not a JSON array, or an RPC error returns no rows and the error sentence. Do not fall back from the enhanced call to the signature list |
| Page | Each request asks for 20 items. Page length is the number of elements in the JSON array, not the number of rows that mapped. The first load has no cursor. A later page sends `before` as the signature of the last array element that has a non-empty signature. Mapped rows whose signature is already on screen are not appended. When the array length is 20 and every mapped signature was already on screen, stop asking for more. Otherwise another page is available only when the array length is 20. The user reaching the end of the list loads the next page. A failure of that later page keeps the rows already shown and shows the older-page error under the list. Leaving the tab and coming back, or changing the active account, cluster, RPC URL, or Helius URL, drops the list and loads the first page again. A response that belongs to an older load does not replace the current list |
| Rows | Each row has a signature, a kind (`send`, `receive`, `swap`, or `tx`), a lead (`fail`, `ok`, or none), a detail string, a unix timestamp in seconds or none, an Orb URL, up to two mints, and up to two https icon URLs. Classification and the signature-list shape are in [`docs/how.md`](./docs/how.md). Every chain amount is taken from the JSON source as a decimal integer string and formatted with `BigInteger`. `rawTokenAmount.decimals` must be an integer from 0 to 18. When a token leg has no raw integer, the detail uses the JSON number's own digit text for tokenAmount and does not parse it as double or float. A native amount may be JSON integer digits, in lamports. A missing amount still uses the shortened signature. Do not add two transfers together |
| Icons | After the rows exist, look up those mints with the same Jupiter metadata call holdings already uses, on Mainnet only. An icon is kept only when its URL starts with `https:`. A failed image is omitted. A Jupiter symbol that is not empty, not the mint, and does not contain `…` replaces that mint's short address inside the detail. A lookup failure still shows the rows |
| Store | Rows live in memory on the home session. They are not written to the vault, to preferences, or to a second file. Process death drops them. The next visit loads again |
| Screen | Under the existing home top bar, one heading `common.activity`. Then one of: the RPC guide or skipped block, `common.loading`, `activity.empty`, `error.activityLoad`, or the list. A list row is, left to right: up to two 22dp token images overlapping by 6dp, then a block whose first line is the kind label and the local time `YYYY-MM-DD HH:MM:SS` on the same baseline (omit the time when the timestamp is missing), then the detail line. The detail is the detail string alone when the lead is none. A `fail` lead prefixes `activity.fail` in the danger color, then ` · ` and the detail when the detail is not empty. An `ok` lead prefixes `activity.success`, then the same joiner when the detail is not empty. The row ends with a 32dp hit target whose glyph is the external-link vector from the phone frame, content description `activity.openOrb`. The row body does not navigate. The glyph opens `https://orb.helius.dev/tx/{signature}?cluster=mainnet-beta` or `devnet` with `Intent.ACTION_VIEW` only when the URL starts with `https://orb.helius.dev/tx/`. While an older page loads, a progress indicator with content description `activity.loadingOlder` sits under the list. An older-page failure shows `error.activityOlderLoad` there. The existing Assets and Activity tabs stay. Activity is the selected tab |
| Copy | `common.activity` Activities / 活動 / 活动. `common.loading` Loading / 載入中 / 加载中. `activity.empty` No transactions yet / 尚無交易 / 尚无交易. `activity.fail` Failed / 失敗 / 失败. `activity.success` Success / 成功 / 成功. `activity.kind.send` Send / 送出 / 发送. `activity.kind.receive` Receive / 收到 / 收到. `activity.kind.swap` Swap / 互換 / 互换. `activity.kind.tx` Transaction / 交易 / 交易. `activity.openOrb` Open in explorer / 在區塊瀏覽器開啟 / 在区块浏览器打开. `activity.loadingOlder` Loading older activity / 載入更舊的活動 / 加载更旧的活动. `error.activityLoad` Activity unavailable / 活動暫時無法載入 / 活动暂时无法加载. `error.activityOlderLoad` Couldn't load older activity / 更舊的活動這次沒載到 / 更旧的活动这次没加载到 |
| Files | New activity types, the pure row mapper, and the fetch live under `com.airwave.wallet.home`. The screen is a new file in that package. `AppStrings.kt` gains the keys above and is not split. Do not add a Gradle dependency. Do not add an Activity. The external glyph is the existing vector `ic_stroke_external`, the phone-frame square and arrow, not a Material icon |

## Still to decide before implementation

(empty)

## Not this version

| Item | Where it goes |
|------|----------------|
| Send, token detail, address book, connected sites, about | Unscheduled |
| Combined-account merged balances on Home | Unscheduled. This list uses the current-wallet address only |
| AI on any screen | Unscheduled |
| Writing history to disk | Not this version |
| Kind colors green for receive and red for send | Not this version. The phone-frame remark is not the web row color |

## Acceptance

From `android/`, set `JAVA_HOME` to the Android Studio JBR (this machine: `C:\Program Files\Android\Android Studio\jbr`):

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Hand-test on one Android emulator. Use a devnet account that has at least one signature, and a mainnet account only when a Helius URL is already stored on the device. Do not paste that URL, a key, or an address into the repo.

1. Activity shows the heading Activities (or the locale equivalent), then rows. Each row has up to two 22dp token images, the second overlapping the first by 6dp when both exist, the kind and a `YYYY-MM-DD HH:MM:SS` time on one line, a detail line, and an external glyph. The kind label uses the normal text color. Only the `activity.fail` prefix uses the danger color. The glyph opens an Orb URL whose path starts with `/tx/`. The row body does nothing.
2. An account with no signatures shows No transactions yet. Mainnet with no Helius key and no allowed RPC shows the existing RPC guide and does not list rows.
3. A failed load shows Activity unavailable. Scrolling to the end of a full page of 20 requests the next page. A failed next page keeps the rows and shows Couldn't load older activity.
4. Switching the active account replaces the list. Locking the app and killing the process does not restore the previous rows without a new load. Assets, accounts, and MWA sign-and-send still behave as in 0.9.0 and 0.10.0.

Unit tests cover the row mapper: a SWAP with both sides, a TRANSFER that is send-only, receive-only, and both directions, a non-transfer type, a failed enhanced row, a signature-list row with and without `err`, a page cap of 20, an amount built from a decimal integer string, and a token object whose `decimals` is missing or outside 0 to 18 (that side has no amount). Do not put a live key in a test.
