package com.airwave.wallet

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.airwave.wallet.solana.LegacyTransaction
import com.airwave.wallet.solana.SolanaJsonRpc
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
import java.util.concurrent.Executors

class LocalAssociation(
    private val context: Context,
    private val model: WalletModel,
) {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
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
                            approve = { seed -> ApprovalResult.Bytes(publicKey(seed)) },
                            finish = { result ->
                                when (result) {
                                    is ApprovalResult.Bytes -> {
                                        val bytes = result.value
                                        val account = AuthorizedAccount(
                                            bytes,
                                            BurnerKeys.base58(bytes),
                                            null,
                                            arrayOf("solana:mainnet", "solana:devnet", "solana:testnet"),
                                            arrayOf("solana:signMessages", "solana:signAndSendTransaction"),
                                        )
                                        request.completeWithAuthorize(account, null, null, null)
                                    }
                                    else -> request.completeWithDecline()
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
                            approve = { seed ->
                                val payload = request.payloads.first()
                                val sig = BurnerKeys.sign(seed, payload)
                                ApprovalResult.Bytes(sig)
                            },
                            finish = { result ->
                                when (result) {
                                    is ApprovalResult.Bytes -> request.completeWithSignedPayloads(arrayOf(result.value))
                                    else -> request.completeWithDecline()
                                }
                            },
                        ),
                    )
                }
            }

            override fun onSignAndSendTransactionsRequest(request: SignAndSendTransactionsRequest) {
                val chain = request.chain ?: request.cluster
                val rpcUrl = model.rpcForSubmit(chain)
                if (rpcUrl == null) {
                    request.completeWithDecline()
                    return
                }
                val payload = request.payloads.firstOrNull() ?: run {
                    request.completeWithDecline()
                    return
                }
                worker.execute {
                    val summary = LegacyTransaction.summarize(payload)
                    val txB64 = LegacyTransaction.base64Wire(payload)
                    val fee = SolanaJsonRpc.estimateFeeLamports(rpcUrl, txB64)
                    val lines = buildList {
                        add("App" to (request.identityName ?: ""))
                        add("Chain" to (chain ?: ""))
                        summary?.instructionNames?.forEach { name ->
                            add("Instruction" to name)
                        }
                        summary?.destination?.let { add("Destination" to it) }
                        summary?.amountLamports?.let { add("Amount" to "$it lamports") }
                        fee?.let { add("Fee" to "$it lamports") }
                    }
                    val hasTransfer = summary?.instructionNames?.contains("Transfer") == true
                    if (hasTransfer && (summary?.destination == null || summary.amountLamports == null || fee == null)) {
                        main.post { request.completeWithDecline() }
                        return@execute
                    }
                    main.post {
                        model.offer(
                            Pending(
                                title = "Sign and send",
                                lines = lines,
                                approve = { secret -> model.signAndSend(payload, rpcUrl, secret) },
                                finish = { result ->
                                    when (result) {
                                        is ApprovalResult.Signatures -> request.completeWithSignatures(result.value)
                                        else -> request.completeWithDecline()
                                    }
                                },
                            ),
                        )
                    }
                }
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

}
