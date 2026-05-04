package net.andrecarbajal.pulso

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TelemetryApplication

fun main(args: Array<String>) {
    runApplication<TelemetryApplication>(*args)
}
