package dev.brella.blasement.plugins

import dev.brella.blasement.data.BlasementClock
import dev.brella.blasement.data.LeagueRegistry
import dev.brella.blasement.endpoints.*
import dev.brella.blasement.endpoints.api.BlaseballApiGetActiveBetsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetIdolsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetRisingStarsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetTributesEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserRewardsEndpoint
import dev.brella.blasement.endpoints.database.*
import dev.brella.blasement.property
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.features.*
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.compression.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.http.cio.websocket.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.time.Duration

val json = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
}
val httpClient = HttpClient(OkHttp) {
    install(ContentEncoding) {
        gzip()
        deflate()
        identity()
    }

    install(JsonFeature) {
        serializer = KotlinxSerializer(json)
    }

    expectSuccess = false

    install(HttpTimeout)

    defaultRequest {
        userAgent("Blasement 1.0.0 (UnderMybrella, https://github.com/UnderMybrella/the-blasement)")
    }
}

fun Application.configureRouting() {
    try {
        val registry = LeagueRegistry(
            json,
            httpClient,
            File(
                environment.config.propertyOrNull("blasement.r2dbc_file")?.getString()
                ?: property("blasement_r2dbc")
                ?: "blasement-r2dbc.json"
            ).takeIf(File::exists)
                ?.readText()
                ?.let(json::parseToJsonElement)
                ?.jsonObject ?: error("No r2dbc config provided!")
        )

        install(DoubleReceive)
        install(io.ktor.websocket.WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            registry.setupRouting(this)
        }
    } catch (th: Throwable) {
        th.printStackTrace()
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
