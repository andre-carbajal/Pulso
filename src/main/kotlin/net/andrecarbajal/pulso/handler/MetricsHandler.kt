package net.andrecarbajal.pulso.handler

import net.andrecarbajal.pulso.service.ConnectionManager
import net.andrecarbajal.pulso.service.EventService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class MetricsHandler(
    private val service: EventService,
    private val connectionManager: ConnectionManager
) {
    fun metrics(req: ServerRequest): Mono<ServerResponse> =
        service.metrics()
            .flatMap { ServerResponse.ok().bodyValue(it) }

    fun health(req: ServerRequest): Mono<ServerResponse> =
        ServerResponse.ok().bodyValue(
            mapOf("status" to "up", "ws_clients" to connectionManager.activeCount())
        )
}
