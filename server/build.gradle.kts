import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
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
    implementation("io.ktor:ktor-client-okhttp:1.5.0")
    implementation("io.ktor:ktor-client-encoding:1.5.0")
    implementation("io.ktor:ktor-client-core-jvm:1.5.0")
    implementation("dev.brella:ktornea-apache:1.0.0-alpha")
    implementation("dev.brella:ktornea-utils:1.2.2-alpha")

    implementation("dev.brella:kornea-blaseball-base:2.1.2-alpha")
    implementation("dev.brella:kornea-blaseball-api:2.1.3-alpha")

    implementation("org.jetbrains.kotlinx:atomicfu:0.15.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

    implementation("io.jsonwebtoken:jjwt-api:0.11.2")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.2")
    // Uncomment the next line if you want to use RSASSA-PSS (PS256, PS384, PS512) algorithms:
    implementation("org.bouncycastle:bcprov-jdk15on:1.60")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.2") // or "io.jsonwebtoken:jjwt-gson:0.11.2' for gson

//    implementation("org.ufoss.kotysa:kotysa-spring-r2dbc:0.2.3")
//    implementation("org.springframework:spring-r2dbc:1.0.0.RELEASE")
    implementation("io.r2dbc:r2dbc-h2:0.8.4.RELEASE")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.4.3")

    implementation(project(":blasement-common"))
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