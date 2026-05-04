package net.andrecarbajal.pulso.handler

import net.andrecarbajal.pulso.model.IngestRequest
import net.andrecarbajal.pulso.service.EventService
import jakarta.validation.Validator
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyToMono
import reactor.core.publisher.Mono

@Component
class IngestHandler(
    private val service: EventService,
    private val validator: Validator
) {
    fun handle(req: ServerRequest): Mono<ServerResponse> =
        req.bodyToMono<IngestRequest>()
            .flatMap { body ->
                val violations = validator.validate(body)
                if (violations.isNotEmpty()) {
                    val errors = violations.map { "${it.propertyPath}: ${it.message}" }
                    ServerResponse.badRequest().bodyValue(mapOf("errors" to errors))
                } else {
                    service.ingest(body)
                        .then(
                            ServerResponse.status(HttpStatus.ACCEPTED)
                                .bodyValue(mapOf("status" to "accepted"))
                        )
                }
            }
}
