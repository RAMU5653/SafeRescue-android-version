package com.saferescue.app

import com.saferescue.app.security.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun samePasswordVerifies() {
        val result = PasswordHasher.hash("StrongPassword123!".toCharArray())
        assertTrue(PasswordHasher.verify("StrongPassword123!".toCharArray(), result.salt, result.hash))
    }

    @Test
    fun wrongPasswordFails() {
        val result = PasswordHasher.hash("StrongPassword123!".toCharArray())
        assertFalse(PasswordHasher.verify("WrongPassword123!".toCharArray(), result.salt, result.hash))
    }

    @Test
    fun randomSaltProducesDifferentHashes() {
        val first = PasswordHasher.hash("StrongPassword123!".toCharArray())
        val second = PasswordHasher.hash("StrongPassword123!".toCharArray())
        assertFalse(first.salt.contentEquals(second.salt))
        assertFalse(first.hash.contentEquals(second.hash))
    }
}
