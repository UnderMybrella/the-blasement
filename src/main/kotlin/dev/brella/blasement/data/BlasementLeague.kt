package dev.brella.blasement.data

import com.github.benmanes.caffeine.cache.Caffeine
import dev.brella.blasement.endpoints.*
import dev.brella.blasement.endpoints.api.BlaseballApiGetActiveBetsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetIdolsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetRisingStarsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetTributesEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserRewardsEndpoint
import dev.brella.blasement.endpoints.database.*
import dev.brella.blasement.loopEvery
import dev.brella.blasement.respondJsonObject
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime

typealias Request = PipelineContext<Unit, ApplicationCall>

enum class EnumVisibilityStatus {
    PUBLIC,
    PROTECTED,
    PRIVATE,
}

@Suppress("SuspendFunctionOnCoroutineScope", "BlockingMethodInNonBlockingContext")
data class BlasementLeague(
    val leagueID: String,
    val json: Json,
    val httpClient: HttpClient,

    val visibility: EnumVisibilityStatus,
    val authentication: String,

    val clock: BlasementClock,
    val siteDataClock: BlasementClock = clock,

    /* Api */
    val apiGetUser: BlaseballApiGetUserEndpoint? = null,
    val apiGetUserRewards: BlaseballApiGetUserRewardsEndpoint? = null,
    val apiGetActiveBets: BlaseballApiGetActiveBetsEndpoint? = null,

    val apiGetIdols: BlaseballApiGetIdolsEndpoint? = null,
    val apiGetTributes: BlaseballApiGetTributesEndpoint? = null,

    val apiGetRisingStars: BlaseballApiGetRisingStarsEndpoint? = null,

    /* Database */
    val databaseGlobalEvents: BlaseballDatabaseGlobalEventsEndpoint? = null,
    val databaseShopSetup: BlaseballDatabaseShopSetupEndpoint? = null,

    val databaseGlobalFeed: BlaseballDatabaseFeedEndpoint.Global? = null,
    val databaseGameFeed: BlaseballDatabaseFeedEndpoint.Game? = null,
    val databasePlayerFeed: BlaseballDatabaseFeedEndpoint.Player? = null,
    val databaseTeamFeed: BlaseballDatabaseFeedEndpoint.Team? = null,
    val databaseStoryFeed: BlaseballDatabaseFeedEndpoint.Story? = null,
    val databaseFeedByPhase: BlaseballDatabaseFeedByPhaseEndpoint? = null,

    val databasePlayerNames: BlaseballDatabasePlayerNamesEndpoint? = null,
    val databasePlayers: BlaseballDatabasePlayersEndpoint? = null,
    val databaseOffseasonRecap: BlaseballDatabaseOffseasonRecapEndpoint? = null,
    val databaseOffseasonSetup: BlaseballDatabaseOffseasonSetupEndpoint? = null,

    val databaseSunSun: BlaseballDatabaseSunSunEndpoint? = null,
    val databaseVault: BlaseballDatabaseVaultEndpoint? = null,

    val databaseAllDivisions: BlaseballDatabaseAllDivisionsEndpoint? = null,
    val databaseAllTeams: BlaseballDatabaseAllTeamsEndpoint? = null,

    val databaseCommunityChestProgress: BlaseballDatabaseCommunityChestProgressEndpoint? = null,
    val databaseBonusResults: BlaseballDatabaseBonusResultsEndpoint? = null,
    val databaseDecreeResults: BlaseballDatabaseDecreeResultsEndpoint? = null,
    val databaseEventResults: BlaseballDatabaseEventResultsEndpoint? = null,

    val databaseGameById: BlaseballDatabaseGameByIdEndpoint? = null,
    val databaseGetPreviousChamp: BlaseballDatabaseGetPreviousChampEndpoint? = null,
    val databaseGiftProgress: BlaseballDatabaseGiftProgressEndpoint? = null,

    val databaseItems: BlaseballDatabaseItemsEndpoint? = null,
    val databasePlayersByItemId: BlaseballDatabasePlayersByItemEndpoint? = null,
    val databasePlayoffs: BlaseballDatabasePlayoffsEndpoint? = null,
    val databaseRenovationProgress: BlaseballDatabaseRenovationProgressEndpoint? = null,
    val databaseRenovations: BlaseballDatabaseRenovationsEndpoint? = null,

    val databaseSubleague: BlaseballDatabaseSubleagueEndpoint? = null,
    val databaseTeam: BlaseballDatabaseTeamEndpoint? = null,
    val databaseTeamElectionStats: BlaseballDatabaseTeamElectionStatsEndpoint? = null,

    val eventsStreamData: BlaseballEventsStreamDataEndpoint? = null
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob()
    val siteData = BlasementSiteData(
        httpClient, indexHtmlTransformers = listOf(
            SiteTransformer.InitialTextTransformer.ReplaceStaticAssets("/leagues/$leagueID")
        ),
        mainJsTransformers = listOf(
            SiteTransformer.InitialTextTransformer.ReplaceApiCalls("/leagues/$leagueID"),
            SiteTransformer.FinalTextTransformer.ReplaceTimeWithWebsocket("/leagues/$leagueID")
        ),
        twoJsTransformers = listOf(),
        mainCssTransformers = listOf(),
        siteDataClock
    )

    val streamData: Deferred<SharedFlow<String>> =
        async {
            eventsStreamData?.setupFlow(this@BlasementLeague) ?: MutableSharedFlow()
        }

    @OptIn(ExperimentalTime::class)
    val temporalFlow: SharedFlow<String> =
        flow {
            loopEvery(clock.temporalUpdateTime, { isActive }) {
                emit(clock.getTime().toEpochMilliseconds().toString())
            }
        }.shareIn(this, SharingStarted.Eagerly, 1)

    val dataCache = Caffeine.newBuilder()
        .maximumSize(1_000)
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .buildAsync<String, JsonElement?>()

    suspend inline fun handle(request: Request, endpoint: BlaseballEndpoint?, path: String) {
        if (endpoint == null)
            return request.call.respondJsonObject(HttpStatusCode.ServiceUnavailable) {
                put("error", "$path endpoint unavailable")
            }

        val response = dataCache.get(request.call.request.uri) { _, _ ->
            future { endpoint.getDataFor(this@BlasementLeague, request) }
        }.await() ?: return request.call.respondJsonObject(HttpStatusCode.InternalServerError) {
            put("error", "$path failed to get a proper response")
        }

        request.call.respond(response)
    }

    suspend fun handleApiTime(session: WebSocketServerSession) =
        with(session) {
            temporalFlow.collect { send(it) }
        }

    suspend fun handleEventsStreamData(pipeline: Request) =
        with(pipeline) {
            call.respondTextWriter(ContentType.Text.EventStream) {
                launch(coroutineContext + Dispatchers.IO) {
                    streamData
                        .await()
                        .collect { data ->
                            try {
                                write("data:")
                                write(data)
                                write("\n\n")
                                flush()
                            } catch (th: Throwable) {
                                this@respondTextWriter.close()
                                cancel("An error occurred while writing", th)
                            }
                        }
                }.join()
            }
        }

    suspend fun handleIndexHtml(pipeline: Request) =
        siteData.respondIndexHtml(pipeline.call)

    suspend fun handleMainJs(pipeline: Request) =
        siteData.respondMainJs(pipeline.call)

    suspend fun handleMainCss(pipeline: Request) =
        siteData.respondMainCss(pipeline.call)

    suspend fun handle2Js(pipeline: Request) =
        siteData.respond2Js(pipeline.call)

    fun describe(): JsonObject =
        buildJsonObject {
            put("league_id", leagueID)
            put("protection", visibility.name)

            put("clock", clock.describe() ?: JsonNull)

            put("apiGetUser", apiGetUser?.describe() ?: JsonNull)
            put("apiGetUserRewards", apiGetUserRewards?.describe() ?: JsonNull)
            put("apiGetActiveBets", apiGetActiveBets?.describe() ?: JsonNull)

            put("apiGetIdols", apiGetIdols?.describe() ?: JsonNull)
            put("apiGetTributes", apiGetTributes?.describe() ?: JsonNull)

            put("databaseGlobalEvents", databaseGlobalEvents?.describe() ?: JsonNull)
            put("databaseShopSetup", databaseShopSetup?.describe() ?: JsonNull)

            put("databaseGlobalFeed", databaseGlobalFeed?.describe() ?: JsonNull)
            put("databaseGameFeed", databaseGameFeed?.describe() ?: JsonNull)
            put("databasePlayerFeed", databasePlayerFeed?.describe() ?: JsonNull)
            put("databaseTeamFeed", databaseTeamFeed?.describe() ?: JsonNull)
            put("databaseStoryFeed", databaseStoryFeed?.describe() ?: JsonNull)
            put("databaseFeedByPhase", databaseFeedByPhase?.describe() ?: JsonNull)

            put("databasePlayerNames", databasePlayerNames?.describe() ?: JsonNull)
            put("databasePlayers", databasePlayers?.describe() ?: JsonNull)
            put("databaseOffseasonRecap", databaseOffseasonRecap?.describe() ?: JsonNull)
            put("databaseOffseasonSetup", databaseOffseasonSetup?.describe() ?: JsonNull)

            put("databaseSunSun", databaseSunSun?.describe() ?: JsonNull)
            put("databaseVault", databaseVault?.describe() ?: JsonNull)

            put("databaseAllDivisions", databaseAllDivisions?.describe() ?: JsonNull)
            put("databaseAllTeams", databaseAllTeams?.describe() ?: JsonNull)

            put("databaseCommunityChestProgress", databaseCommunityChestProgress?.describe() ?: JsonNull)
            put("databaseBonusResults", databaseBonusResults?.describe() ?: JsonNull)
            put("databaseDecreeResults", databaseDecreeResults?.describe() ?: JsonNull)
            put("databaseEventResults", databaseEventResults?.describe() ?: JsonNull)

            put("databaseGameById", databaseGameById?.describe() ?: JsonNull)
            put("databaseGetPreviousChamp", databaseGetPreviousChamp?.describe() ?: JsonNull)
            put("databaseGiftProgress", databaseGiftProgress?.describe() ?: JsonNull)

            put("databaseItems", databaseItems?.describe() ?: JsonNull)
            put("databasePlayersByItemId", databasePlayersByItemId?.describe() ?: JsonNull)
            put("databasePlayoffs", databasePlayoffs?.describe() ?: JsonNull)
            put("databaseRenovationProgress", databaseRenovationProgress?.describe() ?: JsonNull)
            put("databaseRenovations", databaseRenovations?.describe() ?: JsonNull)

            put("databaseSubleague", databaseSubleague?.describe() ?: JsonNull)
            put("databaseTeam", databaseTeam?.describe() ?: JsonNull)
            put("databaseTeamElectionStats", databaseTeamElectionStats?.describe() ?: JsonNull)

            put("eventsStreamData", eventsStreamData?.describe() ?: JsonNull)
        }

    init {
        siteData.launch(this)
    }
}