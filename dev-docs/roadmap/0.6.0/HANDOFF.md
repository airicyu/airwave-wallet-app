# 0.6.0 handoff

## Done this session

`shipped` (2026-11-08). `version.md` and Android `versionName` are `0.6.0`. Do not open 0.7.0 unless the user names it. Do not start `sign_and_send_transactions`. Do not parse an off-chain message header.

## Next session

None for this version.

## Paste-ready

```text
Implement Airwave Wallet App 0.6.0 from dev-docs/roadmap/0.6.0/INDEX.md.
Read order: INDEX, docs/reasoning.md, docs/how.md, 0.5.0 INDEX.
Track A: onSignMessagesRequest offers one SignMessage pending, or declines / authorization-not-valid before offer. addresses must be null, empty, or exactly the active public key.
Track B: MwaActivity draws SignMessageBody (centered title). Do not show it on MainActivity. Advertise solana:signMessages only. Do not finish() inside completeWith*. Watch shows mwa.watchAccount on MwaActivity until teardown.
Run the gradle command in the INDEX. Do not commit unless asked.
```
