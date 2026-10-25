/**
 * Vault plaintext JSON for the accounts array and legacy seed migration.
 * Does not open the password box or touch settings.
 */
package com.airwave.wallet.vault

import java.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class VaultPlaintext(
    val accounts: List<AccountRecord>,
    val migrated: Boolean,
)

object VaultAccounts {
    fun parse(bytes: ByteArray): VaultPlaintext? {
        val json = try {
            JSONObject(bytes.decodeToString())
        } catch (_: Exception) {
            return null
        }
        if (json.has("accounts")) {
            val arr = json.optJSONArray("accounts") ?: return null
            val list = mutableListOf<AccountRecord>()
            for (i in 0 until arr.length()) {
                parseRow(arr.getJSONObject(i))?.let { list += it }
            }
            if (list.isEmpty()) return null
            return VaultPlaintext(list, migrated = false)
        }
        if (json.has("seed")) {
            val seed = try {
                Base64.getDecoder().decode(json.getString("seed"))
            } catch (_: Exception) {
                return null
            }
            if (seed.size != 32) return null
            val id = UUID.randomUUID().toString()
            val address = BurnerKeys.addressOf(seed)
            val row = AccountRecord(
                id = id,
                kind = AccountKind.Burner,
                label = "Account 1",
                address = address,
                seedBase64 = Base64.getEncoder().encodeToString(seed),
            )
            return VaultPlaintext(listOf(row), migrated = true)
        }
        if (json.length() == 0) {
            return VaultPlaintext(emptyList(), migrated = false)
        }
        return null
    }

    fun toJsonBytes(accounts: List<AccountRecord>): ByteArray {
        val arr = JSONArray()
        accounts.forEach { arr.put(toJson(it)) }
        return JSONObject().put("accounts", arr).toString().encodeToByteArray()
    }

    fun signingSeed(record: AccountRecord, resources: android.content.res.Resources): ByteArray? {
        return when (record.kind) {
            AccountKind.Watch -> null
            AccountKind.Burner, AccountKind.Secret -> {
                val b64 = record.seedBase64 ?: return null
                Base64.getDecoder().decode(b64).takeIf { it.size == 32 }?.copyOf()
            }
            AccountKind.Seed -> {
                val phrase = record.mnemonic ?: return null
                Bip39.deriveSolanaSeed(resources, phrase, record.pathKind, record.pathIndex)
            }
        }
    }

    fun hasAddress(accounts: List<AccountRecord>, address: String): Boolean =
        accounts.any { it.address == address }

    private fun parseRow(obj: JSONObject): AccountRecord? {
        val kind = AccountKind.fromWire(obj.optString("kind")) ?: return null
        val id = obj.optString("id").ifBlank { UUID.randomUUID().toString() }
        val label = obj.optString("label", "Account")
        val address = obj.optString("address")
        if (address.isBlank()) return null
        return when (kind) {
            AccountKind.Watch -> AccountRecord(id, kind, label, address)
            AccountKind.Burner, AccountKind.Secret -> {
                val seed = obj.optString("seedBase64", obj.optString("seed"))
                if (seed.isBlank()) return null
                AccountRecord(id, kind, label, address, seedBase64 = seed)
            }
            AccountKind.Seed -> {
                val mnemonic = obj.optString("mnemonic")
                if (mnemonic.isBlank()) return null
                AccountRecord(
                    id = id,
                    kind = kind,
                    label = label,
                    address = address,
                    mnemonic = mnemonic,
                    pathKind = ImportPathKind.fromWire(obj.optString("pathKind")),
                    pathIndex = obj.optInt("pathIndex", 0),
                )
            }
        }
    }

    private fun toJson(record: AccountRecord): JSONObject {
        val obj = JSONObject()
            .put("id", record.id)
            .put("kind", record.kind.wire)
            .put("label", record.label)
            .put("address", record.address)
        when (record.kind) {
            AccountKind.Watch -> Unit
            AccountKind.Burner, AccountKind.Secret -> obj.put("seedBase64", record.seedBase64)
            AccountKind.Seed -> {
                obj.put("mnemonic", record.mnemonic)
                obj.put("pathKind", record.pathKind.wire)
                obj.put("pathIndex", record.pathIndex)
            }
        }
        return obj
    }
}
