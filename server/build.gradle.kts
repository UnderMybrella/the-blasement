import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("io.spring.dependency-management") version "1.0.6.RELEASE"
}

group = "dev.brella"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://maven.brella.dev")
    maven(url = "https://kotlin.bintray.com/ktor")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.security:spring-security-bom:5.4.6")
    }
}

dependencies {
    val ktor_version = "1.5.3"

    implementation("dev.brella:kornea-blaseball-base:2.3.3-alpha")
    implementation("dev.brella:kornea-blaseball-api:2.3.1-alpha") {
        exclude(module = "kornea-blaseball-base")
    }

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")


    implementation("io.ktor:ktor-html-builder:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")

    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")

    implementation("dev.brella:kornea-io:5.2.0-alpha")
    implementation("dev.brella:kornea-toolkit:3.3.1-alpha")

    implementation("io.ktor:ktor-client-okhttp:$ktor_version")
    implementation("io.ktor:ktor-client-encoding:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-serialization:$ktor_version")
    implementation("io.ktor:ktor-client-encoding:$ktor_version")

    implementation("dev.brella:ktornea-utils:1.2.3-alpha")

    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("org.jetbrains.kotlinx:atomicfu:0.16.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

    implementation("io.jsonwebtoken:jjwt-api:0.11.2")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.2")
    // Uncomment the next line if you want to use RSASSA-PSS (PS256, PS384, PS512) algorithms:
    implementation("org.bouncycastle:bcprov-jdk15on:1.68")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.2") // or "io.jsonwebtoken:jjwt-gson:0.11.2' for gson

    implementation("org.springframework.data:spring-data-r2dbc:1.3.0")
    implementation("io.r2dbc:r2dbc-postgresql:0.8.7.RELEASE")
    implementation("io.r2dbc:r2dbc-h2:0.8.4.RELEASE")
    implementation("io.r2dbc:r2dbc-pool:0.9.0.M1")

    implementation("org.springframework.security:spring-security-web")
    implementation("org.springframework.security:spring-security-config")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.5.0-RC")

    implementation("com.github.ben-manes.caffeine:caffeine:3.0.1")

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