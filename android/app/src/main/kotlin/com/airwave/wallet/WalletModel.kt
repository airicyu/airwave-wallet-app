package com.airwave.wallet

import android.app.Application
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.airwave.wallet.vault.BurnerKey
import com.airwave.wallet.vault.BurnerKeys
import com.airwave.wallet.vault.VaultCipher
import com.airwave.wallet.vault.VaultStore
import org.json.JSONObject
import javax.crypto.AEADBadTagException

class WalletModel(app: Application) : AndroidViewModel(app) {
    private val store = VaultStore(app)
    private val cipher = VaultCipher()
    private var passwordChars: CharArray? = null
    private var seed: ByteArray? = null

    var route by mutableStateOf(if (store.exists()) Route.Unlock else Route.Setup)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var address by mutableStateOf<String?>(null)
        private set
    var preview by mutableStateOf<BurnerKey?>(null)
        private set
    var tab by mutableStateOf(Tab.Assets)
    var pending by mutableStateOf<Pending?>(null)
        private set

    fun setup(password: String, again: String) {
        error = null
        when {
            password.length < 8 -> error = "Password is too short."
            password != again -> error = "Passwords do not match."
            else -> {
                remember(password)
                store.write(cipher.seal(passwordChars!!, "{}".encodeToByteArray()))
                route = Route.Lead
            }
        }
    }

    fun unlock(password: String) {
        error = null
        val file = store.read() ?: return
        val chars = password.toCharArray()
        val plain = try {
            cipher.open(chars, file)
        } catch (_: AEADBadTagException) {
            chars.fill(0.toChar())
            error = "Wrong password."
            return
        }
        remember(chars)
        applyPlain(plain)
        route = if (pending != null && address != null) Route.Approval else route
    }

    fun startGenerate() {
        preview?.zero()
        preview = BurnerKeys.generate()
        route = Route.Generate
    }

    fun roll() {
        preview?.zero()
        preview = BurnerKeys.generate()
    }

    fun commitGenerate() {
        val key = preview ?: return
        val body = JSONObject()
            .put("seed", Base64.encodeToString(key.seed, Base64.NO_WRAP))
            .toString()
        val chars = passwordChars ?: return
        store.write(cipher.seal(chars, body.encodeToByteArray()))
        seed?.fill(0)
        seed = key.seed.copyOf()
        address = key.address
        key.zero()
        preview = null
        route = Route.Home
    }

    fun lock() {
        failPending()
        seed?.fill(0)
        seed = null
        address = null
        passwordChars?.fill(0.toChar())
        passwordChars = null
        preview?.zero()
        preview = null
        error = null
        route = if (store.exists()) Route.Unlock else Route.Setup
    }

    fun offer(request: Pending) {
        if (pending != null) {
            request.onResult(false, null)
            return
        }
        if (!store.exists() || (address == null && route != Route.Unlock)) {
            request.onResult(false, null)
            return
        }
        pending = request
        route = if (address == null) Route.Unlock else Route.Approval
    }

    fun approve() {
        val request = pending ?: return
        val secret = seed
        val signed = if (secret == null) null else request.sign(secret)
        pending = null
        route = Route.Home
        request.onResult(signed != null, signed)
    }

    fun reject() {
        val request = pending
        pending = null
        request?.onResult?.invoke(false, null)
        if (address != null) route = Route.Home
    }

    private fun failPending() {
        val request = pending
        pending = null
        request?.onResult?.invoke(false, null)
    }

    private fun remember(password: String) {
        passwordChars?.fill(0.toChar())
        passwordChars = password.toCharArray()
    }

    private fun remember(chars: CharArray) {
        passwordChars?.fill(0.toChar())
        passwordChars = chars
    }

    private fun applyPlain(plain: ByteArray) {
        val json = JSONObject(plain.decodeToString())
        if (json.has("seed")) {
            seed?.fill(0)
            seed = Base64.decode(json.getString("seed"), Base64.NO_WRAP)
            address = BurnerKeys.addressOf(seed!!)
            route = Route.Home
        } else {
            seed?.fill(0)
            seed = null
            address = null
            route = Route.Lead
        }
    }
}

enum class Route { Setup, Unlock, Lead, Generate, Home, Approval }
enum class Tab { Assets, Activity }

data class Pending(
    val title: String,
    val lines: List<Pair<String, String>>,
    val sign: (ByteArray) -> ByteArray?,
    val onResult: (Boolean, ByteArray?) -> Unit,
)
