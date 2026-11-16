# Design review — 0.11.0 Airwave Wallet App

- Date: 2026-11-16 (Asia/Hong_Kong)
- Round: **initial**
- Role: design review (does not edit INDEX / HOW / reasoning / HANDOFF / code; this file is not the contract)
- Baseline: decided sentences and acceptance in [`../INDEX.md`](../INDEX.md); [`how.md`](./how.md); [`reasoning.md`](./reasoning.md); [`../HANDOFF.md`](../HANDOFF.md)
- Architecture bans: [`../../GUIDELINES.md`](../../GUIDELINES.md)
- Current-code sample: `HeliusHoldings.apiKey`, `RpcResolver`, `shell/ShellScreens.kt` Activity placeholder, phone frame `home-activity` in `dev-docs/design-demos/wallet-overall-v1.html`
- **Verdict:** open HIGH remains. The review gate does not pass. The proposal is feasible once H1 is locked in INDEX and HOW. Rows stay in memory, results are not a second store, and the phone-frame row chrome is specified. Kind colors are explicitly out of contract.

## Load-bearing decisions

Reasoning names the approach: Helius enhanced transactions on Mainnet when `HeliusHoldings.apiKey` is present, `getSignaturesForAddress` otherwise, no fallback from a failed enhanced call, in-memory pages, and the stylesheet’s danger color only on the failure lead. Dropped: writing history to disk, green/red kind colors, a new package, and AI. Still to decide is empty.

## Findings (this round)

Closed = written into INDEX / HOW / HANDOFF. Open = the contract still diverges. Non-blocking = record only.

### HIGH

#### H1 — JSON number amount path contradicts the BigInteger rule — **open**

INDEX Rows: amounts are `BigInteger` built from decimal strings, and a chain amount does not use `double` or `float`. HOW Enhanced body then allows a missing `rawTokenAmount` pair to be rendered from a finite JSON number `tokenAmount` (up to 9 fractional digits), and it also allows `rawTokenAmount.tokenAmount` “or a safe integer”. A JSON number and a “safe integer” are the binary-float read. That path is still a chain amount on the row. The two sentences cannot both be implemented.

Lock this sentence in INDEX Rows and in HOW Enhanced body: every chain amount is taken from the JSON source as a decimal string and formatted with `BigInteger`; if `rawTokenAmount.tokenAmount` is missing or is not a decimal integer string, that side has no amount and the detail uses the shortened signature. Do not parse a JSON number as `double`, `float`, or a platform floating type.

### MEDIUM

| ID | Status | Basis |
|----|--------|--------|
| M1 | open | INDEX Page drops signatures already on screen and stops when a page of 20 adds no new row. HOW Fetch and Screen state never say that. An implementer who follows HOW alone can request the same page again. |
| M2 | open | “Page length is 20” is not locked as the raw response length or the mapped-row length. HOW keeps the first 20 rows that survive the mapper and sets `before` to the last shown signature. Dropped items can end the list early or skip signatures. |
| M3 | open | Acceptance names kind, time, detail, and the external glyph. It does not name the two 22dp icons overlapping by 6dp, or that only `activity.fail` uses the danger color. GUIDELINES require the hand-test to name that chrome. |

### LOW

| ID | Status | Basis |
|----|--------|--------|
| L1 | open | Frame `act-link` aria-label is 在瀏覽器開啟. INDEX `activity.openOrb` zh-Hant is 在區塊瀏覽器開啟. Visible row chrome still matches. |
| L2 | open | The shell placeholder reads `home.activity`. INDEX adds `common.activity` for the same heading. HOW does not say the new screen reads `common.activity` and leaves the old key unused. |

## Acceptance cross-check

| Acceptance sentence | Testable at design level | Gap |
|---------------------|--------------------------|-----|
| Gradle assemble, unit test, and lint from `android/` | Yes | None |
| Heading, kind and time on one line, detail, glyph opens `/tx/`, row body does nothing | Yes | M3: overlapping icons and danger-colored fail lead are not in the hand-test |
| Empty sentence; Mainnet without key and without allowed RPC shows the existing guide | Yes | None. Matches `HoldingPhase` plus `RpcResolver` Mainnet null endpoint |
| Failed load sentence; full page of 20 loads the next page; failed next page keeps rows | Yes | M1 and M2: HOW does not state the stop and the cursor |
| Account switch replaces the list; process death does not restore rows; assets, accounts, and MWA stay as shipped | Yes | None |
| Mapper unit tests, including an amount built from a string that is not a `double` | Yes | H1: HOW still permits a JSON-number amount, so the test does not cover the path HOW allows |

## Current-code sample

Current code does not implement this version. That is not a design HIGH.

- `HeliusHoldings.apiKey` returns a query `api-key` or `apiKey` only when `RpcResolver.isAllowedCustomUrl` is true and the host is `api.helius.xyz`. The proposal uses that function and does not log the key. No conflict.
- `RpcResolver.endpointForCluster` always returns a Devnet URL (`PUBLIC_DEVNET` when the custom URL is not allowed) and returns a Mainnet URL only for an allowed custom URL. INDEX “Devnet always fetches” and the Mainnet guide gate match that.
- `shell/ShellScreens.kt` Activity tab shows `home.noActivity` under the existing top bar and the Assets / Activity tabs. The proposal replaces that placeholder. `ui/ShellScreens.kt` still has the same placeholder; 0.10.0 left both trees. HOW names the `shell` file. Confirm at implementation which tree the app compiles; that is not a product contradiction.
- Phone frame `home-activity`: heading, then rows of up to two 22px icons overlapping by 6px, kind and `YYYY-MM-DD HH:MM:SS` on one line, detail, external-link path `d`. INDEX copies that chrome. Sample row text and the remark’s green/red kind colors are explicitly not the contract.

Backlog has no Activity item. Unscheduled screens stay in Not this version. Extension popup layout is not adopted.

## Fix tracker

| ID | Grade | Status | Where it closed |
|----|-------|--------|-----------------|
| H1 | HIGH | open | — |
| M1 | MEDIUM | open | — |
| M2 | MEDIUM | open | — |
| M3 | MEDIUM | open | — |
| L1 | LOW | open | — |
| L2 | LOW | open | — |

## Prior rounds

| Round | Date | Conclusion |
|-------|------|------------|
| initial | 2026-11-16 | Gate not passed. H1 open. Proposal feasible after that sentence is locked. |

---

# Design review — 0.11.0 — round 2

- Date: 2026-11-16 (Asia/Hong_Kong)
- Round: **2**. The initial round above is history. Do not renumber H1, M1, M2, M3, L1, L2.
- Role: design review. This file is not the contract.
- Baseline: decided sentences and acceptance in [`../INDEX.md`](../INDEX.md); [`how.md`](./how.md); [`reasoning.md`](./reasoning.md). HANDOFF still says the design review has not run; that sentence is stale and is not a product requirement.
- Architecture bans: [`../../GUIDELINES.md`](../../GUIDELINES.md) section 4. The main path keeps rows in session memory, does not add a package, and formats chain amounts with `BigInteger` from decimal integer strings. No ban is open.
- Current-code sample: `HeliusHoldings.apiKey`, `RpcResolver.endpointForCluster`, `HoldingsModel`, `WalletModel.syncHoldings`, `shell/ShellScreens.kt`, phone frame `home-activity`.
- **Verdict:** gate passed. No open HIGH. Proceed after M4, M5, and M6 are written into HOW (M4 also into INDEX Rows). The proposal is feasible.

## Load-bearing decisions

Unchanged and still written: enhanced transactions on Mainnet when `HeliusHoldings.apiKey` is non-null, signature list otherwise, no fallback, in-memory pages, danger color only on `activity.fail`. Reasoning now also locks the amount rule (decimal integer string and `BigInteger`; a missing string becomes the shortened signature) and the content-description choice for the external glyph.

## Findings (round 2)

Closed = the deciding sentence is in INDEX, HOW, or reasoning. Open = the contract still diverges.

### HIGH

No open HIGH. H1 is closed.

#### H1 — JSON number amount path contradicts the BigInteger rule — **closed**

INDEX Rows and HOW Enhanced body both say every chain amount is a decimal integer string formatted with `BigInteger`, and a JSON number is not an amount. The old “safe integer” / finite `tokenAmount` path is gone.

### MEDIUM

| ID | Status | Basis |
|----|--------|--------|
| M1 | closed | INDEX Page and HOW Fetch: drop a signature already on screen; when the raw array length is 20 and no mapped signature is new, do not request another page. |
| M2 | closed | INDEX Page and HOW Enhanced body: page length is the JSON array length before mapping. `before` is the signature of the last array element that has a non-empty signature, including an element that did not become a row. |
| M3 | closed | Acceptance item 1 names the 22dp images, the 6dp overlap, the normal kind color, and the danger color only on `activity.fail`. |
| M4 | open | A token side uses `rawTokenAmount.decimals` only when it is an integer from 0 to 18, but the no-amount sentence covers only a missing or non-integer `tokenAmount` (and the same for native `amount`). A missing or out-of-range `decimals` has no else. The formatter cannot run, and an implementer can print the base-unit integer as the display amount. Lock in INDEX Rows and HOW Enhanced body: if `decimals` is missing or not an integer from 0 to 18, that side has no amount and the detail uses the shortened signature. |
| M5 | open | HOW Fetch does not carry INDEX Source. It never names `limit=20`, `token-accounts=balanceChanged`, `sort-order=desc`, `commitment=confirmed`, or `api-key`; the 20-second connect and read timeouts; non-2xx, timeout, non-array, or RPC error as the error sentence with no fallback; or a fetch when `apiKey` is non-null and `RpcResolver.endpointForCluster` is null. `HoldingsModel.sync` returns the guide whenever the endpoint is null. Building Fetch from HOW alone follows that and skips the enhanced call INDEX Source requires. |
| M6 | open | INDEX Page drops the list and loads the first page when the user leaves the tab and comes back. HOW keeps the rows on the home session and reloads only for account, cluster, RPC URL, and Helius URL. The Activity tab is a field on `WalletModel`, so that session survives the tab switch. HOW as written shows the previous rows on return. |

### LOW

| ID | Status | Basis |
|----|--------|--------|
| L1 | closed | Reasoning states the frame aria 在瀏覽器開啟 is not the product string. `activity.openOrb` stays the web string. The visible glyph is the frame path. |
| L2 | closed | HOW Screen state: the heading is `common.activity`, and `home.activity` stays unused. |
| L3 | open | HOW Screen state withholds the next older-page attempt until the user scrolls away. The end sentinel is what starts that load, and it stays visible after a failure, so the retry does not run while the user remains at the end. INDEX only requires keeping the rows and showing `error.activityOlderLoad`. |
| L4 | open | The frame draws the external glyph at 14×14 inside the row. INDEX calls the control a 32dp hit target. HOW says `activity-ext` is 32dp, which can be read as a 32dp icon. The drawn stroke should stay the frame glyph inside the 32dp target. |

## Acceptance cross-check

| Acceptance sentence | Testable at design level | Gap |
|---------------------|--------------------------|-----|
| Gradle assemble, unit test, and lint from `android/` | Yes | None |
| Heading, 22dp icons overlapping by 6dp, kind and time on one line, detail, danger color only on the fail prefix, glyph opens `/tx/`, row body does nothing | Yes | L4 is size wording only. The hand-test names the chrome M3 asked for |
| Empty sentence; Mainnet without key and without allowed RPC shows the existing guide | Yes | M5: the opposite case (key present, Mainnet RPC missing) is an INDEX Source sentence and is not a hand-test step |
| Failed load sentence; full page of 20 loads the next page; failed next page keeps rows | Yes | M1 and M2 are now in HOW. L3 is the retry after that failure |
| Account switch replaces the list; process death does not restore rows; assets, accounts, and MWA stay as shipped | Yes | M6: leaving the tab and returning is in INDEX Page and is not in this hand-test |
| Mapper unit tests, including an amount built from a string that is not a `double` | Yes | H1 is closed. M4: a missing `decimals` is not one of those tests |

## Current-code sample

Current code does not implement this version. That is not a design HIGH.

- `HeliusHoldings.apiKey` still returns a query `api-key` or `apiKey` only for an allowed `api.helius.xyz` URL. INDEX uses that function. No conflict.
- `RpcResolver.endpointForCluster` still always returns a Devnet URL and returns a Mainnet URL only for an allowed custom URL. The guide gate matches. M5 is the missing HOW sentence for the key-without-RPC path, not a second endpoint in code.
- `WalletModel.address` is the account `address` field. Combined accounts store the current wallet there. The list address matches assets. No conflict.
- `shell/ShellScreens.kt` still shows `home.noActivity` under the top bar and the Assets / Activity tabs. `ui/ShellScreens.kt` still has the same placeholder. HOW names the `shell` file. Which tree the app compiles is an implementation check.
- Phone frame `home-activity`: heading, then rows of 22px icons with `margin-left: -6px` on the second, kind and `YYYY-MM-DD HH:MM:SS` on one line, detail, external-link path `d` (`M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6` plus the polyline and the diagonal line) in a 24 view box. INDEX copies that chrome. Sample amounts and the remark’s green/red kind colors stay out of contract.

## Fix tracker

| ID | Grade | Status | Where it closed |
|----|-------|--------|-----------------|
| H1 | HIGH | closed | INDEX Rows; HOW Enhanced body; reasoning paragraph on `BigInteger` |
| M1 | MEDIUM | closed | INDEX Page; HOW Fetch |
| M2 | MEDIUM | closed | INDEX Page; HOW Enhanced body; HOW Fetch |
| M3 | MEDIUM | closed | INDEX Acceptance item 1 |
| M4 | MEDIUM | open | — |
| M5 | MEDIUM | open | — |
| M6 | MEDIUM | open | — |
| L1 | LOW | closed | reasoning, frame aria versus `activity.openOrb` |
| L2 | LOW | closed | HOW Screen state |
| L3 | LOW | open | — |
| L4 | LOW | open | — |

## Prior-round summary

| Round | Date | Conclusion |
|-------|------|------------|
| initial | 2026-11-16 | Gate not passed. H1 open. |
| 2 | 2026-11-16 | Gate passed. H1, M1, M2, M3, L1, and L2 are closed in the contract. M4, M5, and M6 stay open. Feasible. |

---

# Design review — 0.11.0 — round 3

- Date: 2026-11-16 (Asia/Hong_Kong)
- Round: **3** (current). Prior rounds stay above. Do not renumber H1, M1–M6, L1, L2.
- Role: design review. This file is not the contract. INDEX, HOW, reasoning, and HANDOFF were not edited.
- Baseline: decided sentences and acceptance in [`../INDEX.md`](../INDEX.md); [`how.md`](./how.md). Reasoning was read and is unchanged on these points. HANDOFF still says the design review has not run; that sentence is stale and is not a product requirement.
- Architecture bans: [`../../GUIDELINES.md`](../../GUIDELINES.md) section 4. Rows stay in session memory. Chain amounts stay `BigInteger` from decimal integer strings. No ban is open.
- Phone frame: `home-activity` in `dev-docs/design-demos/wallet-overall-v1.html`. Heading, overlapping token icons, kind and time on one line, detail, external-link path `d` at 14×14 inside a 24 view box.
- **Verdict:** gate passed. No open HIGH. No open MEDIUM. Proceed. L3 and L4 stay open and do not block. The proposal is feasible.

## Load-bearing decisions

Unchanged. Enhanced transactions on Mainnet when `HeliusHoldings.apiKey` is non-null, including when the Mainnet RPC endpoint is null. Signature list otherwise. No fallback. In-memory pages. Danger color only on `activity.fail`. Amounts are decimal integer strings and `BigInteger`.

## Findings (round 3)

Closed = INDEX or HOW now contains the required sentence. Open = the contract still diverges.

### HIGH

No open HIGH. H1 stays closed.

### MEDIUM

| ID | Status | Basis |
|----|--------|--------|
| M1 | closed | Unchanged from round 2. INDEX Page and HOW Fetch. |
| M2 | closed | Unchanged from round 2. INDEX Page and HOW Enhanced body. |
| M3 | closed | Unchanged from round 2. INDEX Acceptance item 1. |
| M4 | closed | INDEX Rows: if `decimals` is missing or outside 0 to 18, that side has no amount and the detail uses the shortened signature. HOW Enhanced body: if `decimals` is missing or outside 0 to 18, that side has no amount and the detail uses the shortened signature. Acceptance unit tests name a token object whose `decimals` is missing or outside 0 to 18. |
| M5 | closed | HOW Fetch: on Mainnet, when `HeliusHoldings.apiKey` is non-null, call the enhanced endpoint even if `RpcResolver.endpointForCluster` returns null. Query `limit=20`, `token-accounts=balanceChanged`, `sort-order=desc`, `commitment=confirmed`, and `api-key`. Connect timeout and read timeout are 20 seconds. A non-2xx response, a timeout, a body that is not a JSON array, or an RPC error is the error sentence and does not fall back to `getSignaturesForAddress`. |
| M6 | closed | HOW Tracks and HOW Screen state: entering the Activity tab drops any rows already held and loads the first page. INDEX Page already said leaving the tab and coming back drops the list and loads the first page. |

### LOW

| ID | Status | Basis |
|----|--------|--------|
| L1 | closed | Unchanged. |
| L2 | closed | Unchanged. HOW Screen state still names `common.activity` and leaves `home.activity` unused. |
| L3 | open | HOW Screen state still withholds the next older-page attempt until the user scrolls away. INDEX only requires keeping the rows and showing `error.activityOlderLoad`. |
| L4 | open | The phone frame draws the external glyph at 14×14. INDEX calls the control a 32dp hit target. HOW says `activity-ext` is 32dp, which can be read as a 32dp icon. |

## Acceptance cross-check

| Acceptance sentence | Testable at design level | Gap |
|---------------------|--------------------------|-----|
| Gradle assemble, unit test, and lint from `android/` | Yes | None |
| Heading, 22dp icons overlapping by 6dp, kind and time, detail, danger color only on the fail prefix, glyph opens `/tx/`, row body does nothing | Yes | L4 is size wording only |
| Empty sentence; Mainnet without key and without allowed RPC shows the existing guide | Yes | The key-present, Mainnet-RPC-null fetch is now in HOW Fetch (M5). It is still not a hand-test step. That does not reopen M5 |
| Failed load; full page of 20 loads the next page; failed next page keeps rows | Yes | L3 remains the retry-after-failure wording |
| Account switch replaces the list; process death does not restore rows; assets, accounts, and MWA stay as shipped | Yes | Entering the Activity tab is now in HOW (M6). It is still not a numbered hand-test step. That does not reopen M6 |
| Mapper unit tests, including a missing or out-of-range `decimals` | Yes | M4 is closed |

## Fix tracker

| ID | Grade | Status | Where it closed |
|----|-------|--------|-----------------|
| H1 | HIGH | closed | INDEX Rows; HOW Enhanced body |
| M1 | MEDIUM | closed | INDEX Page; HOW Fetch |
| M2 | MEDIUM | closed | INDEX Page; HOW Enhanced body; HOW Fetch |
| M3 | MEDIUM | closed | INDEX Acceptance item 1 |
| M4 | MEDIUM | closed | INDEX Rows; HOW Enhanced body |
| M5 | MEDIUM | closed | HOW Fetch |
| M6 | MEDIUM | closed | HOW Tracks; HOW Screen state |
| L1 | LOW | closed | reasoning |
| L2 | LOW | closed | HOW Screen state |
| L3 | LOW | open | — |
| L4 | LOW | open | — |

## Prior-round summary

| Round | Date | Conclusion |
|-------|------|------------|
| initial | 2026-11-16 | Gate not passed. H1 open. |
| 2 | 2026-11-16 | Gate passed with M4, M5, and M6 still open. Feasible. |
| 3 | 2026-11-16 | Gate passed. No open HIGH. No open MEDIUM. L3 and L4 stay open. Feasible. Proceed. |
