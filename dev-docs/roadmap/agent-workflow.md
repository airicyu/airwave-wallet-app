# Agent workflow

How agents review and how Tracks run. Read [`GUIDELINES.md`](GUIDELINES.md) for the file layout and the architecture bans.

---

## 1. Roles

| Role | Writes | Does not |
|------|--------|----------|
| **Implementer** | `lib/`, platform runners, and this version's `HANDOFF.md` | Its own final review |
| **Design reviewer** | `docs/design-review.md` | Feature code |
| **Implementation reviewer** | `docs/implementation-review.md` | Feature code while reviewing (a fix is a new Track afterward) |
| **Human** | Says when it ships. Runs the hand-test in `INDEX.md` | — |

The implementer may be the same agent as a reviewer **only after a new conversation**. Do not let the writer grade their own design.

---

## 2. When to review

```text
draft  (INDEX, reasoning, how)
  → design-review.md     stop until High is fixed or the human waives it
  → implement Tracks
  → implementation-review.md
  → done
  → human hand-test
  → shipped
```

| Gate | When | Skip? |
|------|------|-------|
| **Design review** | `INDEX` is `draft` and `reasoning` and `how` are readable | No. A tiny doc typo can be one paragraph, but the file must exist |
| **Implementation review** | All Tracks for the version are done, or the human asks for a review | No, unless the version changes no code (docs only) |
| **Security review** | The version touches the vault, session, pending approval, or request routing | May be a section inside the implementation review. Use a dedicated pass when the diff is large |

`/version-delivery` spawns a new design-review subagent for that gate. The parent session writes the fixes into the contract. It does not grade its own design in the same conversation. See [`.agents/skills/version-delivery/`](../../.agents/skills/version-delivery/SKILL.md).

---

## 3. Design review

**Question:** if we build `how.md` as written, do we get a coherent product or a pile of patches?

Write to `dev-docs/roadmap/X.Y.Z/docs/design-review.md` in Traditional Chinese.

### Look for

1. **Load-bearing decisions.** Does `reasoning` name the approach and the alternatives that were dropped?
2. **Acceptance.** Can every checkbox in `INDEX` be performed by a human? Any "and so on"?
3. **Scope.** Does `how.md` smuggle in work that `INDEX` puts out of scope?
4. **Widget ownership.** For a new screen block or a new state object, which widget creates it, which widget disposes it, and does it survive the parent rebuild? See [design-principles.md](../design-principles.md).
5. **Architecture.** Pending approvals, the durable store, custody, and request results must match [GUIDELINES.md](GUIDELINES.md) section 4, or `INDEX` must name the exception.
6. **Overlap.** If this repeats an earlier version, say whether it replaces that path or runs in parallel.
7. **Hand-test.** Are the device steps one path, not "test everything"?

### Do not

* Invent a new feature.
* Review unwritten code.
* Approve with "LGTM" and no file.
* Import a Chrome extension screen because that repo already shipped it.

### Output shape

```text
# Design review — X.Y.Z

Reviewed: INDEX, reasoning, how (no HANDOFF required)

## Load-bearing decisions
## Findings
### High / Medium / Low
## Waiver
## Verdict: proceed | proceed after fixes | stop
```

**High** means do not implement until `how.md` changes or the human waives it in this file.

---

## 4. Tracks

Large work is not one undifferentiated edit.

| Track | Ships | Depends on |
|-------|--------|------------|
| **A** | Something the user can tap or run | — |
| **B, C, …** | The next slice | The previous Track is usable |

Each Track:

1. Has a section in `how.md` (or a `Track A` heading).
2. Has acceptance checks that can be run before the next Track starts.
3. Ends by updating `HANDOFF.md` (done / next).

The next Track does not start while the previous Track's acceptance is red, unless `INDEX` says they may proceed in parallel.

A single-screen patch can be one Track. Still write the heading so the review knows the boundary.

---

## 5. Implementation review

**Question:** does the code match `how.md`, and did it violate the architecture bans?

Write to `docs/implementation-review.md` in Traditional Chinese.

### Look for

1. Walk `how.md` and mark each step done, missing, or different. Differences go back into `how.md` or become findings.
2. **Pending.** Is the payload only in memory? Does killing the app fail it instead of restoring it?
3. **Storage.** Does UI state follow the one durable store named in the contract?
4. **Custody.** No hard-coded password. No plaintext key in preferences, logs, or fixtures.
5. **Results.** Is the result returned only to the originating request?
6. **Numbers.** Are lamports and token base units `BigInt`? Did a balance use `double`?
7. **Dead ends.** A function that is never called, or a storage key that is never read.

### Output shape

```text
# Implementation review — X.Y.Z

Reviewed at: <commit or "working tree">
Tracks: A, B, …

## Match to how.md
## Findings
### High / Medium / Low
## Fix log
## Verdict: accept | accept with fixes | reject
```

**High** means it does not match the contract, or it breaks a GUIDELINES ban. Fix in a follow-up, then append the fix log. Do not silently rewrite history.

---

## 6. What a review must not do

* Expand the version's product scope ("while we are here, add Mobile Wallet Adapter").
* Rewrite `reasoning` into a different product unless the human agrees.
* Treat chat as a source. If it is not in `INDEX`, `reasoning`, or `how`, it is not a requirement.
* Approve its own design in the same conversation.

---

## 7. Project addenda

* **UI copy.** Errors and empty states are one short sentence. Buttons are verbs. See [design-principles.md](../design-principles.md).
* **Widget ownership.** The parent creates and disposes. A child does not outlive the approval that spawned it.
* **Manual test.** Run the flow with `flutter run` on a device from `flutter devices`. A single screenshot is not the hand-test.
* **Dependencies.** A new package needs a sentence in `reasoning`.
* **Version delivery.** The steps and the copy-paste prompts live in `.agents/skills/version-delivery/`.
