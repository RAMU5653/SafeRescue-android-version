package com.saferescue.app.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun defaultTabIsHome() {
        val vm = HomeViewModel()
        assertEquals(HomeTab.HOME, vm.state.value.selectedTab)
    }

    @Test
    fun selectingTabUpdatesOnlyNavigationState() {
        val vm = HomeViewModel()
        vm.selectTab(HomeTab.EVIDENCE)
        assertEquals(HomeTab.EVIDENCE, vm.state.value.selectedTab)
    }
}
