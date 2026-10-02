package com.lanchat.app.network.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles ECDH key exchange, AES-256-GCM symmetric encryption,
 * and ECDSA signature-based identity authentication using Android Keystore.
 * Requires API 31+ (ECDH / PURPOSE_AGREE_KEY in Keystore).
 */
class EncryptionManager(private val context: Context) {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    init {
        ensureIdentityKeyPair()
    }

    private fun ensureIdentityKeyPair() {
        if (!keyStore.containsAlias(KEY_ALIAS_IDENTITY)) {
            val kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )
            kpg.initialize(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS_IDENTITY,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_AGREE_KEY
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build()
            )
            kpg.generateKeyPair()
        }
    }

    fun getPublicKeyBase64(): String {
        val entry = keyStore.getEntry(KEY_ALIAS_IDENTITY, null) as KeyStore.PrivateKeyEntry
        return Base64.encodeToString(entry.certificate.publicKey.encoded, Base64.NO_WRAP)
    }

    fun signData(data: ByteArray): String {
        val entry = keyStore.getEntry(KEY_ALIAS_IDENTITY, null) as KeyStore.PrivateKeyEntry
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(entry.privateKey)
        signer.update(data)
        return Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
    }

    fun verifySignature(data: ByteArray, signatureBase64: String, publicKeyBase64: String): Boolean {
        return try {
            val pubKeyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(pubKeyBytes))

            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(data)
            verifier.verify(Base64.decode(signatureBase64, Base64.NO_WRAP))
        } catch (e: Exception) {
            false
        }
    }

    fun deriveSharedSecret(peerPublicKeyBase64: String): SecretKey {
        val pubKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val keyFactory = KeyFactory.getInstance("EC")
        val peerPublicKey: PublicKey = keyFactory.generatePublic(X509EncodedKeySpec(pubKeyBytes))

        val entry = keyStore.getEntry(KEY_ALIAS_IDENTITY, null) as KeyStore.PrivateKeyEntry
        val myPrivateKey: PrivateKey = entry.privateKey

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(peerPublicKey, true)

        val secretBytes = keyAgreement.generateSecret()
        // SHA-256 of the raw ECDH secret -> 32 byte AES-256 key
        val derivedKey = MessageDigest.getInstance("SHA-256").digest(secretBytes)
        return SecretKeySpec(derivedKey, "AES")
    }

    fun encryptPayload(plainText: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptPayload(cipherTextBase64: String, secretKey: SecretKey): String {
        val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
        require(combined.size > GCM_IV_LENGTH) { "Invalid cipher length" }

        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    companion object {
        private const val KEY_ALIAS_IDENTITY = "lan_chat_ec_identity"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
