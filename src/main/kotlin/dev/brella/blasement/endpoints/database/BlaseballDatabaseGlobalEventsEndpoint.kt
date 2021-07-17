package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.BlasementLeagueBuilder
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import kotlinx.serialization.json.*
import java.util.*

interface BlaseballDatabaseGlobalEventsEndpoint : BlaseballEndpoint {
    data class Static(val events: JsonElement?) : BlaseballDatabaseGlobalEventsEndpoint {
        constructor(vararg events: Triple<String, String, Long?>) : this(buildJsonArray {
            events.forEach { (id, msg, expire) ->
                addJsonObject {
                    put("id", id)
                    put("msg", msg)
                    put("expire", expire)
                }
            }
        })

        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? = events
        override fun describe(): JsonElement? =
            buildJsonObject {
                put("type", "static")
                put("data", events ?: JsonNull)
            }
    }

    object Chronicler : BlaseballDatabaseGlobalEventsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.getChroniclerEntity("globalevents", league.clock.getTime())

        override fun describe() = JsonPrimitive("chronicler")
    }

    companion object {
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabaseGlobalEventsEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Chronicler
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler
                            "static" -> Static(config["data"])
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }
}

inline class GlobalEventsBuilder(val builder: JsonArrayBuilder) {
    inline fun add(id: String, msg: String, expire: Long? = null) =
        builder.addJsonObject {
            put("id", id)
            put("msg", msg)
            put("expire", expire)
        }

    inline operator fun set(id: String, msg: String) =
        add(id, msg, null)
}

inline fun BlasementLeagueBuilder.Database.buildGlobalEvents(init: GlobalEventsBuilder.() -> Unit) =
    BlaseballDatabaseGlobalEventsEndpoint.Static(buildJsonArray { GlobalEventsBuilder(this).init() })