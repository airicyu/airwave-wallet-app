package com.airwave.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Launcher Activity for the wallet shell: setup, unlock, and Home.
 * Incoming `solana-wallet:` intents go to MwaActivity so that finish() restores the dApp instead of this task.
 */
class MainActivity : ComponentActivity() {
    private val model: WalletModel by lazy { sharedWalletModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WalletApp(model) }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        model.bumpUserActivity()
    }
}
