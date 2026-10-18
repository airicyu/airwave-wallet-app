package com.airwave.wallet.vault

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class VaultFile(
    val kdf: String,
    val salt: String,
    val nonce: String,
    val ciphertext: String,
)

class VaultCipher(
    private val random: SecureRandom = SecureRandom(),
) {
    fun seal(password: CharArray, plaintext: ByteArray): VaultFile {
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        val out = cipher.doFinal(plaintext)
        key.fill(0)
        return VaultFile(
            kdf = "argon2id",
            salt = b64(salt),
            nonce = b64(nonce),
            ciphertext = b64(out),
        )
    }

    fun open(password: CharArray, file: VaultFile): ByteArray {
        require(file.kdf == "argon2id")
        val salt = b64d(file.salt)
        val nonce = b64d(file.nonce)
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return try {
            cipher.doFinal(b64d(file.ciphertext))
        } finally {
            key.fill(0)
        }
    }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withIterations(2)
            .withMemoryAsKB(65536)
            .withParallelism(1)
            .build()
        val generator = Argon2BytesGenerator()
        generator.init(params)
        val key = ByteArray(32)
        generator.generateBytes(password.concatToString().encodeToByteArray(), key)
        return key
    }

    private fun b64(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
    private fun b64d(text: String) = Base64.getDecoder().decode(text)
}
