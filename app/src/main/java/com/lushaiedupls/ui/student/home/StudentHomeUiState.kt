package com.lushaiedupls.ui.student.home

import com.lushaiedupls.data.mock.AttendanceRecord
import com.lushaiedupls.data.mock.OverviewMetric
import com.lushaiedupls.data.mock.SessionSummary

data class StudentHomeUiState(
    val displayName: String = "",
    val notificationCount: Int = 0,
    val overviewMetrics: List<OverviewMetric> = emptyList(),
    val sessionSummary: SessionSummary? = null,
    val attendancePreview: List<AttendanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val needsApproval: Boolean = false,
    val errorMessage: String? = null,
)
