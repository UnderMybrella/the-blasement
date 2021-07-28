package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.BlasementLeagueBuilder
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import io.ktor.application.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.serialization.json.*
import java.util.*

interface BlaseballDatabaseFeedByPhaseEndpoint : BlaseballEndpoint {
    class Upnuts(vararg val providers: String) : BlaseballDatabaseFeedByPhaseEndpoint {
        companion object {
            const val TGB = "7fcb63bc-11f2-40b9-b465-f1d458692a63"

            operator fun invoke(builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder())
        }

        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
            league.httpClient.get<JsonArray>("https://api.sibr.dev/upnuts/feed/global") {
                request.call.request.queryParameters.flattenForEach { k, v -> parameter(k, v) }
                parameter("time", league.clock.getTime().toEpochMilliseconds())
                parameter("one_of_providers", providers.joinToString(","))

                timeout {
                    socketTimeoutMillis = 20_000
                }
            }

        override fun describe(): JsonElement? =
            buildJsonObject {
                put("type", "upnuts")
                putJsonArray("providers") {
                    providers.forEach { add(it) }
                }
            }
    }

    object Live: BlaseballDatabaseFeedByPhaseEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.get<JsonArray>("https://www.blaseball.com/database/feedbyphase") {
                request.call.request.queryParameters.flattenForEach { k, v -> parameter(k, v) }

                timeout {
                    socketTimeoutMillis = 20_000
                }
            }

        override fun describe(): JsonElement? =
            JsonPrimitive("live")
    }

    data class Static(val feed: JsonElement?) : BlaseballDatabaseFeedByPhaseEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? = feed

        override fun describe(): JsonElement? =
            buildJsonObject {
                put("type", "static")
                put("data", feed ?: JsonNull)
            }
    }

    companion object {
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabaseFeedByPhaseEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Upnuts(Upnuts.TGB)
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB)
                            "live" -> Live
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB)
                            "live" -> Live
                            "static" -> Static(config["data"])
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }
}