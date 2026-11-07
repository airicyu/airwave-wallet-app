# Design review — 0.6.0 Airwave Wallet App

- Date: 2026-11-07 (Asia/Hong_Kong)
- Round: **re-review 1**
- Role: design review (does not edit INDEX / HOW / reasoning / HANDOFF / code; this file is not the contract)
- Baseline: decided sentences and acceptance in [`../INDEX.md`](../INDEX.md); [`how.md`](how.md); [`reasoning.md`](reasoning.md); [`../HANDOFF.md`](../HANDOFF.md) (proposal only, no code this version)
- Architecture bans: [`../../GUIDELINES.md`](../../GUIDELINES.md)
- Current-code sample: `MwaHostViewModel.kt`, `MwaActivity.kt`, `ui/MwaScreens.kt`, `mwa/MwaPayloads.kt`, `WalletModel.kt` `offer` / `approve` / `reject`
- **Verdict:** no open HIGH. The design gate passes. The proposal is feasible. Pending stays in memory, the result stays on the originating request, and this version does not schedule sign-and-send, SIWS, or an AI card.

## Findings (re-review 1)

Closed = written into INDEX / HOW / HANDOFF. Open = the contract still diverges. Non-blocking = record only.

H1, M1, M2, M3, L1, L2, and L3 are **closed**. Each locked sentence from the initial round is in the Decided table and mirrored in HOW. HANDOFF Track A names the address rule. HANDOFF Track B names `MwaActivity`, the centered title, `completeWith*` without `finish()`, and `mwa.watchAccount` until teardown.

### HIGH

None open.

### MEDIUM

None open. No new MEDIUM.

### LOW

| ID | Status | Basis |
|----|--------|--------|
| L4 | **open** | Non-blocking. Decided and HOW Track A say `MwaActivity` shows `mwa.watchAccount` until teardown. Acceptance step 4 only says a watch account does not open this screen. A hand-test can pass with the sentence never shown. Add one clause to step 4 if the visible sentence should be part of the device check. |

## Acceptance cross-check (re-review 1)

| Acceptance sentence | Testable at design level | Gap |
|---------------------|--------------------------|-----|
| Gradle `assembleDebug`, `testDebugUnitTest`, `lintDebug` from `android/` with Android Studio JBR | Yes | None |
| One emulator, `bun run playground`, `http://localhost:4747` after 0.5.0 Connect for a signable account | Yes | None |
| 1. Sign message opens this screen (back, title, lock, site, short address, message). Approve succeeds for that request. Reject fails only that request. Sign and send does not open an approval. | Yes. Track B draws `SignMessageBody` on `MwaActivity`. | None |
| 2. Non-UTF-8 shows the binary label and lowercase hex; the signature is over the original bytes. | Yes | None |
| 3. Lock fails the request. After unlock, no pending remains. Kill during this screen fails the request and it does not return. | Yes. Payload stays in memory. Approve and reject do not end chrome with `route = Home`. | None |
| 4. Watch account, empty payload, or more than one payload does not open this screen. | Yes as a negative check. | **L4:** the watch sentence is specified in Decided, not in this step. |
| Unit tests: display, empty and multi-payload decline-before-offer, address mismatch before offer. No mnemonic or private key. | Yes. Address rule is one element equal to the active public key. Empty check runs before `size`. | None |

## Current-code sample (re-review 1)

Current code not yet implementing 0.6.0 is not a design HIGH. HOW now contradicts these shipped behaviors on purpose:

- `MwaActivity` still shows unlock, reauthorize, `FakeWalletAuthorize`, or `AssociateWait`. It does not read `pending` or compose `SignMessageBody`.
- `ApprovalTop` weights the title with start alignment. `SiteLine` is already one concatenated line.
- `offer` sets `mwa.watchAccount` on `mwaError` and returns without a pending. That string is drawn on the home shell only. `finishApproval` and `reject` set `route = Home` when an address exists.
- `emptyPayloads` treats null, a missing first element, and an empty first element as empty, before any `size` read. `display` is strict UTF-8 or lowercase hex.
- `onSignMessagesRequest` still declines. Optional features are still empty. That is the work Tracks A and B add.

No second store, hardcoded password, plaintext key, or `double` amount is introduced by the proposal. DOMAIN is unchanged; pending stays in memory.

## Fix tracker

| ID | Grade | Status | Where it closed (INDEX item / HOW section) |
|----|-------|--------|-----------------------------------------------|
| H1 | HIGH | closed | INDEX Screen, Locked vault, Pending; HOW Track B; HANDOFF Track B |
| M1 | MEDIUM | closed | INDEX Account; HOW Track A addresses bullet; HANDOFF Track A |
| M2 | MEDIUM | closed | INDEX Screen (title centered between back and lock); HOW Track B |
| M3 | MEDIUM | closed | INDEX Account; HOW Track A watch bullet; HANDOFF Track B |
| L1 | LOW | closed | HOW Track A: `emptyPayloads` first, then `size` only when non-null |
| L2 | LOW | closed | INDEX Screen: shipped connect page is `FakeWalletAuthorize` and is not this layout |
| L3 | LOW | closed | INDEX Screen and HOW Track B: site row is one line |
| L4 | LOW | open | — |

## Prior rounds

| Round | Date | Conclusion |
|-------|------|------------|
| initial | 2026-11-07 | Stop for H1. Feasible after the `MwaActivity` host sentence is decided. M1–M3 should be fixed in the same pass. |
| re-review 1 | 2026-11-07 | Gate passes. H1 and M1–M3 and L1–L3 are in the contract. L4 is non-blocking. |

---

## Initial round (kept; IDs unchanged)

- Date: 2026-11-07 (Asia/Hong_Kong)
- Round: **initial**
- **Verdict (that round):** open HIGH remains. The design gate does not pass.

## Load-bearing decisions (initial)

`reasoning.md` names the approach: offer exactly one non-empty `sign_messages` payload, show UTF-8 or lowercase hex, sign the original bytes, and advertise only `solana:signMessages`. Dropped alternatives are off-chain header parsing, SIWS, sign-and-send, a new phone frame, and a new dependency. That matches INDEX **Not this version**. The screen chrome is in the Decided table (glyphs, title, site row, short address, message body, Reject then Approve), so the missing HTML frame is an explicit INDEX choice, not a missing screen spec.

## Findings (initial round)

Snapshot of that round. Live status is the fix tracker above. Status words in this section stay as written then.

### HIGH

#### H1 — Sign-message chrome is not on `MwaActivity` — **open**

INDEX requires a full screen on the existing `MwaActivity` approval route, with unlock-first on that activity, and session chrome that stays until teardown the same way connect does. `completeWith*` must not `finish()` the activity. HOW says the session shape stays `MwaActivity` plus `MwaHostViewModel`, then `onSignMessagesRequest` only calls `WalletModel.offer`, and that `SignMessageBody` already matches.

`offer` sets `WalletModel.route` to `Route.Approval`. The only caller of `MwaApprovalScreen` / `SignMessageBody` is `WalletApp` on that route (`MainActivity`). `MwaActivity` never reads `pending`. Its content is unlock, silent reauthorize, `FakeWalletAuthorize` for `AuthorizeDapp`, or `AssociateWait`. A sign-message callback that only `offer`s leaves `MwaActivity` on the wait spinner. The specified screen is either invisible or it appears by bringing the main activity’s approval route forward. 0.5.0 forbids bringing `MainActivity` Home forward, and this version says the screen is on `MwaActivity`.

Building HOW as written does not produce the decided screen, so the playground hand-test cannot show back, title, lock, site, short address, and the message on the MWA activity.

Lock this decided sentence (and mirror it in HOW Track B): `MwaActivity` draws `SignMessageBody` for the in-memory `SignMessage` pending, after `UnlockScreen` when the vault is locked; it does not leave the user on `AssociateWait` and it does not show this approval on `MainActivity`. `onSignMessagesRequest` still completes only that request from `MwaHostViewModel`. `completeWith*` does not `finish()` the activity; teardown still does. Approve and reject keep that activity’s session chrome until teardown and do not send this flow through `route = Home` as the way the chrome ends.

### MEDIUM

| ID | Status | Basis |
|----|--------|--------|
| M1 | **open** | INDEX: when `addresses` is present and does not equal the active public key, `completeWithAuthorizationNotValid()` and do not `offer`. HOW: non-empty and **none** equals the active public key. A list that contains the active key plus another key follows HOW and fails INDEX. Null or empty still signs with the active key in both texts. Lock one rule in both files: non-null, non-empty `addresses` is authorization-not-valid unless the list is exactly one element and that element equals the active public key bytes. |
| M2 | **open** | INDEX: centered title `mwa.signMessage.title`. `ApprovalTop` puts the title in the remaining row with start alignment. HOW says `SignMessageBody` already matches, so an implementer will not center it. HOW should say the title is centered in the top row between back and lock. |
| M3 | **open** | Decided: a watch account fails before `offer` with `mwa.watchAccount` and does not open this screen. `offer` sets `mwaError`, and the only `Text` of that value is on the home shell in `ShellScreens.kt`, behind `MwaActivity`. Acceptance 4 does not say the sentence is visible. Name the surface: after the decline, `MwaActivity` shows that one sentence until teardown, or the contract states the home shell shows it only after `MwaActivity` finishes. |

### LOW

| ID | Status | Basis |
|----|--------|--------|
| L1 | **open** | HOW checks `payloads.size != 1` in the same bullet as `emptyPayloads`. A null array is already empty via `MwaPayloads.emptyPayloads`. The size check has to run only after that, or it throws. One sentence in HOW is enough. |
| L2 | **open** | “Top row matches 0.5.0 connect” does not match the shipped connect UI (`FakeWalletAuthorize`: name, URI, Connect, Reject, no back or lock). The glyph list in the same INDEX cell is the spec. Soften the cross-reference so it does not point at the shipped connect layout. |
| L3 | **open** | The site row is one concatenated `Text` (`mwa.site` plus the value). INDEX asks for a label and a value. Either say one line is enough or ask for two text parts. Non-blocking. |

## Acceptance cross-check

| Acceptance sentence | Testable at design level | Gap |
|---------------------|--------------------------|-----|
| Gradle `assembleDebug`, `testDebugUnitTest`, `lintDebug` from `android/` with Android Studio JBR | Yes. Command is copied in HOW. | None |
| One emulator, `bun run playground`, `http://localhost:4747` after 0.5.0 Connect for a signable account | Yes. Same client as 0.5.0. | None |
| 1. Sign message opens this screen (back, title Sign message, lock, site, short address, message text). Approve succeeds for that request. Reject fails only that request; account and vault unchanged. Sign and send still does not open an approval. | The checks are human-runnable. | **H1:** HOW never puts this screen on `MwaActivity`, so step 1 is not what Track B builds. |
| 2. Non-UTF-8 shows the binary label and lowercase hex; the signature is over the original bytes. | Yes. `MwaPayloads.display` already uses a strict UTF-8 decoder and lowercase `%02x`. HOW keeps the raw `ByteArray` inside `approve`. | None at design level. |
| 3. Lock fails the request. After unlock, no pending remains. Kill the app during this screen: the request fails and does not return after the next unlock. | Yes. Same pending rules as 0.5.0. HOW does not write the payload to disk. | The screen must actually be the one in H1, or “during this screen” has no host. |
| 4. Watch account, empty payload, or more than one payload does not open this screen. | Yes, as a negative check. | **M1** is not in this sentence (address mismatch is only in the unit-test sentence). **M3:** the watch copy has no named surface. |
| Unit tests: `MwaPayloads.display` (UTF-8 vs hex), empty and multi-payload decline-before-offer, address mismatch before offer. No mnemonic or private key in a test. | Yes. | Address-mismatch test is ambiguous until M1 is locked. Empty-first is covered if tests call `emptyPayloads` (it treats a missing or empty first element as empty) and size != 1. |

## Current-code sample

Current code not implementing 0.6.0 is not a design HIGH by itself. These behaviors matter because HOW treats them as already correct:

- `onSignMessagesRequest`, `onSignTransactionsRequest`, and `onSignAndSendTransactionsRequest` all `completeWithDecline()`. Optional features on `MobileWalletAdapterConfig` are `emptyArray()`. That matches 0.5.0 and is the gap Track A/B are supposed to fill for `sign_messages` only.
- `MwaPayloads.emptyPayloads` and `display` match the payload and UTF-8/hex rules. `failBeforeOffer` is pending-or-watch. `offer` keeps a single in-memory `Pending`, rejects the incoming request when one already exists, sets `mwa.watchAccount` for a watch account, and does not persist the payload.
- `Pending.approve` can close over the raw bytes and return `ApprovalResult.Bytes`. No second store.
- `SignMessageBody` has the decided order and no letter tile or AI card. It is not composed by `MwaActivity`. Shipped connect UI in that activity is `FakeWalletAuthorize`, not `ConnectBody`.
- `finishApproval` / `reject` set `route = Home` when an address exists. That is a current-code conflict with “session chrome stays until teardown” if sign-message reuses those methods unchanged. H1’s locked sentence covers it.
- `mwaError` renders on the home shell only.

Backlog and Chrome extension behavior were not treated as decided. DOMAIN has no sign-message term; pending approval stays the in-memory definition, which this version does not contradict.

Initial-round tracker and prior-round table are replaced by the re-review 1 sections above. Finding text below stays so IDs are not renumbered.
