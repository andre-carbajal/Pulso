package net.andrecarbajal.pulso.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

@Component
class ConnectionManager(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val sink: Sinks.Many<String> = Sinks.many().multicast().onBackpressureBuffer()
    val events: Flux<String> = sink.asFlux()

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    fun register(session: WebSocketSession) {
        sessions[session.id] = session
        log.info("Dashboard conectado: ${session.id} | activos: ${sessions.size}")
    }

    fun unregister(session: WebSocketSession) {
        sessions.remove(session.id)
        log.info("Dashboard desconectado: ${session.id} | activos: ${sessions.size}")
    }

    fun broadcast(payload: Any) {
        val json = objectMapper.writeValueAsString(
            mapOf("type" to "new_event", "payload" to payload)
        )
        sink.tryEmitNext(json)
    }

    fun activeCount(): Int = sessions.size
}
