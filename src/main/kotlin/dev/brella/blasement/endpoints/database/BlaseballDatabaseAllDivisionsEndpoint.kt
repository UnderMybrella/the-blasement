package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getChroniclerEntityList
import io.ktor.application.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

fun interface BlaseballDatabaseAllDivisionsEndpoint : BlaseballEndpoint {
    object Chronicler : BlaseballDatabaseAllDivisionsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.getChroniclerEntityList("division", league.clock.getTime())
                ?.let(::JsonArray)
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}