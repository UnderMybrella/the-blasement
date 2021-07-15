package dev.brella.blasement.plugins

import dev.brella.blasement.data.BlasementClock
import dev.brella.blasement.data.LeagueRegistry
import dev.brella.blasement.endpoints.*
import dev.brella.blasement.endpoints.api.BlaseballApiGetActiveBetsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetIdolsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetTributesEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserRewardsEndpoint
import dev.brella.blasement.endpoints.database.BlaseballDatabaseFeedEndpoint
import dev.brella.blasement.endpoints.database.BlaseballDatabaseOffseasonSetupEndpoint
import dev.brella.blasement.endpoints.database.BlaseballDatabasePlayerNamesEndpoint
import dev.brella.blasement.endpoints.database.BlaseballDatabasePlayersEndpoint
import dev.brella.blasement.endpoints.database.BlaseballDatabaseShopSetupEndpoint
import dev.brella.blasement.endpoints.database.BlaseballDatabaseSunSunEndpoint
import dev.brella.blasement.endpoints.database.BlaseballDatabaseVaultEndpoint
import dev.brella.blasement.endpoints.database.buildGlobalEvents
import dev.brella.blasement.endpoints.database.setAllUpnuts
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

val registry = LeagueRegistry()

fun Application.configureRouting() {
    try {
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

        registry.registerLeague("underground", json, httpClient) {
            api {
                getUser = BlaseballApiGetUserEndpoint.GuestSibr.Season20
                getUserRewards = BlaseballApiGetUserRewardsEndpoint.GuestSibr.Season20
                getActiveBets = BlaseballApiGetActiveBetsEndpoint.GuestSibr.Season20
                getIdols = BlaseballApiGetIdolsEndpoint.Chronicler
                getTribute = BlaseballApiGetTributesEndpoint.Chronicler
            }

            database {
                feed {
                    setAllUpnuts { arrayOf(TGB) }
                }

                globalEvents = buildGlobalEvents {
                    this["brella"] = "\uD83C\uDFB5 Under My Umbrella \uD83C\uDFB5"
                    this["upnuts"] = "What's Up, Scales?"
                    this["tbc"] = "Now that Blaseball isn't updating every ten minutes, I can actually work on this"
                    this["parker"] = "Parker's still in The Vault, right?"
                }

//                databaseGlobalEvents = BlaseballGlobalEventsEndpoint.Chronicler
                shopSetup = BlaseballDatabaseShopSetupEndpoint.Chronicler
                playerNamesIds = BlaseballDatabasePlayerNamesEndpoint.ChroniclerInefficient
                players = BlaseballDatabasePlayersEndpoint.Chronicler
                offseasonSetup = BlaseballDatabaseOffseasonSetupEndpoint.Chronicler
                vault = BlaseballDatabaseVaultEndpoint.Chronicler
                sunSun = BlaseballDatabaseSunSunEndpoint.Chronicler
            }

            eventsStreamData = BlaseballEventsStreamDataEndpoint.Chronicler

            clock = BlasementClock.UnboundedFrom(Instant.parse("2021-06-15T00:00:00Z"), accelerationRate = 2.5) //BlasementClock.Static(time)
        }
    } catch (th: Throwable) {
        th.printStackTrace()
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
