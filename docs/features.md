# Features

Airwave Wallet App is a non-custodial Solana wallet app (separate from the Chrome extension).

**0.6.0** (shipped): a connected dApp can ask the user to sign one message. The wallet shows the bytes on the Mobile Wallet Adapter screen and returns one signature. It does not send a transaction.

**0.5.0** (shipped): a dApp can connect through Mobile Wallet Adapter. `solana-wallet:` opens its own screen. If the vault is locked, that screen stays on unlock until the user unlocks, then Connect authorizes the active account. Sign message and sign transaction are not in this version.

**0.4.0** (shipped): everything in 0.3.0, plus a real wallet-account list (12-word seed, burner, import mnemonic or secret, watch-only). The home pill switches the active account. Combined is listed and does not create an account.

**0.3.0** (shipped): Assets shows SOL, SPL Token, and Token-2022 for the active address, with Jupiter names, icons, and a verified shield left of the token icon. Mainnet without RPC shows the Helius guide. Activity stays empty.

**0.2.0** (shipped): everything in 0.1.0, plus a hamburger menu (wallet accounts, address book, network, settings, connected sites, about, lock wallet), real Network cluster selection and Settings (language, RPC endpoints, API keys, AI settings, default CU price, wallet lock / idle auto-lock). Empty menu rows show a one-line placeholder. Mobile UI follows `dev-docs/design-demos/wallet-020.html`.

**0.1.0**: set an app password, then generate a burner wallet; wallet home with a wallet widget; Assets and Activity tabs with no content yet; MWA connect and sign.

Contracts: [0.6.0](../dev-docs/roadmap/0.6.0/INDEX.md) · [0.5.0](../dev-docs/roadmap/0.5.0/INDEX.md) · [0.4.0](../dev-docs/roadmap/0.4.0/INDEX.md) · [0.3.0](../dev-docs/roadmap/0.3.0/INDEX.md) · [0.2.0](../dev-docs/roadmap/0.2.0/INDEX.md) · [0.1.0](../dev-docs/roadmap/0.1.0/INDEX.md).
