package net.andrecarbajal.pulso.config

import net.andrecarbajal.pulso.handler.TelemetryWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig(private val wsHandler: TelemetryWebSocketHandler) {

    @Bean
    fun webSocketHandlerMapping(): HandlerMapping =
        SimpleUrlHandlerMapping(mapOf("/ws" to wsHandler), -1)

    @Bean
    fun webSocketHandlerAdapter() = WebSocketHandlerAdapter()
}
