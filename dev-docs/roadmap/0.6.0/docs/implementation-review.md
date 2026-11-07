# Implementation review — 0.6.0 Airwave Wallet App

- Date: 2026-11-07 (Asia/Hong_Kong)
- Round: **re-review 3**
- Role: implementation review (does not change code)
- Baseline: decided sentences and acceptance in [`../INDEX.md`](../INDEX.md); [`how.md`](./how.md), [`reasoning.md`](./reasoning.md), [`../HANDOFF.md`](../HANDOFF.md)
- Architecture bans: [`../../GUIDELINES.md`](../../GUIDELINES.md)
- **Verdict:** no open HIGH. No open MEDIUM. H1 stayed closed: a normal teardown (`dropTeardown` false) still calls `failWaitingSign()` and `wallet.endAssociation()`. M3 is closed: when `processLaunch` replaces a scenario it calls `failWaitingSign()` and `endAssociation()` before `dropTeardown` and `previous.close()`, so a held locked sign request is declined and `waitingSign` does not stay set. L1 stays open. `:app:testDebugUnitTest` passed (22 tests, 0 failures). Playground acceptance items 1–4 were not walked on a device, so acceptance is not fully checked.

## Findings

### HIGH

#### H1 — Sign-message pending survives session teardown — **closed**

`WalletModel.reject` and `finishApproval` leave a `SignMessage` pending in memory so `MwaActivity` can keep the approval chrome until teardown. Teardown only sets `MwaEvent.SessionTerminated`, and `MwaActivity` calls `finish()`. Nothing on that path calls `endAssociation` or `dropSessionChrome`. `endAssociation` has no caller.

If the association ends before approve or reject, `pending` stays set and `approvalSettled` stays false. The next `sign_messages` hits `failBeforeOffer` and is declined, and the first request is never completed. INDEX pending rules require one in-memory pending, a result only for that request, and failure of the in-flight request when the session cannot continue (lock and process death already do this; teardown does not).

#### H2 — Locked vault snapshots a blank account and the wrong address check — **closed**

INDEX: if the vault is locked, `MwaActivity` shows `UnlockScreen` first, then this screen, and the signature and the displayed short address are the signable account for `activeAccountId`.

`MwaHostViewModel.offerSignMessage` runs before unlock. It sets `accountShort` from `model.address`, which is null while locked, so the short line is empty. The `addresses` check uses `authorizedAccounts[0]` when `model.address` is null. `WalletModel.unlock` does not rewrite that `Pending`. After unlock the screen still shows the empty short address, and the signature uses the unlocked active seed, which can differ from the key used in the pre-check.

### MEDIUM

#### M1 — A locked watch account is offered, then declined on unlock — **closed**

When the vault is already unlocked, `WalletModel.offer` uses `failBeforeOffer` and does not store a watch pending, and `MwaActivity` shows `mwa.watchAccount`. When the vault is locked, `activeAccount()` is null, so the watch check does not run. The pending is stored. `unlock` then sets `mwaError`, clears the pending, completes it as rejected, and sets `route = Home`. The watch sentence appears, but the failure is after offer, and Home is set on that path. Approve and reject themselves do not set `route = Home`.

### LOW

| ID | Status | Basis |
|----|--------|--------|
| L1 | open | `MwaApprovalScreen` still has a `SignMessage` branch. `offer` sets `route = Home` for this kind, so `MainActivity` does not open it unless `route` was already `Approval`. Re-review 1: still true. |
| L2 | closed | `MwaPayloadsTest` asserts `declineBeforeOffer(arrayOf(ByteArray(0)))`. |

No architecture-ban break found in this diff: the payload stays in the in-memory `Pending`, the signature bytes are the original array, `sign_transactions` and `sign_and_send_transactions` still decline, and capabilities list only `solana:signMessages`. No new dependency. `completeWithSignedPayloads` / `completeWithDecline` do not call `finish()`.

## Acceptance cross-check

| INDEX acceptance sentence | Result | Evidence |
|---------------------------|--------|----------|
| Gradle `assembleDebug`, `testDebugUnitTest`, `lintDebug` from `android/` | pass | Command exited 0 on 2026-11-07. `BUILD SUCCESSFUL`. `MwaPayloadsTest` result XML: 6 tests, 0 failures (`invalidUtf8UsesHex`, `utf8MessageKeepsText`, `emptyPayloadsDecline`, `multiPayloadDeclinesBeforeOffer`, `addressListMustBeExactlyTheActiveKey`, `secondPendingAndWatchFailBeforeOffer`). |
| 1. Playground Sign message opens this screen (back, title, lock, site, short address, message). Approve succeeds for that request. Reject fails only that request. Sign and send does not open an approval. | not walked | No device walk. Static reading: `SignMessageBody` matches the chrome; sign-and-send still calls `completeWithDecline()`. Behavior of approve/reject on a device was not observed. |
| 2. A non-UTF-8 payload shows the binary label and lowercase hex, and the signature is over the original bytes. | partial | Unit test `invalidUtf8UsesHex` expects `fffe`. `approve` signs the `ByteArray` passed into `offerSignMessage`, not the display string. The on-screen label was not walked. |
| 3. Lock on this screen fails the request. After unlock, no pending remains. Kill during this screen: the request fails and does not return. | not walked | Code: `lock` calls `failPending`, which clears `pending` and finishes rejected. Process death drops memory. Not exercised on a device. Re-review 1: real teardown calls `endAssociation` (H1 closed). |
| 4. Watch, empty payload, or more than one payload does not open this screen. Watch shows `mwa.watchAccount` until teardown. | partial | Unlocked watch and payload rules are in `offer` / `declineBeforeOffer` and unit tests. Device not walked. Re-review 1: locked watch is decided in `offer` after unlock (M1 closed). |
| Unit tests cover display, empty and multi-payload decline-before-offer, and address mismatch. No mnemonic or private key in a test. | pass | `MwaPayloadsTest`. Fixtures are the string `hello` / `airwave mwa test` and short byte arrays. |

## Test results

Command (INDEX), from `android/`, `JAVA_HOME` = `C:\Program Files\Android\Android Studio\jbr`, `java` on `PATH` from that JBR `bin`:

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Exit code 0. `lintDebug` executed; assemble and unit-test tasks were up to date and the stored `MwaPayloadsTest` suite had 0 failures.

Manual check: acceptance items 1–4 were **not walked** on a device. Target: none. `emulator-5554` was not used. The playground flow needs an unlocked vault after Connect. This review does not enter a password or a mnemonic. A static screenshot was not taken and would not count.

## Fix tracker

| ID | Grade | Status | Where it closed |
|----|-------|--------|-----------------|
| H1 | HIGH | closed | Re-review 1: real teardown calls `wallet.endAssociation()`, which clears a `SignMessage` pending. `dropTeardown` still returns first. |
| H2 | HIGH | closed | Re-review 1: locked vault stores `waitingSign` and does not `offer` until `onMwaUnlocked`. Short address and `addressesAllow` use `model.address` after `activateAccount`. |
| M1 | MEDIUM | closed | Re-review 1: after unlock, `offer` rejects a watch account before storing pending and sets `route = Home`. `MwaActivity` then shows `mwa.watchAccount`. |
| M2 | MEDIUM | closed | Re-review 2: while locked and `waitingSign` is set, the second `onSignMessagesRequest` calls `completeWithDecline` and returns. |
| M3 | MEDIUM | closed | Re-review 3: `processLaunch` calls `failWaitingSign()` and `endAssociation()` before `dropTeardown` and `previous.close()`. |
| L1 | LOW | open | — |
| L2 | LOW | closed | `declineBeforeOffer(arrayOf(ByteArray(0)))` in `MwaPayloadsTest` |

## Re-review 1

Checked the code marked "fixed in code". No product code was changed in this round.

H1 is fixed. `onScenarioTeardownComplete` returns only when `dropTeardown` is set (a replaced scenario). The real teardown calls `failWaitingSign()` and then `wallet.endAssociation()`. `endAssociation` clears `pending` on both paths: `dropSessionChrome()` when `approvalSettled` is already true (approve and reject leave a `SignMessage` pending on purpose), and `pending = null` plus `finish(Rejected)` when it is not. `completeWith*` still does not `finish()` the Activity. `SessionTerminated` still does.

H2 is fixed. While `address` is null and the vault file exists, `offerSignMessage` assigns `waitingSign` and sets `route = Unlock`. It does not build a `Pending`. `bindWallet` sets `onMwaUnlocked` to `resumeWaitingSign`, which `unlock` calls only after `applyPlain` succeeds. The second pass reads `model.address`, which `activateAccount` set from `activeAccountId`, for both `BurnerKeys.short` and `addressesAllow`. `lock` calls `onMwaInterrupted` (`failWaitingSign`) before `failPending`.

M1 is fixed for the locked path. `resumeWaitingSign` calls `offer` while the account is already active. `offer` treats `AccountKind.Watch` as `failBeforeOffer`, sets `mwaError` to `mwa.watchAccount`, sets `route = Home`, finishes `Rejected`, and does not store `pending`. `MwaActivity` draws `WatchNotice` when there is no sign pending and `mwaError` is set. The older watch branch inside `unlock` does not run on this path, because `pending` was never stored.

L2 is fixed. `MwaPayloadsTest` asserts `declineBeforeOffer(arrayOf(ByteArray(0)))`.

### MEDIUM

#### M2 — A second sign request while locked replaces the held request — **open**

INDEX pending rules: at most one in-memory request, and a second request fails immediately and does not replace the first. `failBeforeOffer` checks `WalletModel.pending` only. While the vault is locked, the first `sign_messages` sits in `waitingSign` and `pending` is still null. A second `onSignMessagesRequest` assigns `waitingSign = request` and returns. The first `SignMessagesRequest` is never `completeWithDecline` or `completeWithSignedPayloads`. `processLaunch` can also replace the scenario with `dropTeardown` and leave the same `waitingSign` in place.

This does not reopen H1 or H2. The single-request unlock path, the address check, and real teardown are in place. Mobile Wallet Adapter usually delivers one method call at a time, so this is not HIGH. It still breaks the one-request sentence for the held slot.

## Test results (re-review 1)

Same INDEX command, from `android/`, `JAVA_HOME` = `C:\Program Files\Android\Android Studio\jbr`, `java` on `PATH` from that JBR `bin`. Exit code 0. `BUILD SUCCESSFUL` in 16s. `testDebugUnitTest` was UP-TO-DATE. Stored `MwaPayloadsTest`: 6 tests, 0 failures, 0 errors (`invalidUtf8UsesHex`, `addressListMustBeExactlyTheActiveKey`, and the empty, multi-payload, and watch cases). `lintDebug` ran.

Playground acceptance items 1–4 were **not walked** on a device. Target: none. This review does not enter a password or a mnemonic.

## Re-review 2

Checked the M2 fix only, and confirmed H1, H2, and M1 stayed closed. No product code was changed in this round.

M2 is fixed for a second sign request. In `offerSignMessage`, when `model.address` is null and the vault file exists, a non-null `waitingSign` calls `request.completeWithDecline()` and returns before `waitingSign = request`. The held `SignMessagesRequest` stays in place. Empty or multi-payload input still declines through `declineBeforeOffer` before that slot is touched, so it also does not replace the first.

H1 stayed closed. Real `onScenarioTeardownComplete` still calls `failWaitingSign()` and then `wallet.endAssociation()`. `dropTeardown` still returns first. `endAssociation` still clears `pending` through `dropSessionChrome()` when `approvalSettled` is already true, and through `pending = null` plus `finish(Rejected)` when it is not.

H2 stayed closed. A locked vault with a vault file still stores `waitingSign` and sets `route = Unlock` without building a `Pending`. `onMwaUnlocked` is still `resumeWaitingSign`, called from `unlock` only after `applyPlain` succeeds. The second pass still uses `model.address` for `BurnerKeys.short` and `addressesAllow`.

M1 stayed closed. `resumeWaitingSign` still calls `offer` after the account is active. `offer` still treats `AccountKind.Watch` as `failBeforeOffer`, sets `mwaError` to `mwa.watchAccount`, sets `route = Home`, finishes `Rejected`, and does not store `pending`.

### MEDIUM

#### M3 — Replacing the scenario while locked leaves the held request — **open**

INDEX pending rules: lock and a session that cannot continue fail the in-flight request. `processLaunch` sets `dropTeardown` and closes the previous scenario when one exists. `onScenarioTeardownComplete` returns on that flag and does not call `failWaitingSign()`. A `waitingSign` from the locked vault stays set, and that `SignMessagesRequest` is never `completeWithDecline`. A later locked `onSignMessagesRequest` then declines itself because `waitingSign` is already set, so the new request fails and the old one remains. This is the leftover clause from M2. It does not reopen M2's second-request path. Mobile Wallet Adapter usually keeps one association, so this is not HIGH.

## Test results (re-review 2)

Command, from `android/`, `JAVA_HOME` = `C:\Program Files\Android\Android Studio\jbr`, `java` on `PATH` from that JBR `bin`:

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:testDebugUnitTest
```

Exit code 0. `BUILD SUCCESSFUL` in 4s. `testDebugUnitTest` ran. Six suites, 22 tests, 0 failures, 0 errors (`HoldingRulesTest` 6, `MwaPayloadsTest` 6, `RpcResolverTest` 4, `SettingsModelsTest` 3, `SecretImportRulesTest` 1, `VaultCipherTest` 2).

Playground acceptance items 1–4 were **not walked** on a device. Target: none. This review does not enter a password or a mnemonic.

## Re-review 3

Checked the M3 fix only, and confirmed H1 stayed closed. No product code was changed in this round.

M3 is fixed. When `processLaunch` already has a scenario, it calls `failWaitingSign()`, then `wallet?.endAssociation()`, then sets `dropTeardown = true`, then `previous.close()`. `failWaitingSign` copies `waitingSign`, sets it to null, and calls `completeWithDecline`. A held locked `SignMessagesRequest` is declined and does not stay set. The later `onScenarioTeardownComplete` for that close still returns on `dropTeardown` and does not decline a second time.

H1 is not reopened. `onScenarioTeardownComplete` still returns first only when `dropTeardown` is set. A normal teardown (`dropTeardown` false) still calls `failWaitingSign()` and then `wallet?.endAssociation()`. `endAssociation` still clears `pending` through `dropSessionChrome()` when `approvalSettled` is already true, and through `pending = null` plus `finish(Rejected)` when it is not.

Non-blocking note, no new ID: a callback from the previous scenario that is already posted to the main dispatcher could still enter `offerSignMessage` after `failWaitingSign` and store a new `waitingSign`. The replacement teardown then returns on `dropTeardown` and does not clear that slot. That needs an in-flight callback to land in the window after the explicit decline and before the old scenario is gone. It does not block the main replace path, where the held request is declined before `dropTeardown`.

## Test results (re-review 3)

Command, from `android/`, `JAVA_HOME` = `C:\Program Files\Android\Android Studio\jbr`, `java` on `PATH` from that JBR `bin`:

```text
java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:testDebugUnitTest
```

Exit code 0. `BUILD SUCCESSFUL` in 3s. `testDebugUnitTest` ran. Six suites, 22 tests, 0 failures, 0 errors (`HoldingRulesTest` 6, `MwaPayloadsTest` 6, `RpcResolverTest` 4, `SettingsModelsTest` 3, `SecretImportRulesTest` 1, `VaultCipherTest` 2).

Playground acceptance items 1–4 were **not walked** on a device. Target: none. This review does not enter a password or a mnemonic.

## Prior rounds

| Round | Date | Conclusion |
|-------|------|------------|
| initial | 2026-11-07 | Open HIGH (H1, H2). Gradle passed. Playground items 1–4 not walked. |
| re-review 1 | 2026-11-07 | H1, H2, M1, L2 closed. No open HIGH. M2 open. Gradle passed. Playground items 1–4 not walked. |
| re-review 2 | 2026-11-07 | H1, H2, M1 stayed closed. M2 closed. M3 open. `:app:testDebugUnitTest` passed (22 tests, 0 failures). Playground items 1–4 not walked. |
| re-review 3 | 2026-11-07 | H1 stayed closed. M3 closed. No open HIGH. No open MEDIUM. L1 open. `:app:testDebugUnitTest` passed (22 tests, 0 failures). Playground items 1–4 not walked. |
