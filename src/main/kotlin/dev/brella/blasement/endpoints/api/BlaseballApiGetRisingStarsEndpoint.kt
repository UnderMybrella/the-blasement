package dev.brella.blasement.endpoints.api

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import io.ktor.application.*
import kotlinx.serialization.json.JsonElement

fun interface BlaseballApiGetRisingStarsEndpoint : BlaseballEndpoint {
    object Chronicler : BlaseballApiGetRisingStarsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            league.httpClient.getChroniclerEntity("risingstars", league.clock.getTime())
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}