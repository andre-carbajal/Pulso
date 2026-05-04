import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.5"
    id("org.graalvm.buildtools.native") version "0.11.5"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
}

group = "net.andrecarbajal.pulso"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql:1.1.1.RELEASE")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.fromTarget("25"))
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("pulso")
            mainClass.set("net.andrecarbajal.pulso.TelemetryApplicationKt")
            buildArgs.addAll(
                "--no-fallback",
                "-H:+ReportExceptionStackTraces",
                "--enable-url-protocols=http,https"
            )
        }
    }
    toolchainDetection.set(false)
}
