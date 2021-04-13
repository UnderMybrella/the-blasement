import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    application
}

group = "dev.brella"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://maven.brella.dev")
    maven(url = "https://kotlin.bintray.com/ktor")
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")

    implementation("io.ktor:ktor-server-netty:1.5.0")
    implementation("io.ktor:ktor-client-serialization:1.5.0")
    implementation("io.ktor:ktor-client-encoding:1.5.0")
    implementation("io.ktor:ktor-serialization:1.5.0")
    implementation("io.ktor:ktor-html-builder:1.5.0")
    implementation("io.ktor:ktor-websockets:1.5.0")

    implementation("dev.brella:kornea-io:5.2.0-alpha")
    implementation("dev.brella:kornea-toolkit:3.3.1-alpha")

    implementation("io.ktor:ktor-client-apache:1.5.0")
    implementation("io.ktor:ktor-client-encoding:1.5.0")
    implementation("io.ktor:ktor-client-core-jvm:1.5.0")
    implementation("dev.brella:ktornea-apache:1.0.0-alpha")
    implementation("dev.brella:ktornea-utils:1.1.0-alpha")

    implementation("dev.brella:kornea-blaseball:1.3.1-alpha")

    implementation("org.jetbrains.kotlinx:atomicfu:0.15.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "ServerKt"
}