package com.fluxzen.babybeam

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class SecurityUtilTest {
    @Test
    fun `test valid message verification`() {
        val payload = "cry_detected"
        // passing null context so the test uses a fallback dummy key instead of trying to hit SharedPreferences
        val signedMessage = SecurityUtil.generateSignedMessage(null, payload)

        assertEquals(payload, SecurityUtil.verifySignedMessage(null, signedMessage))
    }

    @Test
    fun `test invalid message format`() {
        assertNull(SecurityUtil.verifySignedMessage(null, "invalid_format"))
        assertNull(SecurityUtil.verifySignedMessage(null, "part1:part2:part3"))
    }

    @Test
    fun `test tampered payload`() {
        val signedMessage = SecurityUtil.generateSignedMessage(null, "cry_detected")
        val tamperedMessage = signedMessage.replace("cry_detected", "safe")

        assertNull(SecurityUtil.verifySignedMessage(null, tamperedMessage))
    }

    @Test
    fun `test expired timestamp`() {
        // simulate an old message
        val oldTimestamp = System.currentTimeMillis() - 300001 // 5 minutes + 1 ms ago

        val keyPair = SecurityUtil.getOrCreateKeyPair(null)
        val pubKeyBase64 = java.util.Base64.getEncoder().encodeToString(keyPair.public.encoded)

        val payload = "cry_detected"
        val messageToSign = "$pubKeyBase64:$payload:$oldTimestamp"

        val signature = java.security.Signature.getInstance("SHA256withRSA")
        signature.initSign(keyPair.private)
        signature.update(messageToSign.toByteArray(Charsets.UTF_8))
        val sigBytes = signature.sign()
        val sigBase64 = java.util.Base64.getEncoder().encodeToString(sigBytes)

        val oldSignedMessage = "$messageToSign:$sigBase64"

        assertNull(SecurityUtil.verifySignedMessage(null, oldSignedMessage))
    }
}
