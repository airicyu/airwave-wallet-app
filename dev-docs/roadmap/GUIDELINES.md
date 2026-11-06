# Roadmap guidelines

How to write a version contract and the standing architecture rules. Read this before opening `dev-docs/roadmap/X.Y.Z/`.

---

## 1. A version contract is self-sufficient

Each version is a directory. **`INDEX.md` is the entry** and links to the other files in that directory.

```text
dev-docs/roadmap/X.Y.Z/
  INDEX.md
  docs/
    reasoning.md
    how.md
    design-review.md
    implementation-review.md
  HANDOFF.md
```

| File | Role |
|------|------|
| `INDEX.md` | Entry: status, read order, acceptance, out of scope, dependencies, hand-test. Open product choices go under `## Still to decide before implementation`. While that section is non-empty, do not implement |
| `docs/reasoning.md` | What ships, why, what was dropped |
| `docs/how.md` | Steps, commands, types, acceptance checks |
| `docs/design-review.md` | Written by `/version-delivery` before implementation. Not a feature list |
| `docs/implementation-review.md` | After all Tracks. Findings and what was fixed or accepted |
| `HANDOFF.md` | One file. "Done this session" plus "Next session" for the next agent |

**Read order:** `INDEX.md` → `docs/reasoning.md` → `docs/how.md` → code. Do not start from `HANDOFF.md`.

A contract is complete only when an agent with no chat can answer:

1. What is in, what is out
2. Which types, storage keys, and screens
3. What each shipped screen looks like (not only what it does)
4. What the acceptance checks are
5. What the next session should change first

If any answer depends on "we said so in chat," it is not written yet.

### Screen spec (not functions only)

If this version ships UI, **Decided** must describe the phone-frame layout in complete sentences. Linking a design-demo HTML is not a spec.

Copy from the **phone frame** of the HTML the INDEX names (and from that screen's Ship notes). For each shipped screen write:

- How many rows or blocks, and in what order
- Title and subtitle (or equivalent) for each row, in the three locales this app ships
- Which glyph: vector XML copied from web `StrokeIcon` / `TokenTrust` path `d` values, not Material Icons, not a Compose `Canvas` invention, not PNG for those glyphs
- Navigation of the chooser (one import row then a sub-chooser, if that is what the frame shows)

Do not ship a settings-style list of verbs when the frame shows icon + title + subtitle. Vault, RPC, and MWA sentences do not replace those layout sentences.

Web `airicyu/airwave-wallet` supplies IA, copy, and SVG `d` values. It does **not** supply popup pixel layout. The Android visual contract is the demo phone frame, written into this INDEX.

---

## 2. Language

Language rules live in the repo-root [`AGENTS.md`](../../AGENTS.md). Do not restate them here. Identifiers, paths, commands, JSON keys, enums, and drawable names stay English.

---

## 3. Paths

* `lib/` is the Flutter app. `android/`, `ios/`, and `macos/` are the runners. `docs/` is user-facing. `dev-docs/` is for engineers.
* The roadmap directory is **`dev-docs/roadmap/`**.
* Do not put contracts in `docs/` or `lib/`.
* Code references in `how.md` use repo-root paths, for example `lib/main.dart`.
* Do not edit `airicyu/airwave-wallet` from this repo's version work.

---

## 4. Architecture (standing)

These apply until a version's `INDEX.md` explicitly overrides one. Do not break them with a persisted in-flight approval or a hard-coded password.

1. **Pending approvals** live in memory in the process that owns the session. The UI does not scan disk hoping to find the payload. If the process dies, the in-flight approval fails.
2. **Durable state:** one local store (the store that version names) is the source of truth. The UI follows that store. Do not hand-roll a second rehydrate path as the main sync.
3. **Custody:** a password-boxed vault. No hard-coded password as a security mechanism. Unlock keys are not stored in plaintext.
4. **Request results:** a typed command plus an id. The result returns only to the originating request. No broadcast.
5. **Dependencies:** keep the third-party surface small. Adding a package needs a sentence in `reasoning.md`.
6. **Numbers:** do not use `double` for amounts, balances, fees, or simulation deltas. Chain integer units (`lamports`, token base units) are `BigInt` built from strings. Decimals use one named decimal type, chosen in the INDEX that first needs it.
7. **Optional AI:** read-only. It never touches the vault and never signs.

---

## 5. Acceptance checks

Each version's `INDEX.md` must list **hand-test** steps that a human can run. "Works in dev" is not a check. A flow that only proves storage or signing, and never names the on-screen chrome, is not enough.

At least:

* Which screen to open, and on which device
* What the UI should show, matching the Decided screen spec (row count, icons, titles, subtitles)
* What must still be true (for example, killing the app does not resurrect a pending approval)

`flutter analyze` and `flutter test` are available. The INDEX names which commands that version requires. Do not treat the scaffold widget test as proof of a wallet flow.

---

## 6. Out of scope and backlog

Each `INDEX.md` has an **Out of scope** section: what this version does not do, and where it went (another version's `INDEX`, or `backlog/`).

Backlog ideas live in [`backlog/INDEX.md`](backlog/INDEX.md). When an idea is scheduled, link it both ways. Do not leave the idea described only in the backlog.

---

## 7. Status

Each `INDEX.md` starts with:

```text
Status: draft | in progress | done | shipped
```

| Status | Meaning |
|--------|---------|
| `draft` | The contract is still being written. Do not implement. |
| `in progress` | Someone is writing code. |
| `done` | Acceptance is met and the version is not installed yet. |
| `shipped` | The user has taken this version. |

The status table in [`README.md`](README.md) is the **source of truth**. Each version's `INDEX.md` status must match that row.

**Shipping.** The user decides when a version ships. On ship, in the same change: set that row and the `INDEX` to `shipped`, and update the ship files below.

**Do not open the next version on your own.** While the previous version is not `shipped`, do not start a new version directory. If the user asks for one anyway, remind them and let them decide.

**Code changes while the latest version is `shipped` and the next is not opened.** Append the same batch to [`unversioned.md`](unversioned.md) and to the `Next` section of [`changelog.md`](../../changelog.md). Do not invent the next number. Docs, privacy policy, and terms are not tracked on the roadmap.

**Opening or continuing a version absorbs that batch.** The open version is built on top of it and ships it. Move the items into that version's INDEX and under its changelog heading. Clear `unversioned.md` and remove the changelog `Next` section. Do not leave `Next` above an in-progress version.

**More changes while a version is `in progress`.** Append that version's `INDEX` and its changelog heading. Do not open another version.

**Push.** The user pushes by hand. Shipping, editing, and committing do not include `git push`. Do not push unless the user asks.

Update [`README.md`](README.md) when a version is added or its status changes. [`changelog.md`](../../changelog.md) records what shipped. It does not replace `INDEX.md`.

### Ship files

| File | On ship |
|------|---------|
| [`version.md`](../../version.md) | The shipped version, one line. After the first ship this file equals the latest `shipped` row |
| [`pubspec.yaml`](../../pubspec.yaml) | The version name matches `version.md`. Bump `+build` only when that INDEX says so |
| [`changelog.md`](../../changelog.md) | A paragraph under that version, with the date. `Next` has no sentence that still belongs to this version |
| [`dev-docs/roadmap/README.md`](README.md) | This table's row is `shipped` |
| That version's `INDEX.md` | `Status: shipped` |
| Repo-root [`README.md`](../../README.md) | Keeps linking `version.md`. Do not paste a second copy of the number |

Until the first version is `shipped`, `version.md` is the version name being built and matches the version name in `pubspec.yaml`.

---

## 8. Privacy

**Do not write real mnemonics, private keys, passwords, or production secrets** into any tracked file. That includes:

* `how.md` steps and example commands
* `HANDOFF.md` and session notes
* Test fixtures committed to the repo
* Screenshots or logs pasted into a contract

Examples use placeholders: `test-password`, `word1 word2 … word12`, `REPLACE_WITH_TEST_PUBKEY`.

Real secrets belong only on the device or in a password manager, never in git.

---

## 9. HANDOFF

Each version has **one** `HANDOFF.md`:

* **Done this session** — files and decisions written
* **Next session** — the first thing to do, and what not to redo

When the version is `done` or `shipped`, `HANDOFF.md` may shrink to a pointer at `INDEX.md`. Do not leave a second handoff.

---

## 10. Review

How to review, who writes it, and how Tracks run: [`agent-workflow.md`](agent-workflow.md).

---

## 11. What not to write into a contract

* Chat logs ("the user said…")
* A second copy of another version's `how.md` (link it)
* Secrets (see Privacy)
* A line-by-line code review (that belongs in `implementation-review.md`, and only for the current version)
* A Chrome extension behavior copied across because the other repo already has it

---

## 12. Three day-one rules

1. **A pending approval does not live on disk.**
2. **Durable state has one store, and the UI follows that store.**
3. **Vault keys are not a hard-coded password, and they are not stored in plaintext.**
