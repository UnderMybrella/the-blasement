package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.BlasementLeagueBuilder
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import io.ktor.application.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

interface BlaseballDatabaseFeedEndpoint : BlaseballEndpoint {
    interface Game : BlaseballDatabaseFeedEndpoint
    interface Global : BlaseballDatabaseFeedEndpoint
    interface Player : BlaseballDatabaseFeedEndpoint
    interface Team : BlaseballDatabaseFeedEndpoint
    interface Story : BlaseballDatabaseFeedEndpoint

    class Upnuts(vararg val providers: String, val type: String) : Game, Global, Player, Team, Story {
        companion object {
            const val TGB = "7fcb63bc-11f2-40b9-b465-f1d458692a63"

            operator fun invoke(type: String, builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = type)

            inline fun global(builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = "global")

            inline fun game(builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = "game")

            inline fun team(builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = "team")

            inline fun player(builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = "player")

            inline fun story(builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = "story")
        }

        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
            league.httpClient.get<JsonArray>("https://api.sibr.dev/upnuts/feed/$type") {
                request.call.request.queryParameters.flattenForEach { k, v -> parameter(k, v) }
                parameter("time", league.clock.getTime().toEpochMilliseconds())
                parameter("one_of_providers", providers.joinToString(","))

                timeout {
                    socketTimeoutMillis = 20_000
                }
            }
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}

inline fun <T> BlasementLeagueBuilder.Database.Feed.setAll(feed: T) where T : BlaseballDatabaseFeedEndpoint.Global,
                                                                          T : BlaseballDatabaseFeedEndpoint.Game,
                                                                          T : BlaseballDatabaseFeedEndpoint.Player,
                                                                          T : BlaseballDatabaseFeedEndpoint.Team,
                                                                          T : BlaseballDatabaseFeedEndpoint.Story {
    global = feed
    game = feed
    player = feed
    team = feed
    story = feed
}

inline fun BlasementLeagueBuilder.Database.Feed.setAllUpnuts(builder: BlaseballDatabaseFeedEndpoint.Upnuts.Companion.() -> Array<out String>) {
    global = BlaseballDatabaseFeedEndpoint.Upnuts.global(builder)
    game = BlaseballDatabaseFeedEndpoint.Upnuts.game(builder)
    player = BlaseballDatabaseFeedEndpoint.Upnuts.player(builder)
    team = BlaseballDatabaseFeedEndpoint.Upnuts.team(builder)
    story = BlaseballDatabaseFeedEndpoint.Upnuts.story(builder)
}