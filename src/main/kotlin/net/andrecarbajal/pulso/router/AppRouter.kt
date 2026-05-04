package net.andrecarbajal.pulso.router

import net.andrecarbajal.pulso.handler.IngestHandler
import net.andrecarbajal.pulso.handler.MetricsHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class AppRouter(
    private val ingestHandler: IngestHandler,
    private val metricsHandler: MetricsHandler
) {
    @Bean
    fun routes(): RouterFunction<ServerResponse> = router {
        POST("/ingest", ingestHandler::handle)
        GET("/metrics", metricsHandler::metrics)
        GET("/health", metricsHandler::health)
    }
}
