package net.andrecarbajal.pulso.repository

import net.andrecarbajal.pulso.model.ErrorStat
import net.andrecarbajal.pulso.model.FeatureStat
import net.andrecarbajal.pulso.model.OsStat
import net.andrecarbajal.pulso.model.TelemetryEvent
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
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
        SELECT os, count(*) as total
        FROM events
        WHERE time > NOW() - INTERVAL '24 hours'
        GROUP BY os
        ORDER BY total DESC
    """
    )
    fun byOs(): Flux<OsStat>
}
