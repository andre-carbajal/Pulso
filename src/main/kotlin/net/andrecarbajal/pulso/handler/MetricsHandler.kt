package net.andrecarbajal.pulso.handler

import net.andrecarbajal.pulso.service.ConnectionManager
import net.andrecarbajal.pulso.service.EventService
import net.andrecarbajal.pulso.service.FeatureStatsService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class MetricsHandler(
    private val service: EventService,
    private val connectionManager: ConnectionManager,
    private val featureStatsService: FeatureStatsService
) {
    fun metrics(req: ServerRequest): Mono<ServerResponse> =
        service.metrics()
            .flatMap { ServerResponse.ok().bodyValue(it) }

    fun health(req: ServerRequest): Mono<ServerResponse> =
        ServerResponse.ok().bodyValue(
            mapOf("status" to "up", "ws_clients" to connectionManager.activeCount())
        )

    fun featureStats(req: ServerRequest): Mono<ServerResponse> {
        val appId = req.queryParam("appId").orElse(null)
        val range = req.queryParam("range").orElse("24h")

        return featureStatsService.compute(appId, range)
            .flatMap { ServerResponse.ok().bodyValue(it) }
            .onErrorResume(IllegalArgumentException::class.java) { ex ->
                ServerResponse.status(HttpStatus.BAD_REQUEST)
                    .bodyValue(mapOf("error" to (ex.message ?: "invalid request")))
            }
    }
}
