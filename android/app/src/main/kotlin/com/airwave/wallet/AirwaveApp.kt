package com.airwave.wallet
import com.airwave.wallet.session.WalletModel

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Application that owns the process-wide ViewModelStore.
 * MainActivity and MwaActivity both read the same WalletModel from this store, so unlock and the in-memory approval survive switching Activities. Unlock key material still lives only in that ViewModel. Process death clears it; this class does not write it to disk. The walletlib auth database stays on disk so a dApp auth_token remains valid across process starts.
 */
class AirwaveApp : Application(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    override fun onCreate() {
        super.onCreate()
    }
}

/**
 * Returns the WalletModel owned by AirwaveApp rather than by a single Activity.
 * Callers must not create a second in-memory vault session.
 */
fun ComponentActivity.sharedWalletModel(): WalletModel {
    val app = application as AirwaveApp
    return ViewModelProvider(
        app,
        ViewModelProvider.AndroidViewModelFactory.getInstance(app),
    )[WalletModel::class.java]
}
