package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getJsonObjectOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import io.ktor.application.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.util.*

interface BlaseballDatabaseGameByIdEndpoint : BlaseballEndpoint {
    object Chronicler : BlaseballDatabaseGameByIdEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            (league.httpClient.get<JsonObject>("https://api.sibr.dev/chronicler/v1/games/updates") {
                parameter("game", request.call.parameters["id"])
                parameter("count", 1)
                parameter("order", "desc")
            }.getJsonArrayOrNull("data")?.firstOrNull() as? JsonObject)?.getJsonObjectOrNull("data")

        override fun describe(): JsonElement? =
            JsonPrimitive("chronicler")
    }

    object Live : BlaseballDatabaseGameByIdEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.get("https://www.blaseball.com/database/gameById/${request.call.parameters["id"]}")

        override fun describe(): JsonElement? =
            JsonPrimitive("live")
    }

    data class Static(val data: JsonElement?) : BlaseballDatabaseGameByIdEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            data

        override fun describe(): JsonElement? =
            buildJsonObject {
                put("type", "static")
                put("data", data ?: JsonNull)
            }
    }

    companion object {
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabaseGameByIdEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Chronicler
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler
                            "live" -> Live
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler
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