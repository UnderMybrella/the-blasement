package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntityList
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getString
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import io.ktor.application.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.util.*

interface BlaseballDatabasePlayersByItemEndpoint : BlaseballEndpoint {
    object ChroniclerInefficient : BlaseballDatabasePlayersByItemEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? {
            val id = request.call.request.queryParameters["id"] ?: return null
            return league.httpClient.getChroniclerEntityList("player", league.clock.getTime())
                ?.mapNotNull { element ->
                    val player = element as? JsonObject ?: return@mapNotNull null
                    val items = player.getJsonArrayOrNull("items")
                                    ?.filterIsInstance<JsonObject>()
                                ?: return@mapNotNull null

                    if (items.any { it.getStringOrNull("id") == id }) {
                        return@mapNotNull JsonObject(player + Pair("items", JsonArray(items.map { it.getValue("id") })))
                    } else {
                        return@mapNotNull null
                    }
                }
                ?.let(::JsonArray)
        }

        override fun describe(): JsonElement? =
            JsonPrimitive("chronicler")
    }

    object Live : BlaseballDatabasePlayersByItemEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.get("https://www.blaseball.com/database/playersByItemId") {
                parameter("id", request.call.request.queryParameters["id"])
            }

        override fun describe(): JsonElement? =
            JsonPrimitive("live")
    }

    data class Static(val data: JsonElement?): BlaseballDatabasePlayersByItemEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            data

        override fun describe(): JsonElement? =
            buildJsonObject {
                put("type", "static")
                put("data", data ?: JsonNull)
            }
    }

    companion object {
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabasePlayersByItemEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> ChroniclerInefficient
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "chronicler" -> ChroniclerInefficient
                            "live" -> Live
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "chronicler" -> ChroniclerInefficient
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