package net.andrecarbajal.pulso.service

import net.andrecarbajal.pulso.model.IngestRequest
import net.andrecarbajal.pulso.model.MetricsResult
import net.andrecarbajal.pulso.model.ProjectSummary
import net.andrecarbajal.pulso.model.AppVersionStat
import net.andrecarbajal.pulso.model.ArchStat
import net.andrecarbajal.pulso.model.EventTypeStat
import net.andrecarbajal.pulso.model.RecentError
import net.andrecarbajal.pulso.model.toEvent
import net.andrecarbajal.pulso.repository.EventRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class EventService(
    private val repository: EventRepository,
    private val connectionManager: ConnectionManager
) {
    fun ingest(request: IngestRequest): Mono<Void> {
        val event = request.toEvent()
        return repository.save(event)
            .doOnSuccess { saved -> connectionManager.broadcast(saved) }
            .then()
    }

    fun metrics(appId: String?): Mono<MetricsResult> {
        val versionsAndRecentErrorsMono = Mono.zip(
            repository.byAppVersion(appId).collectList(),
            repository.recentErrors(appId).collectList()
        )

        return Mono.zip(
        repository.countLast24h(appId),
        repository.sessions24h(appId),
        repository.topFeatures(appId).collectList(),
        repository.errorStats(appId).collectList(),
        repository.byOs(appId).collectList(),
        repository.byArch(appId).collectList(),
        repository.byEventType(appId).collectList(),
        versionsAndRecentErrorsMono
    ).map { t ->
        val byAppVersion = t.t8.t1
        val recentErrors = t.t8.t2

        MetricsResult(
            executions24h = t.t1,
            sessions24h = t.t2,
            topFeatures = t.t3,
            errors = t.t4,
            byOs = t.t5,
            byArch = t.t6.map { ArchStat(arch = it.arch, total = it.total ?: 0L) },
            byEventType = t.t7.map { EventTypeStat(eventType = it.eventType, total = it.total ?: 0L) },
            byAppVersion = byAppVersion.map { AppVersionStat(appVersion = it.appVersion, total = it.total ?: 0L) },
            recentErrors = recentErrors.map {
                RecentError(
                    time = it.time?.toString() ?: "",
                    appId = it.appId,
                    appVersion = it.appVersion,
                    os = it.os,
                    arch = it.arch,
                    feature = it.feature,
                    errorType = it.errorType,
                    errorMessage = it.errorMessage,
                    sessionId = it.sessionId
                )
            }
        )
    }
    }

    fun projects(): Mono<List<ProjectSummary>> = repository.projectSummaries().collectList()
}
