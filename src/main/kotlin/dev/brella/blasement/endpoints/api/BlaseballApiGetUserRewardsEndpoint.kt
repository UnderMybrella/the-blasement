package dev.brella.blasement.endpoints.api

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.*

interface BlaseballApiGetUserRewardsEndpoint : BlaseballEndpoint {
    object Empty : BlaseballApiGetUserRewardsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
            buildJsonObject {
                put("coins", 0)
                put("lightMode", false)
                put("peanuts", 0)
                putJsonArray("toasts") {}
            }

        override fun describe() = JsonPrimitive("empty")
    }

    data class Static(val data: JsonElement?): BlaseballApiGetUserRewardsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            data

        override fun describe() = buildJsonObject {
            put("type", "static")
            put("data", data ?: JsonNull)
        }
    }

    companion object {
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballApiGetUserRewardsEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Empty
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "empty" -> Empty
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "empty" -> Empty
                            "static" -> Static(config["data"])
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }
}