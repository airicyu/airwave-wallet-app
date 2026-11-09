# Airwave Wallet App roadmap

Version contracts for **airwave-wallet-app**. Start here with no chat history.

**Language:** new roadmap prose is English (see the repo-root `AGENTS.md`). Paths, type names, JSON keys, and commands stay English. Shipped docs through 0.4.0 stay as they were written.

The Chrome extension roadmap in `airicyu/airwave-wallet` does not apply here.

Versions proceed one named INDEX at a time. Do not open the next number unless the user names it. Phone sketches live in [`../design-demos/`](../design-demos/README.md).

| File | Answers |
|------|---------|
| [GUIDELINES.md](./GUIDELINES.md) | How to write a self-sufficient version INDEX |
| [agent-workflow.md](./agent-workflow.md) | Design review, HANDOFF, Tracks, ship checklist |
| [../code-design.md](../code-design.md) | Kotlin file responsibility, Activities, shared lock, MWA session |
| [../DOMAIN.md](../DOMAIN.md) | Domain terms |
| [../design-principles.md](../design-principles.md) | Screen rules when INDEX is silent |
| [unversioned.md](./unversioned.md) | Code changes not yet assigned to a version |
| [../../.agents/skills/version-delivery/](../../.agents/skills/version-delivery/SKILL.md) | Deliver one version |

| Version | Status | Summary |
|---------|--------|---------|
| [0.1.0](./0.1.0/INDEX.md) | shipped | Native Android MVP: app password, generate burner, home + widget, empty tabs, MWA |
| [0.2.0](./0.2.0/INDEX.md) | shipped | Hamburger menu (empty rows + Settings + Lock); settings hub; mobile UI + blue theme |
| [0.3.0](./0.3.0/INDEX.md) | shipped | Home assets: SOL, SPL, Token-2022, Jupiter shield |
| [0.4.0](./0.4.0/INDEX.md) | shipped | Account list: 12-word, burner, import, watch. Combined row does not create |
| [0.5.0](./0.5.0/INDEX.md) | shipped | MWA connect on `MwaActivity`; unlock stays on that session; sign methods decline |
| [0.6.0](./0.6.0/INDEX.md) | shipped | MWA `sign_messages` on `MwaActivity`; no off-chain message format |
| [0.7.0](./0.7.0/INDEX.md) | in progress | MWA `sign_transactions`: legacy, v0, and v1 approval; no submit; no AI |

**Patch** (`0.1.1`, …) = bugfix for the current minor only. **Minor** (`0.2.0`, …) = new scheduled features.

Ideas: [backlog/INDEX.md](./backlog/INDEX.md) (empty until you add rows). Architecture bans: [GUIDELINES.md](./GUIDELINES.md).
