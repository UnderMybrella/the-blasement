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
import io.ktor.client.utils.*
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    val liveBaseUrl: String = "https://www.blaseball.com",
    val chroniclerBaseUrl: String = "https://api.sibr.dev/chronicler",
    val upnutsBaseUrl: String = "https://api.sibr.dev/upnuts",

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
    companion object {
        const val LEAGUE_ID = "league_id"
        const val VISIBILITY = "visibility"

        const val DEFAULTS = "defaults"
        const val API_DEFAULTS = "apiDefaults"
        const val FEED_DEFAULTS = "databaseFeedDefaults"
        const val DATABASE_DEFAULTS = "databaseDefaults"

        const val CLOCK = "clock"
        const val API_GET_USER = "apiGetUser"
        const val API_GET_USER_REWARDS = "apiGetUserRewards"
        const val API_GET_ACTIVE_BETS = "apiGetActiveBets"

        const val API_GET_IDOLS = "apiGetIdols"
        const val API_GET_TRIBUTES = "apiGetTributes"
        const val API_GET_RISING_STARS = "apiGetRisingStars"

        const val DATABASE_GLOBAL_EVENTS = "databaseGlobalEvents"
        const val DATABASE_SHOP_SETUP = "databaseShopSetup"

        const val DATABASE_GLOBAL_FEED = "databaseFeedGlobal"
        const val DATABASE_GAME_FEED = "databaseFeedGame"
        const val DATABASE_PLAYER_FEED = "databaseFeedPlayer"
        const val DATABASE_TEAM_FEED = "databaseFeedTeam"
        const val DATABASE_STORY_FEED = "databaseFeedStory"
        const val DATABASE_FEED_BY_PHASE = "databaseFeedByPhase"

        const val DATABASE_PLAYER_NAMES_AND_IDS = "databasePlayerNames"
        const val DATABASE_PLAYERS = "databasePlayers"
        const val DATABASE_OFFSEASON_RECAP = "databaseOffseasonRecap"
        const val DATABASE_OFFSEASON_SETUP = "databaseOffseasonSetup"

        const val DATABASE_SUN_SUN = "databaseSunSun"
        const val DATABASE_VAULT = "databaseVault"

        const val DATABASE_ALL_DIVISIONS = "databaseAllDivisions"
        const val DATABASE_ALL_TEAMS = "databaseAllTeams"

        const val DATABASE_COMMUNITY_CHEST_PROGRESS = "databaseCommunityChestProgress"
        const val DATABASE_BONUS_RESULTS = "databaseBonusResults"
        const val DATABASE_DECREE_RESULTS = "databaseDecreeResults"
        const val DATABASE_EVENT_RESULTS = "databaseEventResults"

        const val DATABASE_GAME_BY_ID = "databaseGameById"
        const val DATABASE_GET_PREVIOUS_CHAMP = "databaseGetPreviousChamp"
        const val DATABASE_GIFT_PROGRESS = "databaseGiftProgress"

        const val DATABASE_ITEMS = "databaseItems"
        const val DATABASE_PLAYERS_BY_ITEM_ID = "databasePlayersByItemId"
        const val DATABASE_PLAYOFFS = "databasePlayoffs"
        const val DATABASE_RENOVATION_PROGRESS = "databaseRenovationProgress"
        const val DATABASE_RENOVATIONS = "databaseRenovations"

        const val DATABASE_SUBLEAGUE = "databaseSubleague"
        const val DATABASE_TEAM = "databaseTeam"
        const val DATABASE_TEAM_ELECTION_STATS = "databaseTeamElectionStats"

        const val EVENTS_STREAM_DATA = "eventsStreamData"
    }

    override val coroutineContext: CoroutineContext = SupervisorJob()

    val siteData = BlasementSiteData(
        httpClient, indexHtmlTransformers = listOf(
            SiteTransformer.InitialTextTransformer.ReplaceStaticAssets("/leagues/$leagueID")
        ),
        mainJsTransformers = listOf(
            SiteTransformer.InitialTextTransformer.ReplaceApiCalls("/leagues/$leagueID"),
            SiteTransformer.InitialTextTransformer.AddNewBeingsJs,
            SiteTransformer.InitialTextTransformer.AllowCustomEmojis,

            SiteTransformer.FinalTextTransformer.ReplaceDateWithCursedTimeWebsocket("/leagues/$leagueID"),
        ),
        twoJsTransformers = listOf(),
        mainCssTransformers = listOf(
            SiteTransformer.InitialTextTransformer.AddTweetStylesCss,
            SiteTransformer.InitialTextTransformer.AddCustomEmojisCss
        ),
        siteDataClock
    )

    val streamData: Deferred<SharedFlow<String>> =
        async {
            eventsStreamData?.setupFlow(this@BlasementLeague) ?: MutableSharedFlow()
        }

    @OptIn(ExperimentalTime::class)
    val temporalFlow: SharedFlow<String> =
        flow<String> {
            loopEvery(clock.temporalUpdateTime, { isActive }) {
                emit(clock.getTime().toEpochMilliseconds().toString())
            }
        }.shareIn(this, SharingStarted.Eagerly, 1)

    val dataCache = Caffeine.newBuilder()
        .maximumSize(1_000)
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .buildAsync<String, JsonElement?>()

    suspend inline fun handlePut(request: Request, endpoint: BlaseballEndpoint?, path: String) {
        if (endpoint == null) {
            return request.call.respondJsonObject(HttpStatusCode.ServiceUnavailable) {
                put("error", "$path endpoint unavailable")
            }
        }

        if (endpoint !is BlaseballUpdatableEndpoint) {
            return request.call.respondJsonObject(HttpStatusCode.MethodNotAllowed) {
                put("error", "$path endpoint does not support updating data")
            }
        }

        endpoint.updateDataFor(this, request.call)

        if (request.call.response.status() == null) request.call.respond(HttpStatusCode.Created, EmptyContent)
    }
    suspend inline fun handleWebSocket(session: WebSocketServerSession, call: ApplicationCall, endpoint: BlaseballEndpoint?, path: String) {
        if (endpoint == null) {
            return call.respondJsonObject(HttpStatusCode.ServiceUnavailable) {
                put("error", "$path endpoint unavailable")
            }
        }

        if (endpoint !is BlaseballUpdatableEndpoint) {
            return call.respondJsonObject(HttpStatusCode.MethodNotAllowed) {
                put("error", "$path endpoint does not support updating data")
            }
        }

        endpoint.updateDataForWebSocket(this, session, call)
    }
    suspend inline fun handleGet(request: Request, endpoint: BlaseballEndpoint?, path: String) {
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
                launch(coroutineContext) {
                    streamData
                        .await()
                        .collect { data ->
                            try {
                                withContext(Dispatchers.IO) {
                                    write("data:")
                                    write(data)
                                    write("\n\n")
                                    flush()
                                }
                            } catch (th: Throwable) {
                                this@respondTextWriter.close()
                                cancel("An error occurred while writing", th)
                            }
                        }
                }.join()
            }
        }

    suspend fun handleEventsStreamDataWebsocket(session: WebSocketServerSession) =
        with(session) {
            launch(coroutineContext) {
                streamData
                    .await()
                    .collect { send(it) }
            }.join()
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
            put(LEAGUE_ID, leagueID)
            put(VISIBILITY, visibility.name)

            put(CLOCK, clock.describe() ?: JsonNull)

            put(API_GET_USER, apiGetUser?.describe() ?: JsonNull)
            put(API_GET_USER_REWARDS, apiGetUserRewards?.describe() ?: JsonNull)
            put(API_GET_ACTIVE_BETS, apiGetActiveBets?.describe() ?: JsonNull)

            put(API_GET_IDOLS, apiGetIdols?.describe() ?: JsonNull)
            put(API_GET_TRIBUTES, apiGetTributes?.describe() ?: JsonNull)

            put(DATABASE_GLOBAL_EVENTS, databaseGlobalEvents?.describe() ?: JsonNull)
            put(DATABASE_SHOP_SETUP, databaseShopSetup?.describe() ?: JsonNull)

            put(DATABASE_GLOBAL_FEED, databaseGlobalFeed?.describe() ?: JsonNull)
            put(DATABASE_GAME_FEED, databaseGameFeed?.describe() ?: JsonNull)
            put(DATABASE_PLAYER_FEED, databasePlayerFeed?.describe() ?: JsonNull)
            put(DATABASE_TEAM_FEED, databaseTeamFeed?.describe() ?: JsonNull)
            put(DATABASE_STORY_FEED, databaseStoryFeed?.describe() ?: JsonNull)
            put(DATABASE_FEED_BY_PHASE, databaseFeedByPhase?.describe() ?: JsonNull)

            put(DATABASE_PLAYER_NAMES_AND_IDS, databasePlayerNames?.describe() ?: JsonNull)
            put(DATABASE_PLAYERS, databasePlayers?.describe() ?: JsonNull)
            put(DATABASE_OFFSEASON_RECAP, databaseOffseasonRecap?.describe() ?: JsonNull)
            put(DATABASE_OFFSEASON_SETUP, databaseOffseasonSetup?.describe() ?: JsonNull)

            put(DATABASE_SUN_SUN, databaseSunSun?.describe() ?: JsonNull)
            put(DATABASE_VAULT, databaseVault?.describe() ?: JsonNull)

            put(DATABASE_ALL_DIVISIONS, databaseAllDivisions?.describe() ?: JsonNull)
            put(DATABASE_ALL_TEAMS, databaseAllTeams?.describe() ?: JsonNull)

            put(DATABASE_COMMUNITY_CHEST_PROGRESS, databaseCommunityChestProgress?.describe() ?: JsonNull)
            put(DATABASE_BONUS_RESULTS, databaseBonusResults?.describe() ?: JsonNull)
            put(DATABASE_DECREE_RESULTS, databaseDecreeResults?.describe() ?: JsonNull)
            put(DATABASE_EVENT_RESULTS, databaseEventResults?.describe() ?: JsonNull)

            put(DATABASE_GAME_BY_ID, databaseGameById?.describe() ?: JsonNull)
            put(DATABASE_GET_PREVIOUS_CHAMP, databaseGetPreviousChamp?.describe() ?: JsonNull)
            put(DATABASE_GIFT_PROGRESS, databaseGiftProgress?.describe() ?: JsonNull)

            put(DATABASE_ITEMS, databaseItems?.describe() ?: JsonNull)
            put(DATABASE_PLAYERS_BY_ITEM_ID, databasePlayersByItemId?.describe() ?: JsonNull)
            put(DATABASE_PLAYOFFS, databasePlayoffs?.describe() ?: JsonNull)
            put(DATABASE_RENOVATION_PROGRESS, databaseRenovationProgress?.describe() ?: JsonNull)
            put(DATABASE_RENOVATIONS, databaseRenovations?.describe() ?: JsonNull)

            put(DATABASE_SUBLEAGUE, databaseSubleague?.describe() ?: JsonNull)
            put(DATABASE_TEAM, databaseTeam?.describe() ?: JsonNull)
            put(DATABASE_TEAM_ELECTION_STATS, databaseTeamElectionStats?.describe() ?: JsonNull)

            put(EVENTS_STREAM_DATA, eventsStreamData?.describe() ?: JsonNull)
        }

    init {
        siteData.launch(this)
    }
}