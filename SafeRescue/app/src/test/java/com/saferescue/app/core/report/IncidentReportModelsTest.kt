package com.saferescue.app.core.report

import com.saferescue.app.core.risk.RiskSeverity
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentReportModelsTest {
    @Test fun reportPreservesRiskSummary() {
        val report = IncidentReport("id", 1L, 80, RiskSeverity.CRITICAL, emptyList(), 2, true, true)
        assertEquals(80, report.finalRiskScore)
        assertEquals(RiskSeverity.CRITICAL, report.finalRiskSeverity)
        assertEquals(2, report.evidenceCount)
    }
}
