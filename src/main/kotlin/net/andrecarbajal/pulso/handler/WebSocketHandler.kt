package net.andrecarbajal.pulso.handler

import net.andrecarbajal.pulso.service.ConnectionManager
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class TelemetryWebSocketHandler(
    private val connectionManager: ConnectionManager
) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        connectionManager.register(session)

        val output = session.send(
            connectionManager.events.map { session.textMessage(it) }
        )

        val input = session.receive()
            .doFinally { connectionManager.unregister(session) }
            .then()

        return Mono.zip(output, input).then()
    }
}
