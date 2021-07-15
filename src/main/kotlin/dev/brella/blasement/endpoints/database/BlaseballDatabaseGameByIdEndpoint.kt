package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getJsonObjectOrNull
import io.ktor.application.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

fun interface BlaseballDatabaseGameByIdEndpoint : BlaseballEndpoint {
    object Chronicler : BlaseballDatabaseGameByIdEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            (league.httpClient.get<JsonObject>("https://api.sibr.dev/chronicler/v1/games/updates") {
                parameter("game", request.call.parameters["id"])
                parameter("count", 1)
                parameter("order", "desc")
            }.getJsonArrayOrNull("data")?.firstOrNull() as? JsonObject)?.getJsonObjectOrNull("data")
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}