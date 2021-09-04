package dev.brella.blasement.endpoints

import dev.brella.blasement.data.BlasementClock
import dev.brella.blasement.data.Request
import dev.brella.blasement.getBooleanOrNull
import dev.brella.blasement.getIntOrNull
import dev.brella.blasement.getJsonObjectOrNull
import dev.brella.blasement.getJsonPrimitive
import dev.brella.blasement.getJsonPrimitiveOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.cast
import dev.brella.kornea.errors.common.getOrBreak
import dev.brella.kornea.errors.common.getOrNull
import dev.brella.kornea.errors.common.successPooled
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.Clock
import java.util.*

interface JsonTransformer {
    companion object {
        infix fun loadAllFrom(config: JsonArray?): List<JsonTransformer> =
            config?.mapNotNull { (JsonTransformer loadFrom it).getOrNull() } ?: emptyList()

        infix fun loadFrom(config: JsonElement?): KorneaResult<JsonTransformer?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull, null -> null
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "scattered" -> Scattered
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "scattered" -> Scattered
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }

    object Scattered : JsonTransformer {
        override fun invoke(json: JsonElement): JsonElement =
            when (json) {
                is JsonObject -> {
                    json.getJsonObjectOrNull("state")
                        ?.let { state ->
                            state.getJsonObjectOrNull("scattered")
                                ?.let { JsonObject(json + it) }

                            ?: state.getJsonPrimitiveOrNull("unscatteredName")
                                ?.let { JsonObject(json + Pair("name", it)) }
                        } ?: JsonObject(json.mapValues { (_, v) -> invoke(v) })
                }

                is JsonArray -> JsonArray(json.map { invoke(it) })
                else -> json
            }

        override fun describe(): JsonElement =
            JsonPrimitive("scattered")
    }

    operator fun invoke(json: JsonElement): JsonElement
    fun describe(): JsonElement
}

inline operator fun List<JsonTransformer>.invoke(json: JsonElement): JsonElement =
    fold(json) { json, transformer -> transformer(json) }

inline operator fun List<JsonTransformer>.invoke(block: () -> JsonElement): JsonElement =
    fold(block()) { json, transformer -> transformer(json) }