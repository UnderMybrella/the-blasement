package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.Chronicler
import dev.brella.blasement.endpoints.ChroniclerList
import dev.brella.blasement.endpoints.JsonTransformer
import dev.brella.blasement.endpoints.Live
import dev.brella.blasement.endpoints.Static
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserRewardsEndpoint
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getChroniclerEntityList
import dev.brella.blasement.getJsonArrayOrNull
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

interface BlaseballDatabaseAllDivisionsEndpoint : BlaseballEndpoint {
    companion object {
        const val TYPE = "division"
        const val PATH = "/database/allDivisions"
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabaseAllDivisionsEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> ChroniclerList(TYPE)
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "chronicler" -> ChroniclerList(TYPE)
                            "live" -> Live(PATH)
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "chronicler" -> ChroniclerList(TYPE, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
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