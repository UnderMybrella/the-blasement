package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.BlasementLeagueBuilder
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put

fun interface BlaseballDatabaseGlobalEventsEndpoint : BlaseballEndpoint {
    data class Static(val events: JsonElement) : BlaseballDatabaseGlobalEventsEndpoint {
        constructor(vararg events: Triple<String, String, Long?>) : this(buildJsonArray {
            events.forEach { (id, msg, expire) ->
                addJsonObject {
                    put("id", id)
                    put("msg", msg)
                    put("expire", expire)
                }
            }
        })

        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement = events
    }

    object Chronicler : BlaseballDatabaseGlobalEventsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.getChroniclerEntity("globalevents", league.clock.getTime())
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
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