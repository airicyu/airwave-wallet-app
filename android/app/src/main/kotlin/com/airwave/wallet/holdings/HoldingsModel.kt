package com.airwave.wallet.holdings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.RpcResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the in-memory holdings snapshot, the current round id, and the Mainnet guide flag for the assets tab.
 * WalletModel starts a fetch round; JSON parsing lives in HoldingsFetch and its helpers. This class does not open the vault.
 */
class HoldingsModel(private val scope: CoroutineScope) {
    var snapshot by mutableStateOf(HoldingsSnapshot())
        private set

    private var round = 0L
    private var skip = false
    private var inFlight = false
    private var cooldownUntil = 0L
    private var loadedKey: String? = null

    fun clear() {
        round += 1
        skip = false
        inFlight = false
        loadedKey = null
        snapshot = HoldingsSnapshot()
    }

    fun skipGuide() {
        skip = true
        round += 1
        inFlight = false
        snapshot = HoldingsSnapshot(phase = HoldingPhase.Skipped)
    }

    fun sync(address: String?, settings: AirwaveSettings, refresh: Boolean) {
        if (address.isNullOrEmpty()) {
            absentEndpoint()
            return
        }
        val endpoint = RpcResolver.endpointForCluster(settings.cluster, settings)
        if (endpoint == null) {
            absentEndpoint()
            return
        }
        val key = address + "|" + settings.cluster.name + "|" + endpoint
        if (!refresh && key == loadedKey && (inFlight || snapshot.phase == HoldingPhase.Loading || snapshot.phase == HoldingPhase.Ready)) {
            return
        }
        if (refresh) {
            if (inFlight) return
            if (snapshot.phase == HoldingPhase.Ready && System.currentTimeMillis() < cooldownUntil) return
        }
        loadedKey = key
        start(address, settings, endpoint)
    }

    private fun absentEndpoint() {
        round += 1
        inFlight = false
        loadedKey = null
        snapshot = HoldingsSnapshot(phase = if (skip) HoldingPhase.Skipped else HoldingPhase.Guide)
    }

    private fun start(address: String, settings: AirwaveSettings, endpoint: String) {
        round += 1
        val id = round
        inFlight = true
        snapshot = HoldingsSnapshot(phase = HoldingPhase.Loading)
        scope.launch {
            val next = withContext(Dispatchers.IO) { HoldingsFetch.round(address, settings, endpoint) }
            if (id != round) return@launch
            inFlight = false
            snapshot = next
            if (next.phase == HoldingPhase.Ready) cooldownUntil = System.currentTimeMillis() + 3_000
            else loadedKey = null
        }
    }
}
