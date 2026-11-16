package com.airwave.wallet.mwa.tx

import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.RpcResolver
import com.airwave.wallet.vault.BurnerKeys

/**
 * Decides whether a sign-and-send request may open, and whether a returned signature matches this account.
 * It does not sign, submit, or read the vault.
 */
object SignAndSendGate {
    /**
     * The offer outcome for one request chain.
     * [Decline] never opens the screen. [Open] may still disable Approve when the clusters differ.
     */
    sealed class Offer {
        /** Decline before offer. */
        data object Decline : Offer()

        /** Open the approval. [rpcUrl] is the submit endpoint. */
        data class Open(
            val rpcUrl: String,
            val clusterMismatch: Boolean,
            val wallet: Cluster,
            val request: Cluster,
        ) : Offer()
    }

    /** True when the payload count or bytes must be declined before offer. Version decode is separate. */
    fun declinePayload(payloads: Array<ByteArray>?): Boolean {
        if (payloads == null || payloads.size != 1 || payloads[0].isEmpty()) return true
        return false
    }

    /** Maps the request chain to decline or open. A testnet or unknown chain declines. */
    fun offer(chain: String?, settings: AirwaveSettings): Offer {
        val mapped = RpcResolver.mapRequestChain(chain)
        if (mapped == RpcResolver.SubmitCluster.Unknown || mapped == RpcResolver.SubmitCluster.Testnet) {
            return Offer.Decline
        }
        val url = RpcResolver.rpcUrlForSubmit(chain, settings) ?: return Offer.Decline
        val request = if (mapped == RpcResolver.SubmitCluster.Devnet) Cluster.Devnet else Cluster.Mainnet
        return Offer.Open(url, request != settings.cluster, settings.cluster, request)
    }

    /** True when the RPC signature decodes to this account's 64-byte slot. */
    fun signatureMatches(rpcSignature: String, slot: ByteArray): Boolean {
        if (slot.size != 64 || slot.all { it == 0.toByte() }) return false
        return try {
            BurnerKeys.base58Decode(rpcSignature).contentEquals(slot)
        } catch (_: Exception) {
            false
        }
    }
}
