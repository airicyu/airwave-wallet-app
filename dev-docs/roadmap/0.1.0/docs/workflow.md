# 0.1.0 — app workflow

Product flow for the MVP. Visual layout is **not** this file — that is the designer’s **design demo**. If the demo and this file disagree, update one of them in the same change so they match.

Stack context: native Android, Jetpack Compose — [`how.md`](./how.md).

## Delivery order

1. **This workflow** — agreed product paths (this file + INDEX **Decided**).
2. **Design demo** — designer produces screens for the paths below (HTML or equivalent under `dev-docs/design-demos/`).
3. **Tech design review** — `docs/design-review.md` per [`agent-workflow.md`](../../agent-workflow.md). Stay **`draft`** until verdict is proceed.
4. **Scaffold** — create `android/`, then implement. Do not scaffold before step 3 passes.

## Surfaces

| Surface | Role |
|---------|------|
| **App password** | One password for the app vault. Set on first launch. Not asked again when creating an account. |
| **Create burner** | After the vault exists and the app is unlocked: generate a new burner. No password fields on this screen. |
| **Unlock** | Password gate when a vault already exists and the session is locked. |
| **Home** | Shell after unlock: **wallet widget** plus **Assets** and **Activity** tabs. |
| **MWA approval** | Shown when a dApp session needs a user decision (`authorize`, `sign_messages`, `sign_and_send_transactions`). Not a tab. |

Assets and Activity are tabs on Home. **Neither tab has list or chain content in 0.1.0** (empty shell only). The wallet widget lives on Home and stays visible across both tabs.

## Paths

### First launch (no vault)

Set the **app password** (enter twice) → empty vault, session unlocked.

There is no account yet. Home shows a single lead: **Create burner wallet**. That opens the generate screen.

### Generate burner

Same idea as the Chrome extension’s burner screen, cut down to this version:

- Shows a preview public address (short; copy copies the full address).
- **Roll again** replaces the preview before it is saved.
- **Generate** saves that key into the already-unlocked vault.
- No wallet-password fields. No seed backup. No name field in 0.1.0.

Then Home with the wallet widget.

### Later launch (vault exists)

Unlock with the **app password** → Home.

Wrong password: stay on Unlock; one short error. No vault contents shown.

If the vault is unlocked but still has no account, Home is the same lead as after first launch (**Create burner wallet**).

### Home

- Wallet widget shows this account’s identity (short address; copy yields the full address — per [`design-principles.md`](../../../design-principles.md)).
- User switches **Assets** / **Activity**. Each tab body is an empty state (one short sentence). No balances, no history rows.
- **Lock** (from Home) drops the in-memory session and returns to Unlock. The app password still protects the vault.

### dApp session (MWA)

Protocol: **Mobile Wallet Adapter** wallet endpoint. Normative spec: [Mobile Wallet Adapter 2.0](https://solana-mobile.github.io/mobile-wallet-adapter/spec/spec.html). Android behavior follows [Solana Mobile docs](https://docs.solanamobile.com/developers/mobile-wallet-adapter).

This app does **not** define its own deeplink scheme (no Phantom- or Solflare-style custom protocol). Association uses the spec’s **`solana-wallet:`** URI. The app registers for the local association form so the system can launch it:

`solana-wallet:/v1/associate/local?association=…&port=…&v=…`

On that launch the wallet starts a **localhost-only** WebSocket (path `/solana-wallet`) for the session. That registration is the “register” step — an Android intent filter, not a separate user screen.

**Session methods this workflow includes** (mainstream wallet endpoint):

| Method | User-facing |
|--------|-------------|
| `authorize` | Connect approval. Success returns `auth_token` and this account (address). `auth_token` may allow a later silent re-prompt skip when the spec says so. |
| `deauthorize` | Drop that dApp’s authorization. |
| `get_capabilities` | No extra product screen; answer from the implementation. |
| `sign_messages` | Approval showing what will be signed. **Mandatory** feature `solana:signMessages`. |
| `sign_and_send_transactions` | Approval showing the transaction as clearly as practical, then sign and submit. **Mandatory** feature `solana:signAndSendTransaction`. |

`sign_transactions` is **deprecated** in spec 2.x. Support it only if we explicitly keep backward compatibility; it is not a second product flow. **Sign In With Solana** (`solana:signInWithSolana`) and `clone_authorization` are optional and **out of 0.1.0** unless INDEX adds them.

Rules for every in-flight MWA request that needs UI:

1. If the session is locked, Unlock first, then show approval.
2. **Approve** completes that request to the originating dApp only.
3. **Reject** fails that request; Home is unchanged.
4. **Lock** on the approval surface clears the in-memory session and **fails** the in-flight request.
5. At most one pending approval in memory. Not written to disk. Process death fails it. A second request while one is pending fails only the second.

Cluster / chain on `authorize` (`solana:mainnet`, `solana:devnet`, `solana:testnet`, or legacy cluster names) does not change which key we return in 0.1.0 (one account). It is shown on the approval when the dApp sent it.

## Out of this workflow

Import of a seed or private key, seed phrase backup UI, balances, transfers, activity history, multi-account switcher, iOS, a proprietary wallet URL scheme, remote reflector / QR association, Bluetooth, Nostr.
