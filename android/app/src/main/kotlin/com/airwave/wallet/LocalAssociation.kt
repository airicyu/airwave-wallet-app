package com.airwave.wallet

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.airwave.wallet.vault.BurnerKeys
import com.solana.mobilewalletadapter.walletlib.association.LocalAssociationUri
import com.solana.mobilewalletadapter.walletlib.authorization.AuthIssuerConfig
import com.solana.mobilewalletadapter.walletlib.protocol.MobileWalletAdapterConfig
import com.solana.mobilewalletadapter.walletlib.scenario.AuthorizeRequest
import com.solana.mobilewalletadapter.walletlib.scenario.AuthorizedAccount
import com.solana.mobilewalletadapter.walletlib.scenario.DeauthorizedEvent
import com.solana.mobilewalletadapter.walletlib.scenario.LocalWebSocketServerScenario
import com.solana.mobilewalletadapter.walletlib.scenario.ReauthorizeRequest
import com.solana.mobilewalletadapter.walletlib.scenario.Scenario
import com.solana.mobilewalletadapter.walletlib.scenario.SignAndSendTransactionsRequest
import com.solana.mobilewalletadapter.walletlib.scenario.SignMessagesRequest
import com.solana.mobilewalletadapter.walletlib.scenario.SignTransactionsRequest
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
class LocalAssociation(
    private val context: Context,
    private val model: WalletModel,
) {
    private val main = Handler(Looper.getMainLooper())
    private var scenario: LocalWebSocketServerScenario? = null

    fun start(uri: Uri) {
        val association = LocalAssociationUri(uri)
        val config = MobileWalletAdapterConfig(
            true,
            1,
            1,
            arrayOf(MobileWalletAdapterConfig.LEGACY_TRANSACTION_VERSION),
            0L,
        )
        val callbacks = object : Scenario.Callbacks {
            override fun onScenarioReady() = Unit
            override fun onScenarioServingClients() = Unit
            override fun onScenarioServingComplete() = Unit
            override fun onScenarioComplete() { scenario?.close() }
            override fun onScenarioError() { scenario?.close() }
            override fun onScenarioTeardownComplete() = Unit

            override fun onAuthorizeRequest(request: AuthorizeRequest) {
                main.post {
                    if (model.address == null && model.route != Route.Unlock) {
                        request.completeWithDecline()
                        return@post
                    }
                    val chain = request.chain ?: request.cluster ?: ""
                    model.offer(
                        Pending(
                            title = "Connect",
                            lines = listOf(
                                "App" to (request.identityName ?: ""),
                                "Site" to (request.identityUri?.toString() ?: ""),
                                "Chain" to chain,
                                "Account" to BurnerKeys.short(model.address ?: ""),
                            ),
                            sign = { seed -> publicKey(seed) },
                            onResult = { ok, bytes ->
                                if (!ok || bytes == null) request.completeWithDecline()
                                else {
                                    val account = AuthorizedAccount(
                                        bytes,
                                        BurnerKeys.base58(bytes),
                                        null,
                                        arrayOf("solana:mainnet", "solana:devnet", "solana:testnet"),
                                        arrayOf("solana:signMessages", "solana:signAndSendTransaction"),
                                    )
                                    request.completeWithAuthorize(account, null, null, null)
                                }
                            },
                        ),
                    )
                }
            }

            override fun onReauthorizeRequest(request: ReauthorizeRequest) {
                request.completeWithDecline()
            }

            override fun onSignTransactionsRequest(request: SignTransactionsRequest) {
                request.completeWithDecline()
            }

            override fun onSignMessagesRequest(request: SignMessagesRequest) {
                main.post {
                    val message = request.payloads.firstOrNull()?.decodeToString() ?: ""
                    model.offer(
                        Pending(
                            title = "Sign message",
                            lines = listOf(
                                "App" to (request.identityName ?: ""),
                                "Account" to BurnerKeys.short(model.address ?: ""),
                                "Message" to message,
                            ),
                            sign = { seed ->
                                val payload = request.payloads.first()
                                BurnerKeys.sign(seed, payload)
                            },
                            onResult = { ok, bytes ->
                                if (!ok || bytes == null) request.completeWithDecline()
                                else request.completeWithSignedPayloads(arrayOf(bytes))
                            },
                        ),
                    )
                }
            }

            override fun onSignAndSendTransactionsRequest(request: SignAndSendTransactionsRequest) {
                val chain = request.chain ?: request.cluster
                if (rpcFor(chain) == null) {
                    request.completeWithDecline()
                    return
                }
                request.completeWithDecline()
            }

            override fun onDeauthorizedEvent(event: DeauthorizedEvent) {
                event.complete()
            }
        }
        scenario = association.createScenario(context, config, AuthIssuerConfig("Airwave"), callbacks)
        scenario?.start()
    }

    private fun publicKey(seed: ByteArray): ByteArray =
        Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded

    private fun rpcFor(chain: String?): String? = when (chain) {
        "solana:mainnet", "mainnet-beta" -> "https://api.mainnet-beta.solana.com"
        "solana:devnet", "devnet" -> "https://api.devnet.solana.com"
        "solana:testnet", "testnet" -> "https://api.testnet.solana.com"
        else -> null
    }
}
