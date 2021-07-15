package dev.brella.blasement.endpoints

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import kotlinx.serialization.json.JsonElement

fun interface BlaseballEndpoint {
    suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}