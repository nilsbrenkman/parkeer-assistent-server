plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
}

group = "nl.parkeerassistent"
version = "2.0.7"

application {
    mainClass = "nl.parkeerassistent.ApplicationKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jsoup)
    implementation(libs.kotlin.css)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.metrics)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.logback)
    implementation(libs.micrometer.registry.prometheus)
}

tasks.withType(JavaExec::class.java) {
    file(".env").takeIf { it.exists() }?.readLines()?.forEach { s ->
        val (key, value) = s.split("=")
        environment(key to value)
    }
}
