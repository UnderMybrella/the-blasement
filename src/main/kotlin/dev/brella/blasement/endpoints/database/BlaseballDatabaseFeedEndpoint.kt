package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.BlasementLeagueBuilder
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserRewardsEndpoint
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import io.ktor.application.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.*

fun interface BlaseballDatabaseFeedEndpoint : BlaseballEndpoint {
    fun interface Game : BlaseballDatabaseFeedEndpoint
    fun interface Global : BlaseballDatabaseFeedEndpoint
    fun interface Player : BlaseballDatabaseFeedEndpoint
    fun interface Team : BlaseballDatabaseFeedEndpoint
    fun interface Story : BlaseballDatabaseFeedEndpoint

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

    data class Static(val feed: JsonElement?) : Game, Global, Player, Team, Story {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? = feed
    }

    companion object {
        infix fun loadGlobalFrom(config: JsonElement?): KorneaResult<Global?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Upnuts(Upnuts.TGB, type = "global")
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "global")
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "global")
                            "static" -> config["data"].let { Global { _, _ -> it } }
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }

        infix fun loadGameFrom(config: JsonElement?): KorneaResult<Game?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Upnuts(Upnuts.TGB, type = "game")
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "game")
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "game")
                            "static" -> config["data"].let { Game { _, _ -> it } }
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }

        infix fun loadPlayerFrom(config: JsonElement?): KorneaResult<Player?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Upnuts(Upnuts.TGB, type = "player")
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "player")
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "player")
                            "static" -> config["data"].let { Player { _, _ -> it } }
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }

        infix fun loadTeamFrom(config: JsonElement?): KorneaResult<Team?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Upnuts(Upnuts.TGB, type = "team")
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "team")
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "team")
                            "static" -> config["data"].let { Team { _, _ -> it } }
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }

        infix fun loadStoryFrom(config: JsonElement?): KorneaResult<Story?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Upnuts(Upnuts.TGB, type = "story")
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "story")
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "story")
                            "static" -> config["data"].let { Story { _, _ -> it } }
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }
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