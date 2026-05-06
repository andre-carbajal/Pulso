package net.andrecarbajal.pulso.model

data class FeatureStat(val feature: String, val total: Long)
data class ErrorStat(val errorType: String, val total: Long)
data class OsStat(val os: String, val total: Long)
data class ArchStat(val arch: String, val total: Long)
data class EventTypeStat(val eventType: String, val total: Long)
data class AppVersionStat(val appVersion: String, val total: Long)
data class RecentError(
    val time: String,
    val appId: String,
    val appVersion: String,
    val os: String,
    val arch: String?,
    val feature: String?,
    val errorType: String,
    val errorMessage: String?,
    val sessionId: String
)
data class ProjectSummary(val appId: String, val total: Long)

data class MetricsResult(
    val executions24h: Long,
    val sessions24h: Long,
    val topFeatures: List<FeatureStat>,
    val errors: List<ErrorStat>,
    val byOs: List<OsStat>,
    val byArch: List<ArchStat>,
    val byEventType: List<EventTypeStat>,
    val byAppVersion: List<AppVersionStat>,
    val recentErrors: List<RecentError>
)
