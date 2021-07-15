package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import io.ktor.application.*
import kotlinx.serialization.json.JsonElement

fun interface BlaseballDatabaseCommunityChestProgressEndpoint : BlaseballEndpoint {
    object Chronicler : BlaseballDatabaseCommunityChestProgressEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.getChroniclerEntity("CommunityChestProgress", league.clock.getTime())
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}