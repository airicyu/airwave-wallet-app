# Code design

How Kotlin under `android/` is split and commented. An INDEX overrides this file when it says so. Screen layout lives in [design-principles.md](./design-principles.md). Architecture bans live in [roadmap/GUIDELINES.md](./roadmap/GUIDELINES.md). Version order lives in [roadmap/README.md](./roadmap/README.md): one named INDEX at a time.

The Chrome extension's folder tree (`background/`, `popup/`, `shared/`) does not apply here. The responsibility and size rules are the same idea as that repo's `dev-docs/code-design-principles.md`, tightened for this app: **every** Kotlin source file has a header, not only files of 50 lines or more.

## 1. One responsibility

A file does one thing a reader can name in a sentence. The neighboring step (the caller, the I/O, the next screen) is a different file.

- Do not grow a file so that one comment has to say "and". If the comment needs a second responsibility, split.
- Do not paper over a second responsibility with `related logic`, `etc.`, or `and so on`.
- Do not split one responsibility into one file per function unless that function is the whole responsibility.
- A pure data table file only holds that table.
- Tests sit next to the module they exercise, under `src/test`.

## 2. Responsibility comment

Every `.kt` file under `android/` (main and test) starts with an English block comment, before the `package` line or immediately after it as a file-level KDoc. At most **100 words**.

- Usually one or two sentences: what this file does, and the neighboring step it does not do.
- If the comment needs a second different responsibility, split the file. Do not lengthen the comment.
- If you edit a file that has no header, add the header in the same change.
- A string-catalog file says it is the catalog, not the list of keys.

## 3. Line count

Count **total lines, including blanks**. The threshold stays **400**. It is not a higher cap.

- A new `.kt` file stays at or under 400 lines.
- If a change touches a file that is already over 400, that version either splits it, or the INDEX names the file, says this version does not split it, and gives the reason.
- One screen that is still a single responsibility may stay over 400 when the INDEX says so. The next structure pass records that reason. It does not split the file only because the line count is high.
- `android/app/src/main/kotlin/com/airwave/wallet/i18n/AppStrings.kt` is the message catalog. It may exceed 400 lines. That exception is this file. It still needs the header in section 2.
- New files created by a split stay at or under 400 lines.
- A split does not change behavior, routes, storage keys, or public function results unless the INDEX says so.
- Do not split a large file in the same change that moves it to another package. A path change and a responsibility change are different diffs.

## 4. Where code goes

Group by what changes together, under `com.airwave.wallet`.

- A feature's pure decisions, its I/O, and its Compose screen are separate files. They may share one package.
- Do not add a `util`, `components`, or `types` package as the primary cut. A type lives with the module that owns the value.
- Key material stays in `vault`. A UI file does not hold seed bytes.
- Do not keep growing `WalletModel.kt` or `ui/ShellScreens.kt` with the next feature. Call out to the feature's type. Those two files are already the shell.
- Chrome icons that exist on web as `StrokeIcon` or `TokenTrust` live as vector XML under `android/app/src/main/res/drawable/`. See [design-principles.md](./design-principles.md) Android icons. Do not grow a screen with a hand-drawn `Canvas` glyph.

## 5. Structure pass between versions

Run this pass when opening the next named version: **after** version N's implementation review accepts and **before** version N+1's contract is reconciled.

1. Count every `.kt` file under `android/` (total lines, blanks included).
2. List files over 400 lines, and files with no English responsibility header.
3. Split a file over 400 when its responsibility comment needs a second job, or when the version that touched it did not name a reason to leave it. A single-responsibility screen that the INDEX left over 400 stays. Add the missing headers.
4. Behavior stays the same. No new screen, no new store, no new route.
5. Write the file list and the line counts into version N's `how.md` as the structure track.

The pass does not mark the version `shipped`.
