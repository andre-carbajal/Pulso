package net.andrecarbajal.pulso.model

data class FeatureAnalyticsStat(
    val feature: String,
    val calls: Long,
    val avgDurationMs: Double?,
    val p95DurationMs: Double?,
    val errorRate: Double,
    val uniqueSessions: Long,
    val trendPct: Double?,
    val hourly: List<Long>
)

data class FeatureStatsResponse(
    val range: String,
    val appId: String?,
    val summary: FeatureSummary,
    val features: List<FeatureAnalyticsStat>
)

data class FeatureSummary(
    val totalCalls: Long,
    val avgDurationMs: Double,
    val activeFeatures: Int,
    val totalFeatures: Int
)
