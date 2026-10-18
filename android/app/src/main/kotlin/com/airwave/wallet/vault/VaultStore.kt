package com.airwave.wallet.vault

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import java.io.File

class VaultStore(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "airwave.vault.v1")
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    fun exists(): Boolean = file.exists()

    fun write(vault: VaultFile) {
        if (file.exists()) file.delete()
        encrypted().openFileOutput().use { it.write(json(vault).encodeToByteArray()) }
    }

    fun read(): VaultFile? {
        if (!file.exists()) return null
        val text = encrypted().openFileInput().use { it.readBytes().decodeToString() }
        val json = JSONObject(text)
        return VaultFile(
            kdf = json.getString("kdf"),
            salt = json.getString("salt"),
            nonce = json.getString("nonce"),
            ciphertext = json.getString("ciphertext"),
        )
    }

    private fun encrypted() = EncryptedFile.Builder(
        appContext,
        file,
        masterKey,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
    ).build()

    private fun json(vault: VaultFile) = JSONObject()
        .put("kdf", vault.kdf)
        .put("salt", vault.salt)
        .put("nonce", vault.nonce)
        .put("ciphertext", vault.ciphertext)
        .toString()
}
