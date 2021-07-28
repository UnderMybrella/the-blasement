package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getChroniclerEntityList
import dev.brella.blasement.getIntOrNull
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

@Deprecated("Not used as of Under/Overbrackets")
interface BlaseballDatabasePlayoffsEndpoint : BlaseballEndpoint {
    object Chronicler : BlaseballDatabasePlayoffsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            request.call.request.queryParameters["number"]?.toIntOrNull()?.let { season ->
                league.httpClient.getChroniclerEntityList("playoffs", league.clock.getTime())
                    ?.firstOrNull { playoff -> (playoff as? JsonObject)?.getIntOrNull("season") == season }
            }

        override fun describe(): JsonElement? =
            JsonPrimitive("chronicler")
    }

    object Live : BlaseballDatabasePlayoffsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.get("https://www.blaseball.com/database/playoffs") {
                parameter("number", request.call.request.queryParameters["number"])
            }

        override fun describe(): JsonElement? =
            JsonPrimitive("live")
    }

    data class Static(val data: JsonElement?): BlaseballDatabasePlayoffsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            data

        override fun describe(): JsonElement? =
            buildJsonObject {
                put("type", "static")
                put("data", data ?: JsonNull)
            }
    }

    companion object {
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabasePlayoffsEndpoint?> {
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