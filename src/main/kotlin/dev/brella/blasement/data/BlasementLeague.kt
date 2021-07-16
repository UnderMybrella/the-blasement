package dev.brella.blasement.data

import com.github.benmanes.caffeine.cache.Caffeine
import dev.brella.blasement.endpoints.*
import dev.brella.blasement.endpoints.api.BlaseballApiGetActiveBetsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetIdolsEndpoint
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime

typealias Request = PipelineContext<Unit, ApplicationCall>

@Suppress("SuspendFunctionOnCoroutineScope", "BlockingMethodInNonBlockingContext")
data class BlasementLeague(
    val leagueID: String,
    val json: Json,
    val httpClient: HttpClient,
    /* Api */
    val apiGetUser: BlaseballApiGetUserEndpoint?,
    val apiGetUserRewards: BlaseballApiGetUserRewardsEndpoint?,
    val apiGetActiveBets: BlaseballApiGetActiveBetsEndpoint?,

    val apiGetIdols: BlaseballApiGetIdolsEndpoint?,
    val apiGetTributes: BlaseballApiGetTributesEndpoint?,

    /* Database */
    val databaseGlobalEvents: BlaseballDatabaseGlobalEventsEndpoint?,
    val databaseShopSetup: BlaseballDatabaseShopSetupEndpoint?,

    val databaseGlobalFeed: BlaseballDatabaseFeedEndpoint.Global?,
    val databaseGameFeed: BlaseballDatabaseFeedEndpoint.Game?,
    val databasePlayerFeed: BlaseballDatabaseFeedEndpoint.Player?,
    val databaseTeamFeed: BlaseballDatabaseFeedEndpoint.Team?,
    val databaseStoryFeed: BlaseballDatabaseFeedEndpoint.Story?,
    val databaseFeedByPhase: BlaseballDatabaseFeedByPhaseEndpoint?,

    val databasePlayerNames: BlaseballDatabasePlayerNamesEndpoint?,
    val databasePlayers: BlaseballDatabasePlayersEndpoint?,
    val databaseOffseasonRecap: BlaseballDatabaseOffseasonRecapEndpoint?,
    val databaseOffseasonSetup: BlaseballDatabaseOffseasonSetupEndpoint?,

    val databaseSunSun: BlaseballDatabaseSunSunEndpoint?,
    val databaseVault: BlaseballDatabaseVaultEndpoint?,

    val databaseAllDivisions: BlaseballDatabaseAllDivisionsEndpoint?,
    val databaseAllTeams: BlaseballDatabaseAllTeamsEndpoint?,

    val databaseCommunityChestProgress: BlaseballDatabaseCommunityChestProgressEndpoint?,
    val databaseBonusResults: BlaseballDatabaseBonusResultsEndpoint?,
    val databaseDecreeResults: BlaseballDatabaseDecreeResultsEndpoint?,
    val databaseEventResults: BlaseballDatabaseEventResultsEndpoint?,

    val databaseGameById: BlaseballDatabaseGameByIdEndpoint?,
    val databaseGetPreviousChamp: BlaseballDatabaseGetPreviousChampEndpoint?,
    val databaseGiftProgress: BlaseballDatabaseGiftProgressEndpoint?,

    val databaseItems: BlaseballDatabaseItemsEndpoint?,
    val databasePlayersByItemId: BlaseballDatabasePlayersByItemEndpoint?,
    val databasePlayoffs: BlaseballDatabasePlayoffsEndpoint?,
    val databaseRenovationProgress: BlaseballDatabaseRenovationProgressEndpoint?,
    val databaseRenovations: BlaseballDatabaseRenovationsEndpoint?,

    val databaseSubleague: BlaseballDatabaseSubleagueEndpoint?,
    val databaseTeam: BlaseballDatabaseTeamEndpoint?,
    val databaseTeamElectionStats: BlaseballDatabaseTeamElectionStatsEndpoint?,

    val eventsStreamData: BlaseballEventsStreamDataEndpoint?,

    val clock: BlasementClock
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob()
    val siteData = BlasementSiteData(
        httpClient, indexHtmlTransformers = listOf(
            SiteTransformer.InitialTextTransformer.ReplaceStaticAssets("/leagues/underground")
        ),
        mainJsTransformers = listOf(
            SiteTransformer.InitialTextTransformer.ReplaceApiCalls("/leagues/underground"),
            SiteTransformer.FinalTextTransformer.ReplaceTimeWithWebsocket("/leagues/underground")
        ),
        twoJsTransformers = listOf(),
        mainCssTransformers = listOf(),
        clock
    )

    val streamData: SharedFlow<String> =
        eventsStreamData?.setupFlow(this)
        ?: emptyFlow<String>()
            .shareIn(this, SharingStarted.Lazily, 1)

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
                    streamData.collect { data ->
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

    init {
        siteData.launch(this)
    }
}