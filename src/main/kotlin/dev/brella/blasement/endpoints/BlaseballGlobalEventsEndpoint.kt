package dev.brella.blasement.endpoints

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.getChroniclerEntity
import io.ktor.application.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put

fun interface BlaseballGlobalEventsEndpoint {
    data class Static(val events: JsonElement) : BlaseballGlobalEventsEndpoint {
        constructor(vararg events: Triple<String, String, Long?>) : this(buildJsonArray {
            events.forEach { (id, msg, expire) ->
                addJsonObject {
                    put("id", id)
                    put("msg", msg)
                    put("expire", expire)
                }
            }
        })

        override suspend fun getGlobalEventsFor(league: BlasementLeague, call: ApplicationCall): JsonElement = events
    }

    object Chronicler : BlaseballGlobalEventsEndpoint {
        override suspend fun getGlobalEventsFor(league: BlasementLeague, call: ApplicationCall): JsonElement? =
            league.httpClient.getChroniclerEntity("globalevents", league.timeSource())
    }

    suspend fun getGlobalEventsFor(league: BlasementLeague, call: ApplicationCall): JsonElement?
}