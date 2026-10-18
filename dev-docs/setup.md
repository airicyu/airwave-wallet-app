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
