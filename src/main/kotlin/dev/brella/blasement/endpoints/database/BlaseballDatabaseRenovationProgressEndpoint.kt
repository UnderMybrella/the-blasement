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

fun interface BlaseballDatabaseRenovationProgressEndpoint : BlaseballEndpoint {
    object Chronicler : BlaseballDatabaseRenovationProgressEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.getChroniclerEntityList("RenovationProgress", league.clock.getTime())
                ?.let(::JsonArray)
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}