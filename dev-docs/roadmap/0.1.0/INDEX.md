# 0.1.0 — MVP

- **Status:** `draft`
- **Parent:** (none)
- **Changelog:** [`changelog.md`](../../../changelog.md) under `0.1.0 (unreleased)`

## Product (agreed)

Only this scope is decided:

1. **Import burner wallet** — user can bring in a burner wallet (details TBD).
2. **Wallet home** — includes a **wallet widget** on the home surface.
3. **Assets** and **Activity** — bottom (or equivalent) tabs exist; **no tab content yet** (empty shells).
4. **dApp connect** — a dApp can connect to the wallet (protocol and platform TBD).

Not the Chrome extension inject / Wallet Standard stack.

## Read order

1. This file
2. [`docs/reasoning.md`](./docs/reasoning.md) and [`docs/how.md`](./docs/how.md) when they exist beyond placeholders

## Decided

| Topic | Decision |
|-------|----------|
| Scope | The four bullets in **Product** only |

## Still to decide

Everything needed to implement and accept 0.1.0: platform, stack, vault/custody, import UX, connect protocol (e.g. MWA), navigation copy, tests, and hand-test steps. Add rows to **Decided** and flesh out `how.md` before **`in progress`**.

## Out of scope

Anything not listed under **Product**. No separate backlog entries yet — add ideas to [`backlog/INDEX.md`](../backlog/INDEX.md) when you schedule them.

## Acceptance

Not written yet. Define commands and hand-tests when the engineering contract is ready.

## Design review gate

Stay **`draft`** until [`docs/design-review.md`](./docs/design-review.md) exists and passes per [`agent-workflow.md`](../agent-workflow.md). Then set **`in progress`** and implement.
