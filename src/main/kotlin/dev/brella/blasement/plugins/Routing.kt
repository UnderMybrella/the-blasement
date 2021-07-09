package dev.brella.blasement.plugins

import dev.brella.blasement.endpoints.BlasementGetUserEndpoint
import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.BlasementSiteData
import dev.brella.blasement.data.SiteTransformer
import dev.brella.blasement.data.setupLeagues
import dev.brella.blasement.endpoints.BlaseballGlobalEventsEndpoint
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
import kotlinx.datetime.Instant

val httpClient = HttpClient(OkHttp) {
    install(ContentEncoding) {
        gzip()
        deflate()
        identity()
    }

    install(JsonFeature) {
        serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        })
    }

    expectSuccess = false

    install(HttpTimeout)

    defaultRequest {
        userAgent("Blasement 1.0.0 (UnderMybrella, https://github.com/UnderMybrella/the-blasement)")
    }
}

fun Application.configureRouting() {
    try {
        install(DoubleReceive)

        val time = Instant.parse("2021-06-15T00:00:00Z")

        routing {
            setupLeagues(
                mapOf(
                    "underground" to BlasementLeague(
                        "underground",
                        httpClient,

                        getUserEndpoint = BlasementGetUserEndpoint.GuestSibr.Season20,
                        /*getGlobalEventsEndpoint = BlaseballGlobalEventsEndpoint.Static(
                            Triple("brella", "\uD83C\uDFB5 Under My Umbrella \uD83C\uDFB5", null),
                            Triple("upnuts", "What's Up, Scales?", null),
                            Triple("tbc", "Now that Blaseball isn't updating every ten minutes, I can actually work on this", null),
                            Triple("parker", "Parker's still in The Vault, right?", null)
                        )*/
                        getGlobalEventsEndpoint = BlaseballGlobalEventsEndpoint.Chronicler
                    ) { time }
                )
            )
        }
    } catch (th: Throwable) {
        th.printStackTrace()
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
