# 0.2.0 — how

Paths are repo-root relative. Implement under `android/`. Visual reference: [`dev-docs/design-demos/wallet-020.html`](../../../design-demos/wallet-020.html) (Android phone sketch; IA + tokens taken from **live** extension popup code).

## Extension UI reference (read code, not stale demos)

When checking what the extension looks like or which labels/order/fields ship today, read:

| Area | Path in `airicyu/airwave-wallet` |
|------|----------------------------------|
| Menu order / Lock | `wallet/src/popup/PopupMarkup.tsx` (`MenuItems`) |
| Network (cluster) + Settings hub + RPC / keys / CU / password | `wallet/src/popup/settings/SettingsScreens.tsx` (`NetworkScreen`, `RpcScreen`, `KeysScreen`, …) |
| API key row UX | `wallet/src/popup/settings/SecretKeyField.tsx` |
| AI settings | `wallet/src/popup/settings/TxAiSettingsScreen.tsx` |
| Color tokens | `wallet/src/popup/style.css` (`:root`) |
| English / locale strings | `wallet/src/shared/ui-messages.ts` |
| Settings shape | `wallet/src/shared/storage-keys.ts` |

Do **not** use that repo’s `dev-docs/design-demos/` as the source of truth — many demos there are stale and uncleaned. This app’s `wallet-020.html` is the Android contract for 0.2.0.

## Goal

Hamburger + Network (cluster) + empty placeholders + Settings persistence + theme tokens + Lock from the menu, without breaking 0.1.0.

Product IA and field set mirror the live Chrome extension hamburger, Network screen, and Settings hub. Do not port extension TypeScript, CSS layout, or inject code.

## Theme tokens (normative)

Same family as live extension `wallet/src/popup/style.css` `:root`:

| Token | Hex |
|-------|-----|
| `bg` | `#0f1419` |
| `bgElevated` | `#161c24` |
| `fill` | `#1e2630` |
| `stroke` | `#2a3542` |
| `strokeSoft` | `#222a34` |
| `text` | `#e8eef4` |
| `textMuted` | `#9aa7b4` |
| `textDim` | `#6b7785` |
| `accent` | `#3d9cf0` |
| `accentDim` | `#2a6ea8` |
| `danger` | `#c75050` |
| `ok` | `#4ecb8d` |
| `warn` | `#e8b84a` |

Wire Material 3 `colorScheme` from these. Primary buttons and selected accents use `accent`. Radius ~12 dp elevated cards / ~8 dp chips (phone-scaled; not popup chrome).

## Durable settings store

One store. Key **`airwave.settings.v1`** (same name as the extension settings key). Implementation: **EncryptedSharedPreferences** (AndroidX Security) holding one JSON blob. The UI reads that store only — no second hand-written rehydrate path.

Vault stays **`airwave.vault.v1`** from 0.1.0. Settings never hold the password or seed.

### JSON shape (product fields that apply on Android)

```text
{
  "cluster": "devnet" | "mainnet",
  "rpcByCluster": {
    "devnet":  { "urls": string[], "active": string },
    "mainnet": { "urls": string[], "active": string }
  },
  "heliusApiUrl": string,
  "jupiterApiKey": string,
  "openRouterApiKey": string,
  "txAiAnalysisEnabled": boolean,
  "openRouterReportModelMode": "default" | "custom",
  "openRouterReportModelChain": string[],
  "openRouterDecisionsModelMode": "default" | "custom",
  "openRouterDecisionsModelChain": string[],
  "defaultCuPrice": integer,   // micro-lamports / CU; Kotlin Long only — never double
  "locale": "en" | "zh-Hant" | "zh-Hans",
  "idleAutoLockEnabled": boolean,
  "idleAutoLockMinutes": integer   // Kotlin Int; 1…1440
}
```

Defaults: `cluster` = `devnet`; empty custom RPC lists; empty secrets; `txAiAnalysisEnabled` = `true`; model modes `default` with empty custom chains; `defaultCuPrice` = `25000`; `locale` = `en`; `idleAutoLockEnabled` = `true`; `idleAutoLockMinutes` = `10`.

Public / derived for UI (do not persist as separate keys): `heliusConfigured`, `jupiterConfigured`, `openRouterConfigured` booleans from non-empty trimmed secrets (Helius: URL parses to a usable target; keys: non-empty string).

Omit extension-only fields: `shell`, `rpcGuideDismissed`, legacy single-model migrate fields, `rpcUrl` denormalized cache.

There is **no** `testnet` entry in `rpcByCluster` / Network UI. Testnet submit uses the public URL only (below).

### RPC resolution (sign-and-send submit)

**Request chain wins.** Network settings `cluster` does **not** override a mismatched request. Continues 0.1.0 “map the request chain” rule; settings only supply optional custom endpoints per mapped cluster.

1. Read the MWA request’s chain (or legacy cluster field). Map to a cluster key:

| Request chain (or legacy) | Cluster key |
|---------------------------|-------------|
| `solana:mainnet` / `mainnet-beta` | `mainnet` |
| `solana:devnet` / `devnet` | `devnet` |
| `solana:testnet` / `testnet` | `testnet` (public only) |
| anything else | unknown → **fail before signing** |

2. If cluster key is `testnet`: use `https://api.testnet.solana.com` (ignore Network UI).
3. If cluster key is `devnet` or `mainnet`:
   - If `rpcByCluster[key].active` is a non-empty **allowed** custom URL → use it.
   - Else if `devnet` → `https://api.devnet.solana.com`.
   - Else if `mainnet` and active empty → **unset: fail before signing**.
4. Allowed custom URL: `https` any host, or `http` only for `localhost` / `127.0.0.1`; no userinfo in URL.

Network screen `cluster` still drives in-app summaries / “Current” badge on RPC cards; it is not the submit override.

`defaultCuPrice`: persist and show in 0.2.0. Later send / fee paths may read it; do not invent new fee math with `double`. Parse / store as non-negative `Long` in `0…1_000_000_000`.

## Session / Lock

Unchanged from 0.1.0: process memory holds unlock + secrets. **Menu → Lock wallet** clears the in-memory session (same as Home Lock in 0.1.0). **Unlock screen chrome** matches extension `.unlock-screen` (radial background, brand mark, Airwave title, `unlock.lead` / placeholder / primary button) — see [`wallet-020.html`](../../../design-demos/wallet-020.html) **Unlock**. Home top bar no longer shows a Lock text button; Lock lives only in the hamburger (extension pattern).

## Navigation shell

### Home top bar

- Leading: title `Assets` / `Activity` (tabs unchanged).
- Trailing: icon-only **Menu** (hamburger). Accessible name `Menu`. Hit target ≥ 44 dp.
- Address widget + empty tabs stay as 0.1.0.

### Hamburger (phone modal sheet)

On phone, use a **full-width top sheet** under the status bar (not a side drawer or extension popup dropdown): dimmed scrim over Home; sheet content height follows menu rows with rounded bottom corners. **Status-bar insets** on the sheet. Top-leading **Back** dismisses the sheet only (see [`design-principles.md`](../../../design-principles.md)). Visual reference: [`wallet-020.html`](../../../design-demos/wallet-020.html) screen **Home — menu open**. Same seven-row order and **leading icon** per row as extension `MenuItems` (not text-only).

**Ownership:** the Home (or shell) composable that owns the Menu button owns `menuOpen` and the overlay + sheet. Opening Menu sets `menuOpen = true`. Choosing any destination or Lock, or tapping the scrim, sets `menuOpen = false` **before** navigation / lock. The sheet is not kept above later routes after dismiss.

Seven rows, this order (English labels; **leading icon** on each row):

1. Wallet accounts → empty placeholder  
2. Address book → empty placeholder  
3. Network → **Network screen** (cluster pick)  
4. Settings → Settings hub  
5. Connected sites → empty placeholder  
6. About this app → empty placeholder  
7. Lock wallet → lock session (separator above this row)

**Back:** empty / Network / Settings hub / settings subpages use a Back control that pops to the previous screen (Home for first push from the menu; Settings hub for settings children). Optional Menu on subpage top bars is allowed (extension pattern); empty-state body rules unchanged.

### Empty placeholder

Reusable screen: title = menu label; body = one short English empty sentence; Back returns as above. No lists, no CRUD. Used only for the empty menu rows above (not Network).

### Network screen

Mirror extension `NetworkScreen`:

- Radio group: **Devnet** / **Mainnet** → writes `cluster` only. **Do not show RPC names or URLs** on this screen (extension-aligned).
- Does **not** add, edit, or select RPC URLs (that is Settings → RPC).

### Settings hub

Rows (label + summary on the right, extension hub pattern):

| Row | Opens | Hub summary |
|-----|-------|-------------|
| Language | Locale picker | Endonym of current locale |
| RPC | Per-cluster RPC endpoints only | **Omit** |
| API keys | Helius + Jupiter secret fields | **Omit** |
| AI Settings | Toggle + OpenRouter + model chains | **On** or **Off** only |
| Default CU price | Integer field | Current integer as string |
| Wallet lock settings | Idle auto-lock + wallet lock password | **Omit** |

### Settings sub-screens

**Language.** Radio list: `English` / `繁體中文` / `简体中文` (`en` / `zh-Hant` / `zh-Hans`). Changing locale reloads UI strings from the catalog. Survives process kill.

**RPC.** Mirror extension `RpcScreen` (URL only — **no separate RPC name**). Two cluster cards (**Devnet** / **Mainnet**):

| Element | Behavior |
|---------|----------|
| Card header | Cluster label; **Current** badge on the card whose cluster equals settings `cluster` (not a cluster picker) |
| Built-in row | Devnet only: public `https://api.devnet.solana.com`, radio selectable, **Built-in** pill, **no delete** |
| Custom rows | Radio + full URL (monospace, ellipsize); **trash** removes from `urls`; if removed URL was `active`, clear `active` |
| Selected row | Accent radio + subtle row highlight |
| **+** (card footer) | Opens one **add** row: input placeholder `https://`, **Confirm** + **Discard**; allowed URL appends to `urls` and may set `active` |
| Edit custom URL | Tap a custom row to edit inline (same confirm/discard pattern as add); replaces that URL in `urls` and updates `active` if needed |
| Mainnet | Custom endpoints only until user adds; no built-in row |

Persist into `rpcByCluster` only. Allowed URL rules unchanged (HTTPS, or HTTP localhost/127.0.0.1).

**API keys / OpenRouter secret rows.** Layout family matches extension `SecretKeyField` + `.keys-input-row` CSS (Helius, Jupiter, OpenRouter). **Action set in edit mode follows the wanted UX**, not today’s extension bug (eye still present while editing):

**Layout (do not invent a combined control):** each row is a horizontal flex — a **standalone bordered input** (`flex: 1`) then **ghost icon buttons outside the input** (`gap: 4`, no shared outer border around icons). Label above (`0.78rem`, dim). Mask string when configured and not revealed: exactly `••••••` (`MASKED_SECRET_DISPLAY`).

| Phase | Input | Actions to the right of the input |
|-------|-------|-----------------------------------|
| Idle, unset | Empty; placeholder Optional | Eye (Show), Pencil (Edit) |
| Idle, configured | Readonly `••••••` (or plaintext if revealed) | Eye toggles reveal without editing; Pencil enters edit |
| Loading | Brief while secret is read for edit | — |
| Editing | Editable; shows the draft (value being changed); accent border | **Check** (Confirm) persists then idle; **X** (Discard) cancels then idle. **No eye** in edit — reveal is idle-only |

Rules: only one secret row may be editing at a time (claim/release). Blur while editing cancels. Empty confirm may clear the stored secret. Never log plaintext. Accessible names: Show / Edit / Confirm / Discard.

**AI Settings.** Mirror extension `TxAiSettingsScreen` (persist only in 0.2.0 — no assess / no signing). AI remains read-only and never touches the vault.

1. Enable switch → `txAiAnalysisEnabled`.
2. OpenRouter API key → same secret-field control as API keys.
3. Two model-chain blocks (Report model / Grading model), each with a **Default | Custom** segment and up to **5** slots:

| Mode | Slot UI | Persistence |
|------|---------|-------------|
| **Default** | Show the **hardcoded** chain from app source (read-only / disabled inputs). Not editable by the user | Persist `mode = default`. Custom chain array may stay as last custom draft or empty; runtime uses the hardcoded list when mode is default |
| **Custom** | Five text slots the user can edit (OpenRouter model ids) | Persist `mode = custom` and the normalized custom chain (trim, drop empties, max 5) |

Hardcoded default chains for 0.2.0 (same strings as extension `wallet/src/shared/tx-ai.ts` at contract time; change only with INDEX approval):

- Report: `inclusionai/ling-3.0-flash`, `nvidia/nemotron-3.5-lightning`, `~deepseek/deepseek-flash-latest`
- Grading (decisions): `liquid/d1`, `nace-ai/drex-v1.5`

Display pads to 5 slots with empty trailing cells when the chain is shorter.

**Default CU price.** Label `micro-lamports / CU`. Non-negative integer field; invalid input reverts to last saved.

**Wallet lock settings.** Title `Wallet lock settings`. Two cards:

1. **Idle auto-lock** — switch (persist `idleAutoLockEnabled`, default **on**). **Lock after (minutes)** numeric field (persist `idleAutoLockMinutes`, default **10**, clamp `1…1440`). When enabled and the user is unlocked, no user interaction for that many minutes → same as **Lock wallet** (clear in-memory session). Timer does not run on Setup/Unlock/Lead/Generate.
2. **Wallet lock password** — row label + **Change** on the right; expanding or inline form with current + new + confirm. Min length 8. Re-box vault (Argon2id + AES-GCM). Wrong current fails. After success: update in-memory wallet password and **keep session unlocked**. Never store plaintext password outside the password box.

## English copy (normative)

| Key | String |
|-----|--------|
| menu.menu | Menu |
| menu.accounts | Wallet accounts |
| menu.addressBook | Address book |
| menu.network | Network |
| menu.settings | Settings |
| menu.connectedSites | Connected sites |
| menu.about | About this app |
| menu.lock | Lock wallet |
| menu.back | Back |
| empty.accounts | No wallet accounts yet. |
| empty.addressBook | No addresses yet. |
| empty.connectedSites | No connected sites yet. |
| empty.about | About content coming soon. |
| network.title | Network |
| network.devnet | Devnet |
| network.mainnet | Mainnet |
| network.mainnetUnset | Not set |
| settings.title | Settings |
| settings.language | Language |
| settings.rpc | RPC |
| settings.apiKeys | API keys |
| settings.ai | AI Settings |
| settings.cuPrice | Default CU price |
| settings.walletLock | Wallet lock settings |
| settings.idleAutoLock | Idle auto-lock |
| settings.lockAfterMinutes | Lock after (minutes) |
| settings.walletLockPassword | Wallet lock password |
| settings.change | Change |
| settings.rpcCurrent | Current |
| settings.keysConfigured | Configured |
| settings.keysPartial | Partially set |
| settings.keysUnset | Not set |
| settings.cuLabel | micro-lamports / CU |
| settings.helius | Helius API URL |
| settings.jupiter | Jupiter API key |
| settings.openRouter | OpenRouter API key |
| settings.aiOn | On |
| settings.aiOff | Off |
| settings.aiReportModel | Report model |
| settings.aiGradingModel | Grading model |
| settings.aiModeDefault | Default |
| settings.aiModeCustom | Custom |
| common.show | Show |
| common.edit | Edit |
| common.confirm | Confirm |
| common.discard | Discard |
| common.optional | Optional |
| password.current | Current password |
| password.new | New password |
| password.confirm | Confirm new password |
| password.save | Save |
| password.tooShort | Password is too short. |
| password.mismatch | Passwords do not match. |
| password.wrong | Wrong password. |

Default locale `en`. For `zh-Hant` / `zh-Hans`, resolve each Android key through the mapping below to extension `wallet/src/shared/ui-messages.ts` (live strings). Keys with no mapping stay English in those locales for 0.2.0.

| Android key | Extension `ui-messages` key (prefer) |
|-------------|--------------------------------------|
| menu.menu | menu.menu |
| menu.accounts | menu.walletAccounts |
| menu.addressBook | menu.addressCards |
| menu.network | settings.hub.network |
| menu.settings | menu.settings |
| menu.connectedSites | menu.connectedSites |
| menu.about | menu.about |
| menu.lock | menu.lockWallet |
| menu.back | menu.back |
| empty.accounts | (English `No wallet accounts yet.` / zh: 還沒有錢包帳戶) |
| empty.addressBook | addressCards.empty |
| empty.connectedSites | (English `No connected sites yet.` / zh-Hant: 還沒有已連線網站) |
| empty.about | (English `About content coming soon.` / zh-Hant: 關於內容稍後提供) |
| network.title | settings.hub.network |
| network.devnet | (literal Devnet) |
| network.mainnet | (literal Mainnet) |
| network.mainnetUnset | settings.rpc.mainnetUnset |
| settings.title | menu.settings |
| settings.language | settings.hub.language |
| settings.rpc | settings.hub.rpc |
| settings.apiKeys | settings.hub.apiKeys |
| settings.ai | settings.hub.txAi |
| settings.cuPrice | settings.hub.cuPrice |
| settings.password | settings.hub.walletPassword |
| settings.change | settings.hub.changePassword |
| settings.rpcCurrent | settings.rpc.current |
| rpc.builtIn | Built-in |
| rpc.urlPlaceholder | `https://` |
| settings.keysConfigured | settings.hub.keysConfigured |
| settings.keysPartial | settings.hub.keysPartial |
| settings.keysUnset | settings.hub.keysUnset |
| settings.cuLabel | (English `micro-lamports / CU`) |
| settings.helius | (English `Helius API URL`) |
| settings.jupiter | (English `Jupiter API key`) |
| settings.openRouter | settings.openRouter.apiKey |
| settings.aiOn | settings.txAiAnalysis.enable |
| settings.aiOff | settings.txAiAnalysis.off |
| settings.aiReportModel | settings.openRouter.model |
| settings.aiGradingModel | settings.openRouter.decisionsModel |
| settings.aiModeDefault | settings.txAiAnalysis.modeDefault |
| settings.aiModeCustom | settings.txAiAnalysis.modeCustom |
| common.show | common.show |
| common.edit | common.edit |
| common.confirm | common.confirm |
| common.discard | common.discard |
| common.optional | common.optional |
| password.* | mirror extension change-password copy where present; else English |

Parenthetical zh fallbacks above are normative when no extension key exists.

## Suggested tracks

### Track A — Theme tokens

1. Compose color tokens table above.  
2. Material 3 colorScheme from tokens.  
3. Apply to Home top bar, primary buttons, menu accents, Settings surfaces.

### Track B — Hamburger shell + Network

1. Replace Home Lock text button with Menu icon.  
2. Modal menu with seven rows in order.  
3. Empty destinations via `EmptyPlaceholderScreen` (accounts / address book / connected sites / about).  
4. Network screen: cluster radio → `cluster` in settings store.  
5. Settings → Track C. Lock → existing session clear.

### Track C — Settings store + screens

1. EncryptedSharedPreferences JSON at `airwave.settings.v1`.  
2. Hub + six sub-screens; RPC has no cluster picker; secret fields use SecretKeyField phases.  
3. Password change re-boxes vault.  
4. Wire MWA sign-and-send submit to RPC resolution (**request chain** → cluster → settings endpoints / public defaults).  
5. Unit tests: settings round-trip; cluster switch; password change; CU parse bounds.

### Track D — Wire + regression

1. `gradlew.bat :app:assembleDebug` / `testDebugUnitTest` / `lintDebug`.  
2. Hand-tests A–E on Android Studio emulator.  
3. `docs/implementation-review.md` after tracks (Traditional Chinese per workflow).

## Dependencies

No new network SDK. May already have AndroidX Security from 0.1.0 vault wrap; EncryptedSharedPreferences for settings is the same family. If a new artifact is required, name it in `reasoning.md` when pinned.

## Non-goals in code

- Do not port extension popup CSS / 384px shell.  
- Do not implement real account switcher, address-book CRUD, connected-sites revoke, or about content.  
- Do not put cluster selection on the RPC screen.  
- Do not persist pending MWA approvals.  
- Do not run transaction AI assess in 0.2.0 (settings persist only).
