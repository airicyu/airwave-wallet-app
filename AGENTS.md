# Airwave Wallet App — agent guide

This repo is the **Airwave wallet app**. **0.1.0** through **0.4.0** are `shipped`. Work proceeds **one named version at a time**. Do not open the next version unless the user names it. Separate from the Chrome extension repo `airicyu/airwave-wallet`. Do not implement extension code here, and do not apply that repo's roadmap.

Feature ideas: [`dev-docs/roadmap/backlog/INDEX.md`](dev-docs/roadmap/backlog/INDEX.md).

**Replies to the user are Traditional Chinese written prose.** Proper names, paths, APIs, and file names stay in English.

**Documents default to English**, except **`dev-docs/roadmap/` version contracts and reviews are Traditional Chinese** (INDEX、how、reasoning、HANDOFF、design-review、implementation-review). Paths, type names, JSON keys, and commands stay English. After a version is `shipped`, leave its docs as they were written. Domain entries keep their Chinese term names. See [`.agents/skills/version-delivery/SKILL.md`](.agents/skills/version-delivery/SKILL.md).

---

## Repo layout (intent)

| Path | Role |
|------|------|
| [`android/`](android/) | **Native Android** (Kotlin / Compose for 0.1.0; scaffold when INDEX is `in progress`) |
| [`dev-docs/`](dev-docs/) | Engineering contracts, roadmap, domain |
| [`docs/`](docs/) | User-facing docs |
| [`docs/`](docs/README.md) | User docs (guide, features, legal) |
| [`dev-docs/`](dev-docs/README.md) | Engineering docs: contracts, screen rules, domain terms, local setup |
| [`dev-docs/design-demos/`](dev-docs/design-demos/README.md) | HTML phone sketches (Android visual contract; see README workflow) |
| [`changelog.md`](changelog.md) / [`version.md`](version.md) | Ship log and current version |
| [`.agents/skills/version-delivery/`](.agents/skills/version-delivery/SKILL.md) | Deliver one roadmap version |

---

## What to read first

| Situation | Read |
|-----------|------|
| Implementing a scheduled version | That version's `dev-docs/roadmap/X.Y.Z/INDEX.md` and its links. Follow [`dev-docs/roadmap/GUIDELINES.md`](dev-docs/roadmap/GUIDELINES.md) (including **Screen spec**: copy the phone frame into Decided; do not spec functions and ignore UI), [`dev-docs/roadmap/agent-workflow.md`](dev-docs/roadmap/agent-workflow.md), and [`dev-docs/code-design.md`](dev-docs/code-design.md). Screen behavior also follows [`dev-docs/design-principles.md`](dev-docs/design-principles.md) unless that INDEX says otherwise |
| User names a version or `/version-delivery` | [`.agents/skills/version-delivery/SKILL.md`](.agents/skills/version-delivery/SKILL.md) |
| Consistent screen behavior | [`dev-docs/design-principles.md`](dev-docs/design-principles.md) |
| Account, address, vault wording | [`dev-docs/DOMAIN.md`](dev-docs/DOMAIN.md). If that version's INDEX overrides a term in plain sentences, the INDEX wins and DOMAIN is updated to match |

**Priority:** a version INDEX decision > GUIDELINES architecture bans > [`dev-docs/design-principles.md`](dev-docs/design-principles.md) (screens only) and [`dev-docs/code-design.md`](dev-docs/code-design.md) (file shape). Sequence: [`dev-docs/roadmap/README.md`](dev-docs/roadmap/README.md) table; only the version the user named.

---

## Architecture rules

Details are in [`dev-docs/roadmap/GUIDELINES.md`](dev-docs/roadmap/GUIDELINES.md). These hold until an INDEX explicitly replaces one:

1. **Pending approvals** live in memory in the process that owns the session. The UI does not scan disk hoping to find the payload. If the process dies, the in-flight approval fails.
2. **Durable state:** one local store (the store that version names) is the source of truth. The UI follows that store. Do not invent a second hand-written rehydrate path as the main sync.
3. **Custody:** a password-boxed vault encrypted with the user's password. No hardcoded password as a security mechanism. Unlock key material is not persisted in plaintext.
4. **Request results:** a typed command plus an id. The result returns only to the originating request. Do not broadcast it.
5. **Dependencies:** keep third-party packages out unless the reason is necessary and stated.
6. **Numbers:** do not add, subtract, multiply, or divide amounts, balances, fees, or simulated deltas with `double`. Chain integer units (lamports, token base units) are `BigInt` built from strings. Decimals use one named decimal type, chosen in the INDEX that first needs it.
7. **Optional AI:** read-only. It never touches the vault and never signs.

Even a vertical slice must still match the approval lifecycle, durable-state sync, and custody rules above. Do not "get it running" with a hardcoded password or by persisting an in-flight approval.

---

## Product principles

Shared intent with the Chrome extension; this app does not share its code. Full wording: [`docs/product-principles.md`](docs/product-principles.md).

1. Non-custodial: keys stay on device.
2. Password-boxed vault; no hardcoded password as security.
3. Before approve, show what the user is signing as clearly as practical.
4. No proprietary backend; use public RPCs / APIs the user configures (e.g. Helius).
5. Optional AI analysis is read-only and never signs.

Mobile Wallet Adapter and dApp deep links are not started until an INDEX schedules them.

---

## Working habits

- **Do not commit unless the user asks.** **Do not push unless the user asks.** The user pushes by hand. Shipping and committing do not include a push.
- Roadmap and review reports must **not** contain real mnemonics, private keys, passwords, or production secrets (see GUIDELINES privacy).
- Implementation stays in this repo. Do not assume the Chrome extension repo must change.
- Anything discussed in chat but not written into the roadmap or a contract file **does not exist** for the next agent. Decisions go into files.

### Cutting a version

Version status lives in the table in [`dev-docs/roadmap/README.md`](dev-docs/roadmap/README.md). Each INDEX status matches that row.

Until the first version is `shipped`, [`version.md`](version.md) is the version name being built, and it matches the version name in [`pubspec.yaml`](pubspec.yaml). After the first ship, `version.md` equals the latest `shipped` row in that table, and the `pubspec.yaml` version name matches it.

A version proceeds by stating the work, writing it into that version's proposal (INDEX), then changing code. While that happens the version is `in progress`. Only after the user explicitly says ship: mark that README row `shipped`, match the INDEX, and check that [`changelog.md`](changelog.md) has a paragraph for that version. Do not mark shipped, and do not open the next version number, unless the user says ship.

If the previous version is not `shipped`, do not open a new version. This is a soft rule: remind the user, do not hard-block. If they still want it opened, open it.

When the latest version is `shipped` and the next version is not opened, new **code** work is unversioned. Record it in [`dev-docs/roadmap/unversioned.md`](dev-docs/roadmap/unversioned.md) and add one sentence under `Next` at the top of the changelog. Both places list the same changes. The user later chooses: fold them into the version just shipped, open the next version for them, or leave them unversioned after shipping. Do not guess before they choose. Docs, privacy policy, and terms are not tracked on the roadmap.

More changes while a version is in progress go into that version's INDEX and under that version's changelog heading. Do not open the next version.

---

## Links

| Doc | Use |
|-----|-----|
| [README.md](README.md) | Project home (for people) |
| [docs/user-guide.md](docs/user-guide.md) | User guide |
| [docs/features.md](docs/features.md) | Current features |
| [docs/product-principles.md](docs/product-principles.md) | Product principles |
| [dev-docs/README.md](dev-docs/README.md) | Engineering doc map |
| [dev-docs/setup.md](dev-docs/setup.md) | Local run |
| [docs/README.md](docs/README.md) | User doc map |
| [dev-docs/design-principles.md](dev-docs/design-principles.md) | Screen rules |
| [dev-docs/DOMAIN.md](dev-docs/DOMAIN.md) | Domain terms |
| [dev-docs/roadmap/README.md](dev-docs/roadmap/README.md) | Roadmap entry |
| [dev-docs/roadmap/GUIDELINES.md](dev-docs/roadmap/GUIDELINES.md) | How to write a self-sufficient version contract |
| [dev-docs/roadmap/agent-workflow.md](dev-docs/roadmap/agent-workflow.md) | Review gates and Track flow |
