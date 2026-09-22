package com.saferescue.app.core.report

import com.saferescue.app.core.risk.RiskSeverity
import com.saferescue.app.core.timeline.TimelineEvent

data class IncidentReport(
    val incidentId: String,
    val generatedAtMillis: Long,
    val finalRiskScore: Int,
    val finalRiskSeverity: RiskSeverity,
    val timeline: List<TimelineEvent>,
    val evidenceCount: Int,
    val locationAvailable: Boolean,
    val voiceMonitoringUsed: Boolean
)
