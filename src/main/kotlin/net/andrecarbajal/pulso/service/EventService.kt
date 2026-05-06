package net.andrecarbajal.pulso.service

import net.andrecarbajal.pulso.model.IngestRequest
import net.andrecarbajal.pulso.model.MetricsResult
import net.andrecarbajal.pulso.model.ProjectSummary
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

    fun metrics(appId: String?): Mono<MetricsResult> = Mono.zip(
        repository.countLast24h(appId),
        repository.topFeatures(appId).collectList(),
        repository.errorStats(appId).collectList(),
        repository.byOs(appId).collectList()
    ).map { t ->
        MetricsResult(
            executions24h = t.t1,
            topFeatures = t.t2,
            errors = t.t3,
            byOs = t.t4
        )
    }

    fun projects(): Mono<List<ProjectSummary>> = repository.projectSummaries().collectList()
}
