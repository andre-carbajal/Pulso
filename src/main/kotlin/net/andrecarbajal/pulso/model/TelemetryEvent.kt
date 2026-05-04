package net.andrecarbajal.pulso.model

import jakarta.validation.constraints.NotBlank
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("events")
data class TelemetryEvent(
    @Id val id: Long? = null,
    @field:NotBlank val appId: String,
    @field:NotBlank val appVersion: String,
    @field:NotBlank val os: String,
    val arch: String? = null,
    @field:NotBlank val eventType: String,
    val feature: String? = null,
    val durationMs: Long? = null,
    val errorType: String? = null,
    val errorMessage: String? = null,
    @field:NotBlank val sessionId: String,
    val time: Instant = Instant.now()
)

data class IngestRequest(
    @field:NotBlank val appId: String,
    @field:NotBlank val appVersion: String,
    @field:NotBlank val os: String,
    val arch: String? = null,
    @field:NotBlank val eventType: String,
    val feature: String? = null,
    val durationMs: Long? = null,
    val errorType: String? = null,
    val errorMessage: String? = null,
    @field:NotBlank val sessionId: String
)

fun IngestRequest.toEvent() = TelemetryEvent(
    appId = appId,
    appVersion = appVersion,
    os = os,
    arch = arch,
    eventType = eventType,
    feature = feature,
    durationMs = durationMs,
    errorType = errorType,
    errorMessage = errorMessage,
    sessionId = sessionId
)
