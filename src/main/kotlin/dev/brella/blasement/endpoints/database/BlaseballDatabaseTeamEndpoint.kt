package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getChroniclerEntityList
import io.ktor.application.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

fun interface BlaseballDatabaseTeamEndpoint : BlaseballEndpoint {
    object Chronicler : BlaseballDatabaseTeamEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.getChroniclerEntity("team", league.clock.getTime()) {
                parameter("id", request.call.request.queryParameters["id"])
            }
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}