package net.andrecarbajal.pulso.model

data class FeatureStat(val feature: String, val total: Long)
data class ErrorStat(val errorType: String, val total: Long)
data class OsStat(val os: String, val total: Long)

data class MetricsResult(
    val executions24h: Long,
    val topFeatures: List<FeatureStat>,
    val errors: List<ErrorStat>,
    val byOs: List<OsStat>
)
