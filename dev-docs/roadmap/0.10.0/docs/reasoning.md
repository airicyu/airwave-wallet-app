# 0.10.0 — reasoning

## What ships

Two files that had grown past one responsibility are split, and the root package and the `ui` grab bag are grouped by what changes together. Behavior stays the 0.9.0 behavior.

## Why

`SignTxWire` both parsed every message version and owned the byte codec. `CombinedScreens` both created a combined account and managed an existing one. The root package mixed account edits, settings writes, and the two Activities. `code-design.md` already names the packages. This version applies that grouping without a second behavior change in the same diff as the earlier file split.

## What was dropped

A new package named `util`, `components`, or `types`. Moving `mwa` screens into the protocol package. Splitting `AppStrings.kt`.
