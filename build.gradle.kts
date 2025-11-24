plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
}

group = "nl.parkeerassistent"
version = "3.0.1"

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

    testImplementation("io.ktor:ktor-server-test-host-jvm:3.3.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType(JavaExec::class.java) {
    file(".env").takeIf { it.exists() }?.readLines()?.forEach { s ->
        val (key, value) = s.split("=")
        environment(key to value)
    }
}

tasks.test {
    useJUnitPlatform()
}