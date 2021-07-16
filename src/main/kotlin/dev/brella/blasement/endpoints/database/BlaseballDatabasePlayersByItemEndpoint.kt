package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntityList
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getString
import dev.brella.blasement.getStringOrNull
import io.ktor.application.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

fun interface BlaseballDatabasePlayersByItemEndpoint : BlaseballEndpoint {
    object ChroniclerInefficient : BlaseballDatabasePlayersByItemEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? {
            val id = request.call.request.queryParameters["id"] ?: return null
            return league.httpClient.getChroniclerEntityList("player", league.clock.getTime())
                ?.mapNotNull { element ->
                    val player = element as? JsonObject ?: return@mapNotNull null
                    val items = player.getJsonArrayOrNull("items")
                                    ?.filterIsInstance<JsonObject>()
                                ?: return@mapNotNull null

                    if (items.any { it.getStringOrNull("id") == id }) {
                        return@mapNotNull JsonObject(player + Pair("items", JsonArray(items.map { it.getValue("id") })))
                    } else {
                        return@mapNotNull null
                    }
                }
                ?.let(::JsonArray)
        }
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}