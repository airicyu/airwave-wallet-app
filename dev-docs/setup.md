# Developer setup

**0.1.0:** **Native Android** (Kotlin + Compose). Roadmap status is **`draft`** — **`android/` is not in the repo yet** until implementation starts per [`roadmap/0.1.0/INDEX.md`](./roadmap/0.1.0/INDEX.md).

## Prerequisites

- **Android Studio** (current stable; includes SDK Manager)
- **JDK 17** (Studio embedded JDK is fine)
- Physical **Android device** or emulator for hand-tests; MWA connect tests need a real device with a wallet **client** app when you reach that track

## Open the project

After `android/` exists:

1. **File → Open** → select the repo’s **`android/`** directory (Gradle root), not the Flutter-style parent-only layout.
2. Let Gradle sync finish.
3. Run **:app** on a device or emulator (Run ▶).

You can also build from the terminal without opening Studio if the Android SDK is on your `PATH`:

```bash
cd android
./gradlew :app:installDebug
```

Studio is still recommended for layout preview, debugger, and manifest/intent work (e.g. MWA).

## Checks (once the module exists)

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Contract: [`roadmap/0.1.0/INDEX.md`](./roadmap/0.1.0/INDEX.md) and [`roadmap/0.1.0/docs/how.md`](./roadmap/0.1.0/docs/how.md). Agent guide: [AGENTS.md](../AGENTS.md).

User-facing docs: [docs/user-guide.md](../docs/user-guide.md).

## MWA hand-test (emulator)

Use the official Solana Mobile playground, not a page in this repo. Docs: https://docs.solanamobile.com/cli/playground

With the emulator running and Airwave installed, from the repo root:

```bash
bun run playground
```

That script finds Android `platform-tools` (`ANDROID_HOME` / `ANDROID_SDK_ROOT`, then the usual SDK folders on macOS, Linux, and Windows) so `adb` is on `PATH`, then runs `bun x solana-mobile@latest playground`. Needs [Bun](https://bun.sh).

The CLI runs `adb reverse` and opens **Android Chrome** at `http://localhost:4747`. Do not use `http://10.0.2.2:…` (not a secure context). Host Windows Chrome cannot complete local MWA association.

Unlock a signing Airwave account first. Playground Connect / Sign message / Sign and send exercise this app. Sign In With Solana is out of scope until an INDEX schedules it.
