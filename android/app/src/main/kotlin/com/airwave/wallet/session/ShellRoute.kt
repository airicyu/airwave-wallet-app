package com.airwave.wallet.session

/** Names the shell screen WalletApp should show. */
enum class Route { Setup, Unlock, Lead, Generate, Home, Approval }

/** Names the selected Home bottom tab. */
enum class Tab { Assets, Activity }

/** Names a screen pushed on the home stack from the hamburger menu. */
enum class HomeDestination {
    Accounts,
    AddAccount,
    ImportChooser,
    AddBurner,
    GenerateSeed,
    ImportMnemonic,
    ImportSecret,
    WatchAddress,
    CombinedLater,
    EmptyAddressBook,
    EmptyConnected,
    EmptyAbout,
    Network,
    SettingsHub,
    Language,
    Rpc,
    ApiKeys,
    AiSettings,
    CuPrice,
    WalletLockSettings,
}
