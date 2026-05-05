package net.andrecarbajal.pulso.service

import net.andrecarbajal.pulso.model.FeatureAnalyticsStat
import net.andrecarbajal.pulso.model.FeatureStatsResponse
import net.andrecarbajal.pulso.model.FeatureSummary
import net.andrecarbajal.pulso.repository.EventRepository
import net.andrecarbajal.pulso.repository.FeatureStatsBaseRow
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class FeatureStatsService(
    private val repository: EventRepository
) {
    fun compute(appId: String?, range: String): Mono<FeatureStatsResponse> {
        val normalizedRange = range.trim().lowercase()
        val spec = RangeSpec.from(normalizedRange)
            ?: return Mono.error(IllegalArgumentException("Invalid range '$range'. Allowed values: 1h, 6h, 24h, 7d"))

        val baseMono = repository.featureStatsBase(appId, normalizedRange).collectList()
        val bucketsMono = repository.featureStatsBuckets(appId, normalizedRange, spec.bucketInterval).collectList()
        val trendMono = repository.featureStatsTrend(appId, normalizedRange).collectList()
        val totalDistinctMono = repository.featureDistinctTotal(appId, normalizedRange)

        return Mono.zip(baseMono, bucketsMono, trendMono, totalDistinctMono)
            .map { tuple ->
                val baseRows = tuple.t1
                val bucketRows = tuple.t2
                val trendRows = tuple.t3
                val totalDistinct = tuple.t4.toInt()

                val trendByFeature = trendRows.associate { it.feature to it.trendPct }

                val sortedBucketCountsByFeature = bucketRows
                    .groupBy { it.feature }
                    .mapValues { entry ->
                        entry.value
                            .sortedBy { it.bucket }
                            .map { it.calls }
                    }

                val features = baseRows.map { row ->
                    val existingSeries = sortedBucketCountsByFeature[row.feature].orEmpty()
                    val paddedSeries = padSeries(existingSeries, spec.points)

                    FeatureAnalyticsStat(
                        feature = row.feature,
                        calls = row.calls,
                        avgDurationMs = row.avgDurationMs,
                        p95DurationMs = row.p95DurationMs,
                        errorRate = row.errorRate,
                        uniqueSessions = row.uniqueSessions,
                        trendPct = trendByFeature[row.feature],
                        hourly = paddedSeries
                    )
                }

                FeatureStatsResponse(
                    range = normalizedRange,
                    appId = appId,
                    summary = buildSummary(baseRows, totalDistinct),
                    features = features
                )
            }
    }

    private fun buildSummary(baseRows: List<FeatureStatsBaseRow>, totalDistinct: Int): FeatureSummary {
        val totalCalls = baseRows.sumOf { it.calls }
        val weightedDurationSum = baseRows.sumOf {
            if (it.avgDurationMs == null || it.durationSamples <= 0) {
                0.0
            } else {
                it.avgDurationMs * it.durationSamples
            }
        }
        val totalDurationSamples = baseRows.sumOf { it.durationSamples }
        val avgDurationMs = if (totalDurationSamples > 0) {
            weightedDurationSum / totalDurationSamples
        } else {
            0.0
        }

        return FeatureSummary(
            totalCalls = totalCalls,
            avgDurationMs = avgDurationMs,
            activeFeatures = baseRows.count { it.calls > 0 },
            totalFeatures = totalDistinct
        )
    }

    private fun padSeries(values: List<Long>, expectedSize: Int): List<Long> {
        if (values.size >= expectedSize) {
            return values.takeLast(expectedSize)
        }
        val missing = expectedSize - values.size
        return List(missing) { 0L } + values
    }
}

private data class RangeSpec(
    val bucketInterval: String,
    val points: Int
) {
    companion object {
        fun from(range: String): RangeSpec? = when (range) {
            "1h" -> RangeSpec("5 minutes", 12)
            "6h" -> RangeSpec("30 minutes", 12)
            "24h" -> RangeSpec("2 hours", 12)
            "7d" -> RangeSpec("1 day", 7)
            else -> null
        }
    }
}
