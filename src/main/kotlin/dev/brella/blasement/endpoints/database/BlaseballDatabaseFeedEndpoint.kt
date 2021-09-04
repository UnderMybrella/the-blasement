package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.BlasementLeagueBuilder
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.JsonTransformer
import dev.brella.blasement.endpoints.Live
import dev.brella.blasement.endpoints.Static
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserRewardsEndpoint
import dev.brella.blasement.endpoints.invoke
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import io.ktor.application.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.serialization.json.*
import java.util.*

interface BlaseballDatabaseFeedEndpoint : BlaseballEndpoint {
    interface Game : BlaseballDatabaseFeedEndpoint
    interface Global : BlaseballDatabaseFeedEndpoint
    interface Player : BlaseballDatabaseFeedEndpoint
    interface Team : BlaseballDatabaseFeedEndpoint
    interface Story : BlaseballDatabaseFeedEndpoint

    class Upnuts(vararg val providers: String, val type: String, val transformers: List<JsonTransformer> = emptyList()) : Game, Global, Player, Team, Story {
        companion object {
            const val TGB = "7fcb63bc-11f2-40b9-b465-f1d458692a63"

            operator fun invoke(type: String, transformers: List<JsonTransformer> = emptyList(), builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = type, transformers = transformers)

            inline fun global(transformers: List<JsonTransformer> = emptyList(), builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = "global", transformers = transformers)

            inline fun game(transformers: List<JsonTransformer> = emptyList(), builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = "game", transformers = transformers)

            inline fun team(transformers: List<JsonTransformer> = emptyList(), builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = "team", transformers = transformers)

            inline fun player(transformers: List<JsonTransformer> = emptyList(), builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = "player", transformers = transformers)

            inline fun story(transformers: List<JsonTransformer> = emptyList(), builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), type = "story", transformers = transformers)
        }

        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
            transformers {
                league.httpClient.get<JsonArray>("https://api.sibr.dev/upnuts/feed/$type") {
                    request.call.request.queryParameters.flattenForEach { k, v -> parameter(k, v) }
                    parameter("time", league.clock.getTime().toEpochMilliseconds())
                    parameter("one_of_providers", providers.joinToString(","))

                    timeout {
                        socketTimeoutMillis = 20_000
                    }
                }
            }

        override fun describe(): JsonElement? =
            buildJsonObject {
                put("type", "upnuts")
                putJsonArray("providers") {
                    providers.forEach { add(it) }
                }
                put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
            }
    }

    companion object {
        const val PATH = "/database/feed"

        infix fun loadGlobalFrom(config: JsonElement?): KorneaResult<Global?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Upnuts(Upnuts.TGB, type = "global")
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "global")
                            "live" -> Live("$PATH/global")
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "global", transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "live" -> Live("$PATH/global", transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "static" -> Static(config["data"] ?: JsonNull, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
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
                            "live" -> Live("$PATH/game")
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "game", transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "live" -> Live("$PATH/game", transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "static" -> Static(config["data"] ?: JsonNull, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
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
                            "live" -> Live("$PATH/player")
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "player", transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "live" -> Live("$PATH/player", transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "static" -> Static(config["data"] ?: JsonNull, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
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
                            "live" -> Live("$PATH/team")
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "team", transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "live" -> Live("$PATH/team", transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "static" -> Static(config["data"] ?: JsonNull, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
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
                            "live" -> Live("$PATH/story")
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, type = "story", transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "live" -> Live("$PATH/story", transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "static" -> Static(config["data"] ?: JsonNull, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
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
    global = BlaseballDatabaseFeedEndpoint.Upnuts.global(emptyList(), builder)
    game = BlaseballDatabaseFeedEndpoint.Upnuts.game(emptyList(), builder)
    player = BlaseballDatabaseFeedEndpoint.Upnuts.player(emptyList(), builder)
    team = BlaseballDatabaseFeedEndpoint.Upnuts.team(emptyList(), builder)
    story = BlaseballDatabaseFeedEndpoint.Upnuts.story(emptyList(), builder)
}