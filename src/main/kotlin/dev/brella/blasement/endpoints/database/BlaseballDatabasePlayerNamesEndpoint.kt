package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntityList
import dev.brella.blasement.getString
import io.ktor.application.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

fun interface BlaseballDatabasePlayerNamesEndpoint: BlaseballEndpoint {
    object ChroniclerInefficient : BlaseballDatabasePlayerNamesEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.getChroniclerEntityList("player", league.clock.getTime())
                ?.filterIsInstance<JsonObject>()
                ?.map { element ->
                    buildJsonObject {
                        put("id", element["id"] ?: JsonNull)
                        put("name", element["name"] ?: JsonNull)
                    }
                }?.sortedBy { it.getString("name") }
                ?.let(::JsonArray)
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}