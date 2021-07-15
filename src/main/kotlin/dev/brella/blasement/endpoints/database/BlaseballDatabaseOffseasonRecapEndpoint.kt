package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import io.ktor.application.*
import kotlinx.serialization.json.JsonElement

fun interface BlaseballDatabaseOffseasonRecapEndpoint : BlaseballEndpoint {
    object Chronicler : BlaseballDatabaseOffseasonRecapEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.getChroniclerEntity("OffseasonRecap", league.clock.getTime())
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}