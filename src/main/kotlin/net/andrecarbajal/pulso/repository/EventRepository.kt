package net.andrecarbajal.pulso.repository

import net.andrecarbajal.pulso.model.ErrorStat
import net.andrecarbajal.pulso.model.FeatureStat
import net.andrecarbajal.pulso.model.OsStat
import net.andrecarbajal.pulso.model.ProjectSummary
import net.andrecarbajal.pulso.model.TelemetryEvent
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import java.time.Instant
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface EventRepository : ReactiveCrudRepository<TelemetryEvent, Long> {

    @Query(
        """
        SELECT count(*) FROM events
        WHERE time > NOW() - INTERVAL '24 hours'
    """
    )
    fun countLast24h(): Mono<Long>

    @Query(
        """
        SELECT count(*) FROM events
        WHERE time > NOW() - INTERVAL '24 hours'
          AND (:appId IS NULL OR app_id = :appId)
    """
    )
    fun countLast24h(appId: String?): Mono<Long>

    @Query(
        """
        SELECT feature, count(*) as total
        FROM events
        WHERE event_type = 'feature_used'
          AND time > NOW() - INTERVAL '24 hours'
          AND feature IS NOT NULL
        GROUP BY feature
        ORDER BY total DESC
        LIMIT 10
    """
    )
    fun topFeatures(): Flux<FeatureStat>

    @Query(
        """
        SELECT feature, count(*) as total
        FROM events
        WHERE event_type = 'feature_used'
          AND time > NOW() - INTERVAL '24 hours'
          AND feature IS NOT NULL
          AND (:appId IS NULL OR app_id = :appId)
        GROUP BY feature
        ORDER BY total DESC
        LIMIT 10
    """
    )
    fun topFeatures(appId: String?): Flux<FeatureStat>

    @Query(
        """
        SELECT error_type, count(*) as total
        FROM events
        WHERE event_type = 'error'
          AND time > NOW() - INTERVAL '24 hours'
          AND error_type IS NOT NULL
        GROUP BY error_type
        ORDER BY total DESC
    """
    )
    fun errorStats(): Flux<ErrorStat>

    @Query(
        """
        SELECT error_type, count(*) as total
        FROM events
        WHERE event_type = 'error'
          AND time > NOW() - INTERVAL '24 hours'
          AND error_type IS NOT NULL
          AND (:appId IS NULL OR app_id = :appId)
        GROUP BY error_type
        ORDER BY total DESC
    """
    )
    fun errorStats(appId: String?): Flux<ErrorStat>

    @Query(
        """
        SELECT os, count(*) as total
        FROM events
        WHERE time > NOW() - INTERVAL '24 hours'
        GROUP BY os
        ORDER BY total DESC
    """
    )
    fun byOs(): Flux<OsStat>

    @Query(
        """
        SELECT os, count(*) as total
        FROM events
        WHERE time > NOW() - INTERVAL '24 hours'
          AND (:appId IS NULL OR app_id = :appId)
        GROUP BY os
        ORDER BY total DESC
    """
    )
    fun byOs(appId: String?): Flux<OsStat>

    @Query(
        """
        SELECT COALESCE(arch, 'unknown') as arch, count(*) as total
        FROM events
        WHERE time > NOW() - INTERVAL '24 hours'
          AND (:appId IS NULL OR app_id = :appId)
        GROUP BY COALESCE(arch, 'unknown')
        ORDER BY total DESC
    """
    )
    fun byArch(appId: String?): Flux<ArchRow>

    @Query(
        """
        SELECT event_type, count(*) as total
        FROM events
        WHERE time > NOW() - INTERVAL '24 hours'
          AND (:appId IS NULL OR app_id = :appId)
        GROUP BY event_type
        ORDER BY total DESC
    """
    )
    fun byEventType(appId: String?): Flux<EventTypeRow>

    @Query(
        """
        SELECT app_version, count(*) as total
        FROM events
        WHERE time > NOW() - INTERVAL '24 hours'
          AND (:appId IS NULL OR app_id = :appId)
        GROUP BY app_version
        ORDER BY total DESC
        LIMIT 10
    """
    )
    fun byAppVersion(appId: String?): Flux<AppVersionRow>

    @Query(
        """
        SELECT COUNT(DISTINCT session_id)
        FROM events
        WHERE time > NOW() - INTERVAL '24 hours'
          AND (:appId IS NULL OR app_id = :appId)
    """
    )
    fun sessions24h(appId: String?): Mono<Long>

    @Query(
        """
        SELECT
          time,
          app_id,
          app_version,
          os,
          arch,
          feature,
          error_type,
          error_message,
          session_id
        FROM events
        WHERE
          event_type = 'error'
          AND time > NOW() - INTERVAL '24 hours'
          AND error_type IS NOT NULL
          AND (:appId IS NULL OR app_id = :appId)
        ORDER BY time DESC
        LIMIT 20
    """
    )
    fun recentErrors(appId: String?): Flux<RecentErrorRow>

    @Query(
        """
        SELECT app_id, count(*) AS total
        FROM events
        WHERE app_id IS NOT NULL
        GROUP BY app_id
        ORDER BY total DESC, app_id ASC
    """
    )
    fun projectSummaries(): Flux<ProjectSummary>

    @Query(
        """
        SELECT
          feature,
          COUNT(*) AS calls,
          AVG(duration_ms) FILTER (WHERE duration_ms IS NOT NULL) AS avg_duration_ms,
          PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY duration_ms)
            FILTER (WHERE duration_ms IS NOT NULL) AS p95_duration_ms,
          COUNT(*) FILTER (WHERE error_type IS NOT NULL)::float / COUNT(*) AS error_rate,
          COUNT(DISTINCT session_id) AS unique_sessions,
          COUNT(duration_ms) AS duration_samples
        FROM events
        WHERE
          event_type = 'feature_used'
          AND feature IS NOT NULL
          AND time > NOW() - CAST(:range AS interval)
          AND (:appId IS NULL OR app_id = :appId)
        GROUP BY feature
        ORDER BY calls DESC
    """
    )
    fun featureStatsBase(appId: String?, range: String): Flux<FeatureStatsBaseRow>

    @Query(
        """
        SELECT
          feature,
          time_bucket(CAST(:bucketSize AS interval), time) AS bucket,
          COUNT(*) AS calls
        FROM events
        WHERE
          event_type = 'feature_used'
          AND feature IS NOT NULL
          AND time IS NOT NULL
          AND time > NOW() - CAST(:range AS interval)
          AND (:appId IS NULL OR app_id = :appId)
        GROUP BY feature, bucket
        ORDER BY feature, bucket
    """
    )
    fun featureStatsBuckets(appId: String?, range: String, bucketSize: String): Flux<FeatureBucketRow>

    @Query(
        """
        WITH current_period AS (
          SELECT feature, COUNT(*) AS calls
          FROM events
          WHERE
            event_type = 'feature_used'
            AND feature IS NOT NULL
            AND time > NOW() - CAST(:range AS interval)
            AND (:appId IS NULL OR app_id = :appId)
          GROUP BY feature
        ),
        previous_period AS (
          SELECT feature, COUNT(*) AS calls
          FROM events
          WHERE
            event_type = 'feature_used'
            AND feature IS NOT NULL
            AND time BETWEEN NOW() - (CAST(:range AS interval) * 2) AND NOW() - CAST(:range AS interval)
            AND (:appId IS NULL OR app_id = :appId)
          GROUP BY feature
        )
        SELECT
          c.feature,
          ROUND((c.calls - COALESCE(p.calls, 0))::numeric / NULLIF(p.calls, 0) * 100, 1) AS trend_pct
        FROM current_period c
        LEFT JOIN previous_period p USING (feature)
    """
    )
    fun featureStatsTrend(appId: String?, range: String): Flux<FeatureTrendRow>

    @Query(
        """
        SELECT COUNT(DISTINCT feature)
        FROM events
        WHERE
          event_type = 'feature_used'
          AND feature IS NOT NULL
          AND time > NOW() - CAST(:range AS interval)
          AND (:appId IS NULL OR app_id = :appId)
    """
    )
    fun featureDistinctTotal(appId: String?, range: String): Mono<Long>
}

interface FeatureStatsBaseRow {
    val feature: String
    val calls: Long?
    val avgDurationMs: Double?
    val p95DurationMs: Double?
    val errorRate: Double?
    val uniqueSessions: Long?
    val durationSamples: Long?
}

interface FeatureBucketRow {
    val feature: String?
    val bucket: Instant?
    val calls: Long?
}

interface FeatureTrendRow {
    val feature: String
    val trendPct: Double?
}

interface ArchRow {
    val arch: String
    val total: Long?
}

interface EventTypeRow {
    val eventType: String
    val total: Long?
}

interface AppVersionRow {
    val appVersion: String
    val total: Long?
}

interface RecentErrorRow {
    val time: Instant?
    val appId: String
    val appVersion: String
    val os: String
    val arch: String?
    val feature: String?
    val errorType: String
    val errorMessage: String?
    val sessionId: String
}
