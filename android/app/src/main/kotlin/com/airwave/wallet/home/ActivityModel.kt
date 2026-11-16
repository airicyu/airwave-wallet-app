/**
 * Holds the in-memory activity list for the home session.
 * It does not parse JSON and it does not write the rows to disk.
 */
package com.airwave.wallet.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.airwave.wallet.holdings.HoldingPhase
import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.RpcResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the Activity tab should draw besides the row list. */
enum class ActivityPhase { Idle, Loading, Empty, Error, List, Guide, Skipped }

/** Session list for the Activity tab. */
class ActivityModel(private val scope: CoroutineScope) {
    var phase by mutableStateOf(ActivityPhase.Idle)
        private set
    var rows by mutableStateOf<List<ActivityRow>>(emptyList())
        private set
    var older by mutableStateOf(Older.Off)
        private set

    private var generation = 0
    private var cursor: String? = null
    private var hasMore = false
    private var inFlight = false
    private var needLeave = false
    private var owner = ""
    private var settings = AirwaveSettings()

    /** Older-page indicator under the list. */
    enum class Older { Off, Loading, Error }

    fun clear() {
        generation += 1
        inFlight = false
        rows = emptyList()
        phase = ActivityPhase.Idle
        older = Older.Off
        hasMore = false
        cursor = null
    }

    fun enter(address: String?, settings: AirwaveSettings, holdingsPhase: HoldingPhase) {
        generation += 1
        inFlight = false
        needLeave = false
        rows = emptyList()
        older = Older.Off
        hasMore = false
        cursor = null
        if (address.isNullOrEmpty()) {
            phase = ActivityPhase.Empty
            return
        }
        if (blocked(settings, holdingsPhase)) return
        owner = address
        this.settings = settings
        start(generation, before = null, append = false)
    }

    fun loadOlder() {
        if (phase != ActivityPhase.List || !hasMore || inFlight || needLeave) return
        val before = cursor ?: return
        start(generation, before, append = true)
    }

    fun olderLeftScreen() {
        needLeave = false
    }

    private fun blocked(settings: AirwaveSettings, holdingsPhase: HoldingPhase): Boolean {
        val key = activityHeliusKey(settings.heliusApiUrl)
        val endpoint = RpcResolver.endpointForCluster(settings.cluster, settings)
        if (settings.cluster == Cluster.Mainnet && key == null && endpoint == null) {
            phase = if (holdingsPhase == HoldingPhase.Skipped) ActivityPhase.Skipped else ActivityPhase.Guide
            return true
        }
        return false
    }

    private fun start(gen: Int, before: String?, append: Boolean) {
        inFlight = true
        if (append) older = Older.Loading else phase = ActivityPhase.Loading
        scope.launch {
            val page = withContext(Dispatchers.IO) { ActivityFetch.page(owner, settings, before) }
            if (gen != generation) return@launch
            inFlight = false
            if (page.failed || page.mapped == null) {
                if (append) {
                    hasMore = true
                    needLeave = true
                    older = Older.Error
                } else {
                    rows = emptyList()
                    phase = ActivityPhase.Error
                }
                return@launch
            }
            val mapped = page.mapped
            val seen = rows.map { it.signature }.toSet()
            val extra = mapped.rows.filter { it.signature !in seen }
            if (mapped.rawCount == 20 && extra.isEmpty()) hasMore = false else hasMore = mapped.rawCount == 20
            cursor = mapped.cursor
            rows = if (append) rows + extra else extra
            older = Older.Off
            phase = if (rows.isEmpty()) ActivityPhase.Empty else ActivityPhase.List
        }
    }
}
