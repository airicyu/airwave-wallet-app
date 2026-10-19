# Implementation review — 0.2.0

Reviewed: INDEX, reasoning, how, `android/` implementation, `wallet-020.html` alignment (2026-10-20).

## Summary

0.2.0 delivers the extension-aligned hamburger IA on native Android with mobile chrome (top sheet menu, bottom tabs, unlock/setup gradient shell), durable `airwave.settings.v1`, Network vs RPC split, Settings hub and subpages, idle auto-lock, and MWA sign-and-send RPC resolution by request chain. Unit tests and `compileDebugKotlin` / prior agent `assembleDebug` + `lintDebug` were green on Windows + Android Studio JBR.

## Findings

### High

（無）

### Medium

| ID | Topic | Status |
|----|--------|--------|
| M1 | INDEX hand-tests A–E not recorded in this file | **Waived at ship** — user requested ship after on-device iteration (UI alignment, compile fixes). Re-run INDEX hand-test locally after install. |

### Low

| ID | Topic | Status |
|----|--------|--------|
| L1 | `implementation-review.md` was missing until ship | Closed — this file |

## Verdict

**Ship** — no open HIGH; M1 waived by product owner at ship.
