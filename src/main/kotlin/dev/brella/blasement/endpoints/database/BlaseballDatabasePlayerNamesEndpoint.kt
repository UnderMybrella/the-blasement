package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.JsonTransformer
import dev.brella.blasement.endpoints.Live
import dev.brella.blasement.endpoints.Static
import dev.brella.blasement.endpoints.invoke
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

interface BlaseballDatabasePlayerNamesEndpoint : BlaseballEndpoint {
    data class ChroniclerInefficient(val transformers: List<JsonTransformer> = emptyList()) : BlaseballDatabasePlayerNamesEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            transformers {
                league.httpClient.getChroniclerEntityList("player", league.clock.getTime())
                    ?.filterIsInstance<JsonObject>()
                    ?.map { element ->
                        buildJsonObject {
                            put("id", element["id"] ?: JsonNull)
                            put("name", element["name"] ?: JsonNull)
                        }
                    }?.sortedBy { it.getString("name") }
                    ?.let(::JsonArray)
                ?: JsonNull
            }

        override fun describe(): JsonElement = buildJsonObject {
            put("type", "chronicler")
            put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
        }
    }

    companion object {
        const val PATH = "/database/playerNamesIds"
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabasePlayerNamesEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> ChroniclerInefficient()
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "chronicler" -> ChroniclerInefficient()
                            "live" -> Live(PATH)
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "chronicler" -> ChroniclerInefficient(JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "live" -> Live(PATH, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "static" -> Static(config["data"] ?: JsonNull, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }
}