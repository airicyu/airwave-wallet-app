# Code design

How Kotlin under `android/` is split and commented. An INDEX overrides this file when it says so. Screen layout lives in [design-principles.md](./design-principles.md). Architecture bans live in [roadmap/GUIDELINES.md](./roadmap/GUIDELINES.md). Version order lives in [roadmap/README.md](./roadmap/README.md): one named INDEX at a time.

The Chrome extension's folder tree (`background/`, `popup/`, `shared/`), command-file line cap, and CSS catalogs do not apply here. Responsibility, size, security, and smallest-design rules follow that repo's `dev-docs/code-design-principles.md`, tightened for this app: **every named type** has a KDoc on its declaration, not a floating file header.

## 1. Smallest design

Smallest design applies only when section 2 is already satisfied. With no security hole, make the smallest change that meets this contract: one straight path a reader can maintain. A leak, an injection, or an unauthorized sign path is not a place to keep the diff small. That fix is required, and its size follows the hole.

- Do not add a layer, mode, fallback, or framework the INDEX did not ask for, once section 2 is already satisfied.
- Do not walk records one by one when one pass does the same work. Extra per-item handling that the contract does not require is harder to maintain and slower.
- Do not build the next case early. Add it when a contract asks for it.
- Do not shrink, defer, or skip a fix that section 2 requires. The architecture bans in GUIDELINES stay in force.

## 2. Security

This app holds keys. A hole that leaks a secret, lets another app sign, or injects untrusted data into a privileged surface has serious consequences. Find that hole in the design and close it in this change. Section 1 does not limit that work.

- Treat Intent extras, deep links, page or dApp content, storage values, RPC and simulation results, and model output as untrusted.
- Do not concatenate that input into HTML, a script, a URL, or a command string. Do not pass it through an evaluator.
- An incoming Intent or message is not permission to sign, export keys, or change the vault. Keep the origin check and the approval path this repo already uses.
- Do not log, display in full, or persist a mnemonic, a private key, or the unlock secret.
- Close the hole before calling the design small. When a check this repo already has closes it, use that check. When it does not, add the check the hole needs.

## 3. One responsibility

A file does one thing a reader can name in a sentence. The neighboring step (the caller, the I/O, the next screen) is a different file.

- Do not grow a file so that one comment has to say "and". If the comment needs a second responsibility, split.
- Do not paper over a second responsibility with `related logic`, `etc.`, or `and so on`.
- Do not split one responsibility into one file per function unless that function is the whole responsibility.
- A pure data table file only holds that table.
- Tests sit next to the module they exercise, under `src/test`.

## 4. Responsibility comment

Put English KDoc **on the declaration** of every named type under `android/` (main and test): `class`, `data class`, `enum class`, `object`, `sealed class`, `interface`.

Write **complete sentences** a person can read aloud, as a short paragraph. Length follows the type, not a sentence quota. A one-line enum or DTO needs one sentence. A boundary type (Activity, session owner, protocol adapter, password box) may use a short paragraph so the reader knows why it exists, what it owns, and what the neighboring step does instead. Do not pad a simple type to a paragraph. Do not write a spec, a method catalog, or a changelog. At most **150 words**. If you need more, the type is doing two jobs: split it.

Do not use headline fragments, colon lists, or "X vs Y" as the whole comment.

- If the comment needs a second different responsibility, split the type or the file. Do not lengthen the comment.
- If you edit a type that has no KDoc, add it in the same change.
- Nested types count. Nested `enum class` / `data class` / `object` get their own KDoc, in the same prose. Nested types stay short.
- Do not keep a file-level comment above `package` when the file has a named type. The type KDocs are the only responsibility comments. Do not copy the same sentences onto every type in the file.
- A file that has **no** named type (only top-level functions, including Compose screens and `WalletModel` extensions) keeps one file-level KDoc instead, written the same way.
- A string-catalog `object` says it is the catalog, not the list of keys.
- Do not require a comment on every method. Do not restate the type or function name in prose.

## 5. Line count

The threshold stays **400**. It is not a higher cap. Count **code lines** only: physical lines that have code. **Blank lines do not count.** Comment lines do not count: a full-line `//` comment, and every line of a `/* */` or `/** */` block (including the file header). A line that mixes code and an end-of-line `//` comment still counts as one code line.

- A new `.kt` file stays at or under 400 lines.
- If a change touches a file that is already over 400, that version either splits it, or the INDEX names the file, says this version does not split it, and gives the reason.
- One screen that is still a single responsibility may stay over 400 when the INDEX says so. The next structure pass records that reason. It does not split the file only because the line count is high.
- `android/app/src/main/kotlin/com/airwave/wallet/i18n/AppStrings.kt` is the message catalog. It may exceed 400 lines. That exception is this file. The `AppStrings` object still needs the KDoc in section 4.
- New files created by a split stay at or under 400 lines.
- A split does not change behavior, routes, storage keys, or public function results unless the INDEX says so.
- Do not split a large file in the same change that moves it to another package. A path change and a responsibility change are different diffs.

## 6. Where code goes

Group by what changes together, under `com.airwave.wallet`.

- A feature's pure decisions, its I/O, and its Compose screen are separate files. They may share one package.
- Do not add a `util`, `components`, or `types` package as the primary cut. A type lives with the module that owns the value.
- Key material stays in `vault`. A UI file does not hold seed bytes.
- Do not keep growing `WalletModel.kt` or `ui/ShellScreens.kt` with the next feature. Call out to the feature's type. Those two files are already the shell.
- Chrome icons that exist on web as `StrokeIcon` or `TokenTrust` live as vector XML under `android/app/src/main/res/drawable/`. See [design-principles.md](./design-principles.md) Android icons. Do not grow a screen with a hand-drawn `Canvas` glyph.

## 7. When to add an Activity

An Activity is a **short-lived intent container**: another app (or another task) launches it; it may show one or more views for that one job; when the job ends it `finish()`es and the caller is in front again. It is not an in-app page. Unlock, accounts, settings, and the three MWA approval kinds stay Compose routes or composables on the Activity that already owns that task.

Add a **new** Activity only when **all** of these hold:

1. `finish()` must restore the **caller** (browser, share sheet, another app). Keeping `MainActivity` in front would steal that back stack.
2. The job is **one incoming intent and one protocol session**, not a menu pick. Methods on that same session (authorize, sign message, sign-and-send) stay **one** Activity and switch composables.
3. Bringing the wallet shell (Home) to the foreground would be the wrong screen.

Do **not** add an Activity because a phone frame, title, or file length changed. Today: `MainActivity` is the launcher shell; `MwaActivity` is the `solana-wallet:` session host (own task, same role as FakeWallet's `MobileWalletAdapterActivity`). An INDEX may name another Activity only with the three tests above.

There are two Activities and no Fragments. A new in-app screen (Home, Activity tab, send, close account, in-app transaction approval) is a composable on `MainActivity`. Connect, sign message, and sign transaction for one dApp session are composables on `MwaActivity`. Do not add an Activity or a Fragment for those screens.

## 8. Lock screen and what comes next

`AirwaveApp` owns one `WalletModel`. Both Activities read it with `sharedWalletModel()`. Locked means `route` is `Unlock` and the signing seed is only in that model. `UnlockScreen` is one composable. Each Activity's root draws it first and does not draw its own next screen until unlock succeeds.

`unlock()` decrypts, then `applyPlain()` sets `route` to `Home`. If `pending` is already set, the next line sets `route` to `Approval`. `MainActivity` renders only `route`. Send and close-account approvals are that `Approval` route on `MainActivity`, because the caller is the wallet.

`MwaActivity` keeps the session request (`authorize`, and later sign message or sign transaction) in the session host until the user answers. After unlock, `route` is no longer `Unlock`, so the same Activity draws that still-open request. It does not navigate to Home to find the page.

The Scenario and `MwaActivity` stay up through unlock and the approval. `completeWith*` does not `finish()`. Close the Scenario when that dApp session actually ends, then `finish()` so the browser returns. Do not `close` or `finish` merely because the lock screen is showing. If the websocket drops before the user answers, fail that one request and then tear down. Process death fails the in-flight request. The result returns only to that request.

## 9. Structure pass between versions

Run this pass when opening the next named version: **after** version N's implementation review accepts and **before** version N+1's contract is reconciled.

1. Count every `.kt` file under `android/` (code lines as in section 5: blanks and comment lines excluded).
2. List files over 400 lines, named types with no declaration KDoc, and function-only files with no file-level KDoc.
3. Split a file over 400 when its responsibility comment needs a second job, or when the version that touched it did not name a reason to leave it. A single-responsibility screen that the INDEX left over 400 stays. Add the missing type KDocs.
4. Behavior stays the same. No new screen, no new store, no new route.
5. Write the file list and the line counts into version N's `how.md` as the structure track.

The pass does not mark the version `shipped`.
