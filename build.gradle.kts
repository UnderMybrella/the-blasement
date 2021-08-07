val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.5.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.20"

    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.bmuschko.docker-remote-api") version "7.0.0"
}

group = "dev.brella"
version = "1.1.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
    maven(url = "https://maven.brella.dev")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")

    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-encoding:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-serialization:$ktor_version")
    implementation("io.ktor:ktor-client-encoding:$ktor_version")

    implementation("com.github.ben-manes.caffeine:caffeine:3.0.3")

    implementation("com.arakelian:java-jq:1.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.5.0")

    implementation("org.springframework.data:spring-data-r2dbc:1.3.0")
    implementation("org.springframework.security:spring-security-crypto:5.5.1")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.69")
    implementation("io.r2dbc:r2dbc-postgresql:0.8.7.RELEASE")
    implementation("io.r2dbc:r2dbc-pool:0.9.0.M1")

    implementation("dev.brella:kornea-errors:2.2.3-alpha")

    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}

application {
    mainClass.set("dev.brella.blasement.ApplicationKt")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    append("META-INF/spring.handlers")
    append("META-INF/spring.schemas")
    append("META-INF/spring.tooling")
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer::class.java) {
        paths = listOf("META-INF/spring.factories")
        mergeStrategy = "append"
    }
}

tasks.create<com.bmuschko.gradle.docker.tasks.image.Dockerfile>("createDockerfile") {
    group = "docker"

    destFile.set(File(rootProject.buildDir, "docker/Dockerfile"))
    from("azul/zulu-openjdk-alpine:11-jre")
    label(
        mapOf(
            "org.opencontainers.image.authors" to "UnderMybrella \"undermybrella@abimon.org\""
        )
    )
    copyFile(tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").get().archiveFileName.get(), "/app/the-blasement.jar")

    copyFile("blasement-r2dbc.json", "/app/blasement-r2dbc.json")
    copyFile("logback.xml", "/app/logback.xml")
    copyFile("application.conf", "/app/application.conf")
    entryPoint("java")
    defaultCommand(
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:MinHeapFreeRatio=20",
        "-XX:MaxHeapFreeRatio=40",
        "-XX:+UseStringDeduplication",
        "-Dlogback.configurationFile=/app/logback.xml",
        "-Dblasement_r2dbc=/app/blasement-r2dbc.json",
        "-jar",
        "/app/the-blasement.jar",
        "-config=/app/application.conf"
    )
}

tasks.create<Sync>("syncShadowJarArchive") {
    group = "docker"

    dependsOn("assemble")
    from(
        tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").get().archiveFile.get().asFile,
        File(rootProject.projectDir, "deployment/application.conf"),
        File(rootProject.projectDir, "deployment/blasement-r2dbc.json"),
        File(rootProject.projectDir, "deployment/logback.xml")
    )

    into(
        tasks.named<com.bmuschko.gradle.docker.tasks.image.Dockerfile>("createDockerfile").get().destFile.get().asFile.parentFile
    )
}

tasks.named("createDockerfile") {
    dependsOn("syncShadowJarArchive")
}

tasks.create<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("buildImage") {
    group = "docker"

    dependsOn("createDockerfile")
    inputDir.set(tasks.named<com.bmuschko.gradle.docker.tasks.image.Dockerfile>("createDockerfile").get().destFile.get().asFile.parentFile)

    images.addAll("undermybrella/the-blasement:$version", "undermybrella/the-blasement:latest")
}

tasks.create<com.bmuschko.gradle.docker.tasks.image.DockerPushImage>("pushImage") {
    group = "docker"
    dependsOn("buildImage")

    images.addAll("undermybrella/the-blasement:$version", "undermybrella/the-blasement:latest")
}