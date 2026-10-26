/**
 * Starts Solana Mobile playground with Android platform-tools on PATH.
 * Does not implement a dApp; bun x fetches the CLI.
 */
import { spawnSync } from "node:child_process";
import { existsSync } from "node:fs";
import { homedir } from "node:os";
import { delimiter, join } from "node:path";

function adbName(): string {
  return process.platform === "win32" ? "adb.exe" : "adb";
}

function sdkCandidates(): string[] {
  const home = homedir();
  const fromEnv = [process.env.ANDROID_HOME, process.env.ANDROID_SDK_ROOT].filter(
    (value): value is string => Boolean(value),
  );
  const usual = [
    join(home, "Library", "Android", "sdk"),
    join(home, "Android", "Sdk"),
    join(process.env.LOCALAPPDATA ?? join(home, "AppData", "Local"), "Android", "Sdk"),
  ];
  return [...fromEnv, ...usual];
}

function findPlatformTools(): string | undefined {
  const bin = adbName();
  for (const sdk of sdkCandidates()) {
    const dir = join(sdk, "platform-tools");
    if (existsSync(join(dir, bin))) return dir;
  }
  const pathDirs = (process.env.PATH ?? "").split(delimiter);
  for (const dir of pathDirs) {
    if (dir && existsSync(join(dir, bin))) return dir;
  }
  return undefined;
}

const tools = findPlatformTools();
if (!tools) {
  console.error(
    "adb not found. Install Android platform-tools, or set ANDROID_HOME / ANDROID_SDK_ROOT.",
  );
  process.exit(1);
}

const env = {
  ...process.env,
  PATH: `${tools}${delimiter}${process.env.PATH ?? ""}`,
};

const result = spawnSync(
  process.execPath,
  ["x", "solana-mobile@latest", "playground", ...process.argv.slice(2)],
  { stdio: "inherit", env },
);
process.exit(result.status ?? 1);
