package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getChroniclerEntityList
import dev.brella.blasement.getIntOrNull
import io.ktor.application.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Deprecated("Not used as of Under/Overbrackets")
fun interface BlaseballDatabasePlayoffsEndpoint : BlaseballEndpoint {
    object Chronicler : BlaseballDatabasePlayoffsEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            request.call.request.queryParameters["number"]?.toIntOrNull()?.let { season ->
                league.httpClient.getChroniclerEntityList("playoffs", league.clock.getTime())
                    ?.firstOrNull { playoff -> (playoff as? JsonObject)?.getIntOrNull("season") == season }
            }
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}