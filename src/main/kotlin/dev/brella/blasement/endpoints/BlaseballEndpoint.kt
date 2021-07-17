package dev.brella.blasement.endpoints

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

interface BlaseballEndpoint {
    suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?

    fun describe(): JsonElement?
}