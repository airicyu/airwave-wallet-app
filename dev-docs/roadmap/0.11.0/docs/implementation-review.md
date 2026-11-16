# Implementation review — 0.11.0

Reviewed at: working tree (HEAD `dfa4fbe` plus uncommitted Activity files)
Tracks: Map, Fetch, Screen
Date: 2026-11-16
Round: 1
Role: implementation review (does not change code)
Baseline: decided sentences and acceptance in [`../INDEX.md`](../INDEX.md); [`how.md`](./how.md), [`../HANDOFF.md`](../HANDOFF.md)
Architecture bans: [`../../GUIDELINES.md`](../../GUIDELINES.md)

- **Verdict:** reject. Open HIGH: H1. `:app:testDebugUnitTest --tests com.airwave.wallet.home.ActivityMapTest` passed (exit 0). The Activity flow was not walked on a device. A static reading of the composable is not that check.

## Match to how.md

| how.md step | Result |
|-------------|--------|
| Map one enhanced object or one signature-list entry into a row. Cap mapped rows at 20. Cursor is the last non-empty signature in the raw array. | done, with H1. `rowsFromEnhanced` and `rowsFromSignature` live in `ActivityMap.kt`. `ActivityMapTest` covers swap, send, receive, both directions, other type, failed enhanced, signature `err`, the cap, and a non-string amount. |
| Amounts are decimal integer strings and `BigInteger`. A JSON number is not an amount. Decimals must be an integer from 0 to 18. | done in `tokenLabel`, `nativeLabel`, and `formatUnits`. No `double` or `float` in the activity files. The out-of-range decimals case is not in the unit test (M2). |
| Mainnet with a Helius key calls the enhanced URL even when the JSON-RPC endpoint is null. Other reachable cases call `getSignaturesForAddress`. No fallback. Timeouts are 20 seconds. The key is not copied into the error text. | done. `ActivityFetch.page` branches on `HeliusHoldings.apiKey`. The owner is path-encoded on `URL("https", "api.helius.xyz", …)`. Failures return `ActivityPage(null, true)`. |
| After a successful enhanced Mainnet page, Jupiter metadata fills https icons and usable symbols. A lookup failure still returns the rows. | done. `withIcons` keeps `https:` URLs and swallows lookup failure. |
| Session memory holds generation, rows, cursor, and in-flight state. Entering the tab, or changing account, cluster, RPC URL, or Helius URL, drops the list and loads page one. A stale generation does not replace the list. | done in code. `ActivityModel` is on `WalletModel`. `lock` calls `activity.clear()`. Nothing in these files writes rows to the vault or to preferences. The composable also reloads when the holdings phase changes (L2). Not walked on a device. |
| Heading `common.activity`, then guide, skipped, loading, empty, error, or the list. Row chrome is overlapping 22dp icons, kind and local time on one line, detail with fail or success lead, and a 32dp external glyph. The row body does not navigate. The glyph opens an Orb `/tx/` URL. | done in code. `HomeActivityScreen` uses `AirwaveColors.Text` for the kind and `AirwaveColors.Danger` only for `activity.fail`. `ic_stroke_external.xml` is the phone-frame path (L1). Not walked on a device. |

Phone frame `home-activity` was read as HTML. It was not exercised as a running app.

## Findings

### HIGH

#### H1 — An unparsable token leg falls through to native SOL

INDEX Rows: if `rawTokenAmount.tokenAmount` is missing or is not a decimal integer string, or if `decimals` is missing or outside 0 to 18, that side has no amount and the detail uses the shortened signature. `how.md` also says token legs come from `tokenTransfers`, and if any token leg exists, native transfers are ignored.

`transferLegs` skips a token object when `tokenLabel` returns null. `legsForOwner` then treats that as "no token legs" and reads `nativeTransfers`. A TRANSFER that has a token leg with a bad amount and a native leg with a valid lamport string is classified from the SOL leg, so the detail is a SOL amount. The same skip lets `swapSides` fill a missing swap side from that native leg.

`missingDecimalsHasNoAmount` only builds a token leg and no native leg, so it still expects the shortened signature and does not catch this path.

### MEDIUM

#### M1 — Swap side preference treats a blank account as the owner

`how.md` Enhanced body: the spent or received side prefers the first `tokenInputs` or `tokenOutputs` entry whose `account` or `userAccount` is this owner, otherwise the first entry that has an amount. The same preference applies to `nativeInput` and `nativeOutput`.

`firstSide` sets the preferred side when `account` and `userAccount` are both empty. An earlier entry with no account wins over a later entry that names the owner.

#### M2 — The mapper test does not cover decimals outside 0 to 18

INDEX Acceptance: unit tests cover a token object whose `decimals` is missing or outside 0 to 18, and that side has no amount.

`missingDecimalsHasNoAmount` omits `decimals`. No test sets `decimals` below 0 or above 18. `tokenLabel` does reject that range.

### LOW

| ID | Status | Basis |
|----|--------|--------|
| L1 | open | `how.md` names `res/drawable/ic_external.xml`. The screen uses the existing `ic_stroke_external.xml`. Its path matches the phone-frame square-and-arrow (`M18 13v6…`, `M15 3h6v6`, `M10 14L21 3`), 24 view box, drawn at 14dp inside a 32dp hit target. |
| L2 | open | `HomeActivityScreen` reloads when `holdings.snapshot.phase` changes, in addition to address, cluster, RPC URL, and Helius URL. INDEX names only those identity changes, plus leaving the tab and coming back. |

No architecture-ban break found in these files. Rows stay on `ActivityModel` in the home `viewModelScope`. `lock` clears them. Process death drops the ViewModel with the list. Chain amounts use `BigInteger`. The Helius key is a query parameter and is not interpolated into `error.activityLoad` or `error.activityOlderLoad`. No new Gradle dependency and no new Android `Activity`. Copy keys in INDEX are present for en, zh-Hant, and zh-Hans. The tab label still uses `home.activity`. The list uses `WalletModel.address`, which activation sets from the account row, including a combined account's current-wallet address.

## Acceptance cross-check

| INDEX acceptance sentence | Result | Evidence |
|---------------------------|--------|----------|
| From `android/`, `JAVA_HOME` at the Android Studio JBR, run `:app:testDebugUnitTest --tests com.airwave.wallet.home.ActivityMapTest` | pass | Exit 0 on 2026-11-16. `BUILD SUCCESSFUL`. The JBR `java.exe` was used. This review did not run `:app:assembleDebug` or `:app:lintDebug`. |
| Unit tests cover swap both sides, send-only, receive-only, both directions, a non-transfer type, a failed enhanced row, a signature row with and without `err`, a page cap of 20, an amount from a decimal integer string, and decimals missing or outside 0 to 18. No live key. | partial | Seven tests in `ActivityMapTest`. The outside-range decimals case is M2. H1 is untested. |
| 1. Heading, row chrome, normal kind color, danger only on `activity.fail`, glyph opens an Orb URL whose path starts with `/tx/`, row body does nothing. | not walked | Code matches the chrome sentences. The flow was not walked on a device. |
| 2. No signatures shows No transactions yet. Mainnet with no Helius key and no allowed RPC shows the existing RPC guide and does not list rows. | not walked | Empty page sets `ActivityPhase.Empty`. `blocked` sets Guide or Skipped and does not call `start`. Not observed on a device. |
| 3. A failed load shows Activity unavailable. The end of a full page of 20 requests the next page. A failed next page keeps the rows and shows Couldn't load older activity. | not walked | First-page failure sets `ActivityPhase.Error`. `hasMore` is true when `rawCount == 20` unless every mapped signature is already on screen. Append failure sets `Older.Error` and does not clear `rows`. Not observed on a device. |
| 4. Switching the active account replaces the list. Locking and killing the process does not restore rows without a new load. Assets, accounts, and MWA sign-and-send still behave as in 0.9.0 and 0.10.0. | not walked | `enter` clears rows before the new load. `lock` calls `activity.clear()`. No durable activity store. Assets, accounts, and MWA were not re-run on a device. |

## Fix log

(empty)
