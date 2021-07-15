package dev.brella.blasement.endpoints.api

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import io.ktor.application.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

fun interface BlaseballApiGetActiveBetsEndpoint: BlaseballEndpoint {
    sealed class GuestSibr : BlaseballApiGetActiveBetsEndpoint {
        object Season20 : GuestSibr() {
            val EMPTY = JsonArray(emptyList())
            override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement = EMPTY
        }
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}