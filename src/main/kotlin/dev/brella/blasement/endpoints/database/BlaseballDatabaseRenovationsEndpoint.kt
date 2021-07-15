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

fun interface BlaseballDatabaseRenovationsEndpoint : BlaseballEndpoint {
    object LiveData : BlaseballDatabaseRenovationsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.get("https://www.blaseball.com/database/renovations") {
                url.parameters.appendAll(request.call.request.queryParameters)
            }
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}