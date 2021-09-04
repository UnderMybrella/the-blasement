package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.JsonTransformer
import dev.brella.blasement.endpoints.Static
import dev.brella.blasement.endpoints.invoke
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getJsonObjectOrNull
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

interface BlaseballDatabaseGameByIdEndpoint : BlaseballEndpoint {
    data class Chronicler(val transformers: List<JsonTransformer> = emptyList()) : BlaseballDatabaseGameByIdEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            transformers {
                (league.httpClient.get<JsonObject>("https://api.sibr.dev/chronicler/v1/games/updates") {
                    parameter("game", request.call.parameters["id"])
                    parameter("count", 1)
                    parameter("order", "desc")
                }.getJsonArrayOrNull("data")?.firstOrNull() as? JsonObject)?.getJsonObjectOrNull("data") ?: JsonNull
            }

        override fun describe(): JsonElement =
            buildJsonObject {
                put("type", "chronicler")
                put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
            }
    }

    data class Live(val transformers: List<JsonTransformer> = emptyList()) : BlaseballDatabaseGameByIdEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            transformers {
                league.httpClient.get("https://www.blaseball.com/database/gameById/${request.call.parameters["id"]}")
            }

        override fun describe(): JsonElement =
            buildJsonObject {
                put("type", "live")
                put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
            }
    }

    companion object {
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabaseGameByIdEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Chronicler()
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler()
                            "live" -> Live()
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler(JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "live" -> Live(JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "static" -> Static(config["data"] ?: JsonNull, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }
}