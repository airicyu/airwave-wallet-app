package com.airwave.wallet.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun read(): AirwaveSettings {
        val json = prefs.getString(JSON_KEY, null)
        return if (json == null) AirwaveSettings() else AirwaveSettings.fromJson(json)
    }

    fun write(settings: AirwaveSettings) {
        prefs.edit().putString(JSON_KEY, settings.toJson().toString()).apply()
    }

    companion object {
        const val SETTINGS_KEY = "airwave.settings.v1"
        private const val PREFS_NAME = SETTINGS_KEY
        private const val JSON_KEY = "json"
    }
}
