package dev.brella.blasement.endpoints

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.getChroniclerEntity
import io.ktor.application.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

fun interface BlaseballShopSetupEndpoint {
    object Chronicler : BlaseballShopSetupEndpoint {
        override suspend fun getShopSetupFor(league: BlasementLeague, call: ApplicationCall): JsonElement? =
            league.httpClient.getChroniclerEntity("shopsetup", league.timeSource())
    }

    suspend fun getShopSetupFor(league: BlasementLeague, call: ApplicationCall): JsonElement?
}