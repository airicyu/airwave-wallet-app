package com.airwave.wallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val model: WalletModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent { WalletApp(model) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        model.bumpUserActivity()
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme != "solana-wallet") return
        val path = data.path ?: data.encodedPath ?: return
        if (!path.contains("associate/local")) return
        LocalAssociation(this, model).start(data)
    }
}
