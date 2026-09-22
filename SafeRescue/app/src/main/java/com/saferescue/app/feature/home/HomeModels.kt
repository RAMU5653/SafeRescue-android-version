package com.saferescue.app.feature.home

/**
 * Phase 3 dashboard navigation. This is UI navigation state only; it does not
 * perform emergency operations or claim that later-phase sensors are active.
 */
enum class HomeTab(val label: String) {
    HOME("Home"),
    SAFETY("Safety"),
    EVIDENCE("Evidence"),
    ME("Me")
}

data class DashboardUiState(
    val selectedTab: HomeTab = HomeTab.HOME
)
