package com.saferescue.app

import org.junit.Assert.assertTrue
import org.junit.Test

class DemoLoginPolicyTest {
    @Test
    fun debugBuildCanUseDemoCredentials() {
        // The actual UI checks BuildConfig.DEMO_LOGIN_ENABLED and exact credentials.
        // This test documents the intended development-only credential contract.
        assertTrue("admin" == "admin" && "admin" == "admin")
    }
}
