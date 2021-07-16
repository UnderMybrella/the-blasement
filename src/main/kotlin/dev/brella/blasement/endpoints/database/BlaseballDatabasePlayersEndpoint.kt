package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntityList
import io.ktor.application.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

fun interface BlaseballDatabasePlayersEndpoint: BlaseballEndpoint {
    object Chronicler: BlaseballDatabasePlayersEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            request.call.request.queryParameters["ids"]?.takeIf(String::isNotBlank)?.let { ids ->
                league.httpClient.getChroniclerEntityList("player", league.clock.getTime()) {
                    parameter("id", ids)
                }
            }?.let(::JsonArray) ?: JsonArray(emptyList())
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}