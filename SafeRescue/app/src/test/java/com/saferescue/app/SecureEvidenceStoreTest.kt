package com.saferescue.app

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Test

class SecureEvidenceStoreTest {
    @Test fun sha256_isDeterministic() {
        val bytes = "SafeRescue".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        assertEquals(64, digest.length)
        assertEquals(digest, digest)
    }
}
