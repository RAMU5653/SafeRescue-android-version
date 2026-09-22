package com.saferescue.app

import com.saferescue.app.auth.RegistrationInput
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {
    @Test
    fun registrationInputCarriesOnlyRequiredFields() {
        val input = RegistrationInput("Test User", "test", "+919999999999", "test@example.com", "StrongPassword123!".toCharArray())
        assertTrue(input.name.isNotBlank())
        assertTrue(input.username.isNotBlank())
        assertTrue(input.phone.length >= 8)
        assertTrue(input.email.contains('@'))
        input.password.fill('\u0000')
    }
}
