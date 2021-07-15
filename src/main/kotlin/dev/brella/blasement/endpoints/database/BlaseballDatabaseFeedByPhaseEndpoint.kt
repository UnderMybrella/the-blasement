package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.BlasementLeagueBuilder
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import io.ktor.application.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

interface BlaseballDatabaseFeedByPhaseEndpoint : BlaseballEndpoint {
    class Upnuts(vararg val providers: String) : BlaseballDatabaseFeedByPhaseEndpoint {
        companion object {
            const val TGB = "7fcb63bc-11f2-40b9-b465-f1d458692a63"

            operator fun invoke(builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder())
        }

        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
            league.httpClient.get<JsonArray>("https://api.sibr.dev/upnuts/feed/global") {
                request.call.request.queryParameters.flattenForEach { k, v -> parameter(k, v) }
                parameter("time", league.clock.getTime().toEpochMilliseconds())
                parameter("one_of_providers", providers.joinToString(","))

                timeout {
                    socketTimeoutMillis = 20_000
                }
            }
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}