package dev.brella.blasement.endpoints.api

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import io.ktor.application.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

fun interface BlaseballApiGetUserRewardsEndpoint : BlaseballEndpoint {
    sealed class GuestSibr : BlaseballApiGetUserRewardsEndpoint {
        object Season20 : GuestSibr() {
            override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
                buildJsonObject {
                    put("coins", 0)
                    put("lightMode", false)
                    put("peanuts", 0)
                    putJsonArray("toasts") {}
                }
        }
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}