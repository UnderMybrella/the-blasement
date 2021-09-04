package dev.brella.blasement.endpoints

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.api.BlaseballApiGetActiveBetsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetIdolsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetRisingStarsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetTributesEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserRewardsEndpoint
import dev.brella.blasement.endpoints.database.*
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getChroniclerEntityList
import dev.brella.blasement.plugins.json
import io.ktor.application.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface BlaseballEndpoint {
    suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?

    fun describe(): JsonElement?
}

interface BlaseballUpdatableEndpoint {
    suspend fun updateDataFor(league: BlasementLeague, call: ApplicationCall)
    suspend fun updateDataForWebSocket(league: BlasementLeague, session: WebSocketServerSession, call: ApplicationCall)
}

data class Live(val path: String, val transformers: List<JsonTransformer> = emptyList()) :
    BlaseballEndpoint,

    BlaseballApiGetActiveBetsEndpoint,
    BlaseballApiGetIdolsEndpoint,
    BlaseballApiGetRisingStarsEndpoint,
    BlaseballApiGetTributesEndpoint,
    BlaseballApiGetUserEndpoint,
    BlaseballApiGetUserRewardsEndpoint,

    BlaseballDatabaseAllDivisionsEndpoint,
    BlaseballDatabaseAllTeamsEndpoint,
    BlaseballDatabaseBonusResultsEndpoint,
    BlaseballDatabaseCommunityChestProgressEndpoint,
    BlaseballDatabaseDecreeResultsEndpoint,
    BlaseballDatabaseEventResultsEndpoint,
    BlaseballDatabaseFeedByPhaseEndpoint,

    BlaseballDatabaseFeedEndpoint.Game,
    BlaseballDatabaseFeedEndpoint.Global,
    BlaseballDatabaseFeedEndpoint.Player,
    BlaseballDatabaseFeedEndpoint.Team,
    BlaseballDatabaseFeedEndpoint.Story,

    BlaseballDatabaseGameByIdEndpoint,
    BlaseballDatabaseGetPreviousChampEndpoint,
    BlaseballDatabaseGiftProgressEndpoint,
    BlaseballDatabaseGlobalEventsEndpoint,
    BlaseballDatabaseItemsEndpoint,
    BlaseballDatabaseOffseasonRecapEndpoint,
    BlaseballDatabaseOffseasonSetupEndpoint,
    BlaseballDatabasePlayerNamesEndpoint,
    BlaseballDatabasePlayersByItemEndpoint,
    BlaseballDatabasePlayersEndpoint,
    BlaseballDatabasePlayoffsEndpoint,
    BlaseballDatabaseRenovationProgressEndpoint,
    BlaseballDatabaseRenovationsEndpoint,
    BlaseballDatabaseShopSetupEndpoint,
    BlaseballDatabaseSubleagueEndpoint,
    BlaseballDatabaseSunSunEndpoint,
    BlaseballDatabaseTeamElectionStatsEndpoint,
    BlaseballDatabaseTeamEndpoint,
    BlaseballDatabaseVaultEndpoint {
    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
        transformers.fold(league.httpClient.get("${league.liveBaseUrl}${path}") {
            request.call.request.queryParameters.flattenForEach { k, v -> parameter(k, v) }

            timeout {
                socketTimeoutMillis = 20_000
            }
        }) { json, transformer -> transformer(json) }

    override fun describe(): JsonElement =
        buildJsonObject {
            put("type", "live")
            put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
        }
}

data class Chronicler(val type: String, val transformers: List<JsonTransformer> = emptyList()) :
    BlaseballEndpoint,

    BlaseballApiGetActiveBetsEndpoint,
    BlaseballApiGetIdolsEndpoint,
    BlaseballApiGetRisingStarsEndpoint,
    BlaseballApiGetTributesEndpoint,
    BlaseballApiGetUserEndpoint,
    BlaseballApiGetUserRewardsEndpoint,

    BlaseballDatabaseAllDivisionsEndpoint,
    BlaseballDatabaseAllTeamsEndpoint,
    BlaseballDatabaseBonusResultsEndpoint,
    BlaseballDatabaseCommunityChestProgressEndpoint,
    BlaseballDatabaseDecreeResultsEndpoint,
    BlaseballDatabaseEventResultsEndpoint,
    BlaseballDatabaseFeedByPhaseEndpoint,

    BlaseballDatabaseFeedEndpoint.Game,
    BlaseballDatabaseFeedEndpoint.Global,
    BlaseballDatabaseFeedEndpoint.Player,
    BlaseballDatabaseFeedEndpoint.Team,
    BlaseballDatabaseFeedEndpoint.Story,

    BlaseballDatabaseGameByIdEndpoint,
    BlaseballDatabaseGetPreviousChampEndpoint,
    BlaseballDatabaseGiftProgressEndpoint,
    BlaseballDatabaseGlobalEventsEndpoint,
    BlaseballDatabaseItemsEndpoint,
    BlaseballDatabaseOffseasonRecapEndpoint,
    BlaseballDatabaseOffseasonSetupEndpoint,
    BlaseballDatabasePlayerNamesEndpoint,
    BlaseballDatabasePlayersByItemEndpoint,
    BlaseballDatabasePlayersEndpoint,
    BlaseballDatabasePlayoffsEndpoint,
    BlaseballDatabaseRenovationProgressEndpoint,
    BlaseballDatabaseRenovationsEndpoint,
    BlaseballDatabaseShopSetupEndpoint,
    BlaseballDatabaseSubleagueEndpoint,
    BlaseballDatabaseSunSunEndpoint,
    BlaseballDatabaseTeamElectionStatsEndpoint,
    BlaseballDatabaseTeamEndpoint,
    BlaseballDatabaseVaultEndpoint {
    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
        transformers(league.httpClient.getChroniclerEntity(league.chroniclerBaseUrl, "idols", league.clock.getTime()) {
            parameter("id", request.call.request.queryParameters["ids"])
        } ?: JsonNull)

    override fun describe(): JsonElement =
        buildJsonObject {
            put("type", "chronicler")
            put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
        }
}

data class ChroniclerList(val type: String, val transformers: List<JsonTransformer> = emptyList()) :
    BlaseballEndpoint,

    BlaseballApiGetActiveBetsEndpoint,
    BlaseballApiGetIdolsEndpoint,
    BlaseballApiGetRisingStarsEndpoint,
    BlaseballApiGetTributesEndpoint,
    BlaseballApiGetUserEndpoint,
    BlaseballApiGetUserRewardsEndpoint,

    BlaseballDatabaseAllDivisionsEndpoint,
    BlaseballDatabaseAllTeamsEndpoint,
    BlaseballDatabaseBonusResultsEndpoint,
    BlaseballDatabaseCommunityChestProgressEndpoint,
    BlaseballDatabaseDecreeResultsEndpoint,
    BlaseballDatabaseEventResultsEndpoint,
    BlaseballDatabaseFeedByPhaseEndpoint,

    BlaseballDatabaseFeedEndpoint.Game,
    BlaseballDatabaseFeedEndpoint.Global,
    BlaseballDatabaseFeedEndpoint.Player,
    BlaseballDatabaseFeedEndpoint.Team,
    BlaseballDatabaseFeedEndpoint.Story,

    BlaseballDatabaseGameByIdEndpoint,
    BlaseballDatabaseGetPreviousChampEndpoint,
    BlaseballDatabaseGiftProgressEndpoint,
    BlaseballDatabaseGlobalEventsEndpoint,
    BlaseballDatabaseItemsEndpoint,
    BlaseballDatabaseOffseasonRecapEndpoint,
    BlaseballDatabaseOffseasonSetupEndpoint,
    BlaseballDatabasePlayerNamesEndpoint,
    BlaseballDatabasePlayersByItemEndpoint,
    BlaseballDatabasePlayersEndpoint,
    BlaseballDatabasePlayoffsEndpoint,
    BlaseballDatabaseRenovationProgressEndpoint,
    BlaseballDatabaseRenovationsEndpoint,
    BlaseballDatabaseShopSetupEndpoint,
    BlaseballDatabaseSubleagueEndpoint,
    BlaseballDatabaseSunSunEndpoint,
    BlaseballDatabaseTeamElectionStatsEndpoint,
    BlaseballDatabaseTeamEndpoint,
    BlaseballDatabaseVaultEndpoint {
    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
        transformers(JsonArray(league.httpClient.getChroniclerEntityList(league.chroniclerBaseUrl, "idols", league.clock.getTime()) {
            parameter("id", request.call.request.queryParameters["ids"])
        } ?: emptyList()))

    override fun describe(): JsonElement =
        buildJsonObject {
            put("type", "chronicler")
            put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
        }
}

data class Static(var data: JsonElement, val transformers: List<JsonTransformer> = emptyList()) :
    BlaseballEndpoint,

    BlaseballApiGetActiveBetsEndpoint,
    BlaseballApiGetIdolsEndpoint,
    BlaseballApiGetRisingStarsEndpoint,
    BlaseballApiGetTributesEndpoint,
    BlaseballApiGetUserEndpoint,
    BlaseballApiGetUserRewardsEndpoint,

    BlaseballDatabaseAllDivisionsEndpoint,
    BlaseballDatabaseAllTeamsEndpoint,
    BlaseballDatabaseBonusResultsEndpoint,
    BlaseballDatabaseCommunityChestProgressEndpoint,
    BlaseballDatabaseDecreeResultsEndpoint,
    BlaseballDatabaseEventResultsEndpoint,
    BlaseballDatabaseFeedByPhaseEndpoint,

    BlaseballDatabaseFeedEndpoint.Game,
    BlaseballDatabaseFeedEndpoint.Global,
    BlaseballDatabaseFeedEndpoint.Player,
    BlaseballDatabaseFeedEndpoint.Team,
    BlaseballDatabaseFeedEndpoint.Story,

    BlaseballDatabaseGameByIdEndpoint,
    BlaseballDatabaseGetPreviousChampEndpoint,
    BlaseballDatabaseGiftProgressEndpoint,
    BlaseballDatabaseGlobalEventsEndpoint,
    BlaseballDatabaseItemsEndpoint,
    BlaseballDatabaseOffseasonRecapEndpoint,
    BlaseballDatabaseOffseasonSetupEndpoint,
    BlaseballDatabasePlayerNamesEndpoint,
    BlaseballDatabasePlayersByItemEndpoint,
    BlaseballDatabasePlayersEndpoint,
    BlaseballDatabasePlayoffsEndpoint,
    BlaseballDatabaseRenovationProgressEndpoint,
    BlaseballDatabaseRenovationsEndpoint,
    BlaseballDatabaseShopSetupEndpoint,
    BlaseballDatabaseSubleagueEndpoint,
    BlaseballDatabaseSunSunEndpoint,
    BlaseballDatabaseTeamElectionStatsEndpoint,
    BlaseballDatabaseTeamEndpoint,
    BlaseballDatabaseVaultEndpoint,

    BlaseballUpdatableEndpoint {
    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement = transformers.fold(data) { json, transformer -> transformer(json) }

    override suspend fun updateDataFor(league: BlasementLeague, call: ApplicationCall) {
        data = call.receiveOrNull() ?: JsonNull
    }

    override suspend fun updateDataForWebSocket(league: BlasementLeague, session: WebSocketServerSession, call: ApplicationCall) {
        session.incoming.receiveAsFlow()
            .filterIsInstance<Frame.Text>()
            .onEach { frame ->
                data = json.parseToJsonElement(frame.readText())
            }.launchIn(session)
            .join()
    }

    override fun describe(): JsonElement =
        buildJsonObject {
            put("type", "static")
            put("data", data)
            put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
        }
}
