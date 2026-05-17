package com.fluxzen.babybeam

import android.content.Context
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object SecurityUtil {
    private const val PREFS_NAME = "BabyBeamSecurityPrefs"
    private const val PREF_PRIVATE_KEY = "MyPrivateKey"
    private const val PREF_PUBLIC_KEY = "MyPublicKey"
    private const val PREF_TRUSTED_PEER_KEY = "TrustedPeerPublicKey"
    private const val ALGORITHM = "SHA256withRSA"
    private const val KEY_ALGORITHM = "RSA"
    private const val MAX_TIME_DIFF_MS = 300000L // 5 minutes

    private var myKeyPair: KeyPair? = null

    // For testing
    var fallbackKeyPairForTesting: KeyPair? = null

    // Gets or generates the device's own RSA key pair
    fun getOrCreateKeyPair(context: Context?): KeyPair {
        if (context == null) {
            if (fallbackKeyPairForTesting == null) {
                val kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM)
                kpg.initialize(2048)
                fallbackKeyPairForTesting = kpg.generateKeyPair()
            }
            return fallbackKeyPairForTesting!!
        }

        if (myKeyPair != null) return myKeyPair!!
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val privStr = prefs.getString(PREF_PRIVATE_KEY, null)
        val pubStr = prefs.getString(PREF_PUBLIC_KEY, null)

        if (privStr != null && pubStr != null) {
            try {
                val privKeyBytes = Base64.getDecoder().decode(privStr)
                val pubKeyBytes = Base64.getDecoder().decode(pubStr)
                val kf = KeyFactory.getInstance(KEY_ALGORITHM)
                val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(privKeyBytes))
                val publicKey = kf.generatePublic(X509EncodedKeySpec(pubKeyBytes))
                myKeyPair = KeyPair(publicKey, privateKey)
                return myKeyPair!!
            } catch (e: Exception) {
                // Fallback to generation if parsing fails
            }
        }

        val kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()
        prefs.edit()
            .putString(PREF_PRIVATE_KEY, Base64.getEncoder().encodeToString(kp.private.encoded))
            .putString(PREF_PUBLIC_KEY, Base64.getEncoder().encodeToString(kp.public.encoded))
            .apply()
        myKeyPair = kp
        return kp
    }

    // Creates a signed message containing the sender's public key
    fun generateSignedMessage(context: Context?, payload: String): String {
        val keyPair = getOrCreateKeyPair(context)
        val timestamp = System.currentTimeMillis()
        val pubKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        val messageToSign = "$pubKeyBase64:$payload:$timestamp"

        val signature = Signature.getInstance(ALGORITHM)
        signature.initSign(keyPair.private)
        signature.update(messageToSign.toByteArray(Charsets.UTF_8))
        val sigBytes = signature.sign()
        val sigBase64 = Base64.getEncoder().encodeToString(sigBytes)

        return "$messageToSign:$sigBase64"
    }

    // Verifies the message. Uses TOFU (Trust On First Use) to save the trusted peer's public key.
    fun verifySignedMessage(context: Context?, signedMessage: String): String? {
        val parts = signedMessage.split(":")
        if (parts.size != 4) return null // Format: pubKey : payload : timestamp : signature

        val pubKeyBase64 = parts[0]
        val payload = parts[1]
        val timestampStr = parts[2]
        val sigBase64 = parts[3]

        val timestamp = timestampStr.toLongOrNull() ?: return null
        val currentTime = System.currentTimeMillis()
        if (Math.abs(currentTime - timestamp) > MAX_TIME_DIFF_MS) {
            return null // Reject messages that are too old (replay attack mitigation)
        }

        if (context != null) {
            // Check if we have a trusted peer key
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val trustedPubKeyStr = prefs.getString(PREF_TRUSTED_PEER_KEY, null)

            if (trustedPubKeyStr == null) {
                // TOFU: Trust On First Use - Save this public key as the trusted one for future messages
                prefs.edit().putString(PREF_TRUSTED_PEER_KEY, pubKeyBase64).apply()
            } else if (trustedPubKeyStr != pubKeyBase64) {
                // Message is from an unknown/untrusted device
                return null
            }
        }

        try {
            val pubKeyBytes = Base64.getDecoder().decode(pubKeyBase64)
            val kf = KeyFactory.getInstance(KEY_ALGORITHM)
            val publicKey = kf.generatePublic(X509EncodedKeySpec(pubKeyBytes))

            val messageToVerify = "$pubKeyBase64:$payload:$timestampStr"

            val signature = Signature.getInstance(ALGORITHM)
            signature.initVerify(publicKey)
            signature.update(messageToVerify.toByteArray(Charsets.UTF_8))

            val sigBytes = Base64.getDecoder().decode(sigBase64)
            if (signature.verify(sigBytes)) {
                return payload
            }
        } catch (e: Exception) {
            // Verification failed
        }

        return null
    }
}
