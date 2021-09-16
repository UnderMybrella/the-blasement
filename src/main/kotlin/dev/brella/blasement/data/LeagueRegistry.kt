package dev.brella.blasement.data

import dev.brella.blasement.data.BlasementLeague.Companion.API_DEFAULTS
import dev.brella.blasement.data.BlasementLeague.Companion.API_GET_ACTIVE_BETS
import dev.brella.blasement.data.BlasementLeague.Companion.API_GET_IDOLS
import dev.brella.blasement.data.BlasementLeague.Companion.API_GET_RISING_STARS
import dev.brella.blasement.data.BlasementLeague.Companion.API_GET_TRIBUTES
import dev.brella.blasement.data.BlasementLeague.Companion.API_GET_USER
import dev.brella.blasement.data.BlasementLeague.Companion.API_GET_USER_REWARDS
import dev.brella.blasement.data.BlasementLeague.Companion.CLOCK
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_ALL_DIVISIONS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_ALL_TEAMS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_BONUS_RESULTS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_COMMUNITY_CHEST_PROGRESS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_DECREE_RESULTS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_DEFAULTS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_EVENT_RESULTS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_FEED_BY_PHASE
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_GAME_BY_ID
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_GAME_FEED
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_GET_PREVIOUS_CHAMP
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_GIFT_PROGRESS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_GLOBAL_EVENTS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_GLOBAL_FEED
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_ITEMS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_OFFSEASON_RECAP
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_OFFSEASON_SETUP
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_PLAYERS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_PLAYERS_BY_ITEM_ID
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_PLAYER_FEED
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_PLAYER_NAMES_AND_IDS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_PLAYOFFS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_RENOVATIONS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_RENOVATION_PROGRESS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_SHOP_SETUP
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_STORY_FEED
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_SUBLEAGUE
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_SUN_SUN
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_TEAM
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_TEAM_ELECTION_STATS
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_TEAM_FEED
import dev.brella.blasement.data.BlasementLeague.Companion.DATABASE_VAULT
import dev.brella.blasement.data.BlasementLeague.Companion.DEFAULTS
import dev.brella.blasement.data.BlasementLeague.Companion.EVENTS_STREAM_DATA
import dev.brella.blasement.data.BlasementLeague.Companion.FEED_DEFAULTS
import dev.brella.blasement.data.BlasementLeague.Companion.LEAGUE_ID
import dev.brella.blasement.data.BlasementLeague.Companion.VISIBILITY
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.BlaseballEventsStreamDataEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetActiveBetsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetIdolsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetRisingStarsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetTributesEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserRewardsEndpoint
import dev.brella.blasement.endpoints.database.*
import dev.brella.blasement.getJsonObjectOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.blasement.getValue
import dev.brella.blasement.mergeJsonConfigs
import dev.brella.blasement.respondJsonObject
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.KorneaResult.Companion.errorAsIllegalArgument
import dev.brella.kornea.errors.common.cast
import dev.brella.kornea.errors.common.consumeOnSuccessGetOrBreak
import dev.brella.kornea.errors.common.doOnFailure
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.flatMapOrSelf
import dev.brella.kornea.errors.common.successPooled
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty1
import kotlin.time.ExperimentalTime

class LeagueRegistry(val json: Json, val httpClient: HttpClient, datablaseConfig: JsonObject) : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob()

    private val leagues: MutableMap<String, BlasementLeague> = HashMap()
    private val datablase = BlasementDatabase.asAsync(this, datablaseConfig)
    private val argon2 = Argon2PasswordEncoder()
    private val utc = java.time.Clock.systemUTC()

    private val ADMIN_AUTH = (System.getProperty("blasement.admin")
                             ?: System.getenv("BLASEMENT_ADMIN"))
        ?.let { str ->
            if (!str.startsWith("argon2:")) {
                val encoded = argon2.encode(str)
                println("WARN: admin auth passed in cleartext; use '$encoded'!")

                encoded
            } else {
                str
            }
        }

    private inline fun matchesAuth(authToken: String, league: BlasementLeague): Boolean =
        argon2.matches(authToken, league.authentication) || (ADMIN_AUTH != null && argon2.matches(authToken, ADMIN_AUTH))

    public fun registerTemporaryLeague(league: BlasementLeague) =
        leagues.put(league.leagueID, league)

    public inline fun registerTemporaryLeague(block: BlasementLeagueBuilder.() -> Unit) =
        registerTemporaryLeague(buildBlasementLeague(block))

    public inline fun registerTemporaryLeague(
        leagueID: String? = null,
        json: Json? = null,
        http: HttpClient? = null,
        clock: BlasementClock? = null,
        siteDataClock: BlasementClock? = clock,
        visibility: EnumVisibilityStatus? = null,
        authentication: String? = null,
        block: BlasementLeagueBuilder.() -> Unit
    ) =
        registerTemporaryLeague(buildBlasementLeague(leagueID, json, http, clock, siteDataClock, visibility, authentication, block = block))

    public suspend fun registerLeague(config: JsonObject, authentication: String, createdAt: Long = utc.millis(), leagueID: String? = null): KorneaResult<BlasementLeague> =
        parseLeagueFromConfig(config, authentication, createdAt = createdAt, leagueID = leagueID)
            .flatMapOrSelf { league ->
                if (datablase.await()
                        .client
                        .sql("INSERT INTO blasement_instances (instance_id, authentication, config, created) VALUES ($1, $2, $3, $4) ON CONFLICT DO NOTHING")
                        .bind("$1", league.leagueID)
                        .bind("$2", league.authentication)
                        .bind("$3", io.r2dbc.postgresql.codec.Json.of(config.toString()))
                        .bind("$4", createdAt)
                        .fetch()
                        .awaitRowsUpdated() == 1
                ) {
                    leagues[league.leagueID] = league

                    return@flatMapOrSelf null
                } else {
                    KorneaResult.errorAsIllegalArgument(-1, "Failed to register league - already exists with ID ${league.leagueID}")
                }
            }
//
//    public suspend inline fun registerLeague(block: BlasementLeagueBuilder.() -> Unit) =
//        registerLeague(buildBlasementLeague(block))
//
//    public suspend inline fun registerLeague(
//        leagueID: String? = null,
//        json: Json? = null,
//        http: HttpClient? = null,
//        clock: BlasementClock? = null,
//        protection: EnumProtectionStatus? = null,
//        authentication: String? = null,
//        block: BlasementLeagueBuilder.() -> Unit
//    ) =
//        registerLeague(buildBlasementLeague(leagueID, json, http, clock, protection, authentication, block = block))

    @OptIn(ExperimentalTime::class)
    fun parseLeagueFromConfig(config: JsonObject, authentication: String, createdAt: Long = utc.millis(), leagueID: String? = null): KorneaResult<BlasementLeague> =
        KorneaResult.successPooled(
            buildBlasementLeague(leagueID, json, httpClient, authentication = authentication) {
                config.getStringOrNull(LEAGUE_ID)?.let { this.leagueID = it }

                visibilityStatus =
                    when (val visibilityStatus = config.getStringOrNull(VISIBILITY)?.lowercase(Locale.getDefault())) {
                        "private" -> EnumVisibilityStatus.PRIVATE
                        "protected" -> EnumVisibilityStatus.PROTECTED
                        "public" -> EnumVisibilityStatus.PUBLIC
                        null -> EnumVisibilityStatus.PUBLIC
                        else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown visibility status '$visibilityStatus'")
                    }

                clock = BlasementClock
                    .loadFrom(config[CLOCK], createdAt, utc)
                    .consumeOnSuccessGetOrBreak { return it.cast() }

                siteDataClock = config["siteDataClock"]
                    ?.let { json ->
                        BlasementClock
                            .loadFrom(json, createdAt, utc)
                            .consumeOnSuccessGetOrBreak { return it.cast() }
                    }

                config.getStringOrNull("liveBaseUrl")
                    ?.let { this.liveBaseUrl = it }
                config.getStringOrNull("chroniclerBaseUrl")
                    ?.let { this.chroniclerBaseUrl = it }
                config.getStringOrNull("upnutsBaseUrl")
                    ?.let { this.upnutsBaseUrl = it }

                val defaults = config[DEFAULTS]
                val apiDefaults = config[API_DEFAULTS]
                val databaseDefaults = config[DATABASE_DEFAULTS]
                val databaseFeedDefaults = config[FEED_DEFAULTS]

                api {
                    getActiveBets = BlaseballApiGetActiveBetsEndpoint
                        .loadFrom(mergeJsonConfigs(config[API_GET_ACTIVE_BETS], apiDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1000, "Failed to load config for $API_GET_ACTIVE_BETS", it) }

                    getIdols = BlaseballApiGetIdolsEndpoint
                        .loadFrom(mergeJsonConfigs(config[API_GET_IDOLS], apiDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1001, "Failed to load config for $API_GET_IDOLS", it) }

                    getRisingStars = BlaseballApiGetRisingStarsEndpoint
                        .loadFrom(mergeJsonConfigs(config[API_GET_RISING_STARS], apiDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1002, "Failed to load config for $API_GET_RISING_STARS", it) }

                    getTribute = BlaseballApiGetTributesEndpoint
                        .loadFrom(mergeJsonConfigs(config[API_GET_TRIBUTES], apiDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1003, "Failed to load config for $API_GET_TRIBUTES", it) }

                    getUser = BlaseballApiGetUserEndpoint
                        .loadFrom(mergeJsonConfigs(config[API_GET_USER], apiDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1004, "Failed to load config for $API_GET_USER", it) }

                    getUserRewards = BlaseballApiGetUserRewardsEndpoint
                        .loadFrom(mergeJsonConfigs(config[API_GET_USER_REWARDS], apiDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1005, "Failed to load config for $API_GET_USER_REWARDS", it) }
                }

                database {
                    feed {
                        global = BlaseballDatabaseFeedEndpoint
                            .loadGlobalFrom(mergeJsonConfigs(config[DATABASE_GLOBAL_FEED], databaseFeedDefaults, databaseDefaults, defaults))
                            .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1100, "Failed to load config for $DATABASE_GLOBAL_FEED", it) }

                        game = BlaseballDatabaseFeedEndpoint
                            .loadGameFrom(mergeJsonConfigs(config[DATABASE_GAME_FEED], databaseFeedDefaults, databaseDefaults, defaults))
                            .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1101, "Failed to load config for $DATABASE_GAME_FEED", it) }

                        player = BlaseballDatabaseFeedEndpoint
                            .loadPlayerFrom(mergeJsonConfigs(config[DATABASE_PLAYER_FEED], databaseFeedDefaults, databaseDefaults, defaults))
                            .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1102, "Failed to load config for $DATABASE_PLAYER_FEED", it) }

                        team = BlaseballDatabaseFeedEndpoint
                            .loadTeamFrom(mergeJsonConfigs(config[DATABASE_TEAM_FEED], databaseFeedDefaults, databaseDefaults, defaults))
                            .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1103, "Failed to load config for $DATABASE_TEAM_FEED", it) }

                        story = BlaseballDatabaseFeedEndpoint
                            .loadStoryFrom(mergeJsonConfigs(config[DATABASE_STORY_FEED], databaseFeedDefaults, databaseDefaults, defaults))
                            .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1104, "Failed to load config for $DATABASE_STORY_FEED", it) }
                    }

                    allDivisions = BlaseballDatabaseAllDivisionsEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_ALL_DIVISIONS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1200, "Failed to load config for $DATABASE_ALL_DIVISIONS", it) }

                    allTeams = BlaseballDatabaseAllTeamsEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_ALL_TEAMS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1201, "Failed to load config for $DATABASE_ALL_TEAMS", it) }

                    communityChestProgress = BlaseballDatabaseCommunityChestProgressEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_COMMUNITY_CHEST_PROGRESS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1202, "Failed to load config for $DATABASE_COMMUNITY_CHEST_PROGRESS", it) }

                    bonusResults = BlaseballDatabaseBonusResultsEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_BONUS_RESULTS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1203, "Failed to load config for $DATABASE_BONUS_RESULTS", it) }

                    decreeResults = BlaseballDatabaseDecreeResultsEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_DECREE_RESULTS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1204, "Failed to load config for $DATABASE_DECREE_RESULTS", it) }

                    eventResults = BlaseballDatabaseEventResultsEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_EVENT_RESULTS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1205, "Failed to load config for $DATABASE_EVENT_RESULTS", it) }

                    feedByPhase = BlaseballDatabaseFeedByPhaseEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_FEED_BY_PHASE], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1206, "Failed to load config for $DATABASE_FEED_BY_PHASE", it) }

                    gameById = BlaseballDatabaseGameByIdEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_GAME_BY_ID], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1207, "Failed to load config for $DATABASE_GAME_BY_ID", it) }

                    getPreviousChamp = BlaseballDatabaseGetPreviousChampEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_GET_PREVIOUS_CHAMP], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1208, "Failed to load config for $DATABASE_GET_PREVIOUS_CHAMP", it) }

                    giftProgress = BlaseballDatabaseGiftProgressEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_GIFT_PROGRESS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1209, "Failed to load config for $DATABASE_GIFT_PROGRESS", it) }

                    globalEvents = BlaseballDatabaseGlobalEventsEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_GLOBAL_EVENTS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x120A, "Failed to load config for $DATABASE_GLOBAL_EVENTS", it) }

                    items = BlaseballDatabaseItemsEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_ITEMS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x120B, "Failed to load config for $DATABASE_ITEMS", it) }

                    offseasonRecap = BlaseballDatabaseOffseasonRecapEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_OFFSEASON_RECAP], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x120C, "Failed to load config for $DATABASE_OFFSEASON_RECAP", it) }

                    offseasonSetup = BlaseballDatabaseOffseasonSetupEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_OFFSEASON_SETUP], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x120D, "Failed to load config for $DATABASE_OFFSEASON_SETUP", it) }

                    playerNamesIds = BlaseballDatabasePlayerNamesEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_PLAYER_NAMES_AND_IDS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x120E, "Failed to load config for $DATABASE_PLAYER_NAMES_AND_IDS", it) }

                    players = BlaseballDatabasePlayersEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_PLAYERS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x120F, "Failed to load config for $DATABASE_PLAYERS", it) }

                    playersByItemId = BlaseballDatabasePlayersByItemEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_PLAYERS_BY_ITEM_ID], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1210, "Failed to load config for $DATABASE_PLAYERS_BY_ITEM_ID", it) }

                    playoffs = BlaseballDatabasePlayoffsEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_PLAYOFFS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1211, "Failed to load config for $DATABASE_PLAYOFFS", it) }

                    renovationProgress = BlaseballDatabaseRenovationProgressEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_RENOVATION_PROGRESS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1212, "Failed to load config for $DATABASE_RENOVATION_PROGRESS", it) }

                    renovations = BlaseballDatabaseRenovationsEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_RENOVATIONS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1213, "Failed to load config for $DATABASE_RENOVATIONS", it) }

                    shopSetup = BlaseballDatabaseShopSetupEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_SHOP_SETUP], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1214, "Failed to load config for $DATABASE_SHOP_SETUP", it) }

                    subleague = BlaseballDatabaseSubleagueEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_SUBLEAGUE], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1215, "Failed to load config for $DATABASE_SUBLEAGUE", it) }

                    sunSun = BlaseballDatabaseSunSunEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_SUN_SUN], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1216, "Failed to load config for $DATABASE_SUN_SUN", it) }

                    team = BlaseballDatabaseTeamEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_TEAM], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1217, "Failed to load config for $DATABASE_TEAM", it) }

                    teamElectionStats = BlaseballDatabaseTeamElectionStatsEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_TEAM_ELECTION_STATS], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1218, "Failed to load config for $DATABASE_TEAM_ELECTION_STATS", it) }

                    vault = BlaseballDatabaseVaultEndpoint
                        .loadFrom(mergeJsonConfigs(config[DATABASE_VAULT], databaseDefaults, defaults))
                        .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1219, "Failed to load config for $DATABASE_VAULT", it) }
                }

                eventsStreamData = BlaseballEventsStreamDataEndpoint
                    .loadFrom(mergeJsonConfigs(config[EVENTS_STREAM_DATA], defaults))
                    .consumeOnSuccessGetOrBreak { return errorAsIllegalArgument(0x1300, "Failed to load config for $EVENTS_STREAM_DATA", it) }
            }
        )

    val initJob = launch {
        try {
            datablase.await()
                .client
                .sql("SELECT instance_id, authentication, config, created FROM blasement_instances")
                .map { row ->
                    Pair(
                        parseLeagueFromConfig(
                            json.decodeFromString(row.getValue("config")),
                            row.getValue("authentication"),
                            createdAt = row.getValue("created"),
                            leagueID = row.getValue("instance_id")
                        ),
                        row.getValue<String>("instance_id")
                    )
                }
                .all()
                .asFlow()
                .onEach { (leagueResult, leagueID) ->
                    leagueResult
                        .doOnFailure { println("ERR: $leagueID failed to load from database - ${leagueResult}") }
                        .doOnSuccess(::registerTemporaryLeague)
                }
                .launchIn(this)
                .join()
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    @ContextDsl
    private inline fun Route.getLeague(path: String, crossinline body: suspend BlasementLeague.(PipelineContext<Unit, ApplicationCall>) -> Unit): Route =
        get(path) {
            league()?.let { body(it, this) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.leagueEndpoint(path: String, endpoint: KProperty1<BlasementLeague, BlaseballEndpoint?>): Route =
        route(path) {
            get {
                league()?.let { it.handleGet(this, endpoint.get(it), path) }
                ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                    put("error", "No league found")
                }
            }

            put {
                league()?.let { league ->
                    val authToken = call.request.header(HttpHeaders.Authorization)
                    if (authToken == null || !matchesAuth(authToken, league)) {
                        return@put call.respondJsonObject(HttpStatusCode.Unauthorized) {
                            put("error", "Invalid authentication token")
                        }
                    }

                    league.handlePut(this, endpoint.get(league), path)
                } ?: return@put call.respondJsonObject(HttpStatusCode.NotFound) {
                    put("error", "No league found")
                }
            }

            webSocket("/update") {
                league()?.let { league ->
                    val authToken = call.request.header(HttpHeaders.Authorization)
                    if (authToken == null || !matchesAuth(authToken, league)) {
                        return@webSocket call.respondJsonObject(HttpStatusCode.Unauthorized) {
                            put("error", "Invalid authentication token")
                        }
                    }

                    league.handleWebSocket(this, call, endpoint.get(league), path)
                } ?: return@webSocket call.respondJsonObject(HttpStatusCode.NotFound) {
                    put("error", "No league found")
                }
            }
        }

    @ContextDsl
    private inline fun Route.getLeague(path: String, endpoint: KProperty1<BlasementLeague, BlaseballEndpoint?>): Route =
        get(path) {
            league()?.let { it.handleGet(this, endpoint.get(it), path) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.putLeague(path: String, endpoint: KProperty1<BlasementLeague, BlaseballEndpoint?>): Route =
        put(path) {
            league()?.let { league ->
                val authToken = call.request.header(HttpHeaders.Authorization)
                if (authToken == null || !matchesAuth(authToken, league)) {
                    return@put call.respondJsonObject(HttpStatusCode.Unauthorized) {
                        put("error", "Invalid authentication token")
                    }
                }

                league.handlePut(this, endpoint.get(league), path)
            } ?: return@put call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.webSocketLeague(path: String, crossinline body: suspend BlasementLeague.(WebSocketServerSession) -> Unit): Unit =
        webSocket(path) {
            league()?.let { body(it, this) }
            ?: return@webSocket call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.getWithLeagueBody(crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(league: BlasementLeague) -> Unit): Route =
        get {
            league()?.let { body(this, it) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.getWithLeagueBody(path: String, crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(league: BlasementLeague) -> Unit): Route =
        get(path) {
            league()?.let { body(this, it) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    private inline fun PipelineContext<Unit, ApplicationCall>.league(): BlasementLeague? {
        val authToken = call.request.header(HttpHeaders.Authorization)

        val league = leagues[call.parameters.getOrFail("league_id")] ?: return null

        if (league.visibility == EnumVisibilityStatus.PRIVATE)
            if (authToken == null || !matchesAuth(authToken, league)) return null

        return league
    }

    private inline fun WebSocketServerSession.league(): BlasementLeague? {
        val authToken = call.request.header(HttpHeaders.Authorization)

        val league = leagues[call.parameters.getOrFail("league_id")] ?: return null

        if (league.visibility == EnumVisibilityStatus.PRIVATE)
            if (authToken == null || !matchesAuth(authToken, league)) return null

        return league
    }

    public fun setupRouting(root: Route) {
        root.route("/leagues/{league_id}") {
            getWithLeagueBody("/") { league ->
                println("Endpoint: ${call.request.uri.trim().substringAfter("/leagues/${call.parameters["league_id"]}")}")
                league.handleIndexHtml(this)
            }

            getWithLeagueBody("/{...}") { league ->
                println("Endpoint: ${call.request.uri.trim().substringAfter("/leagues/${call.parameters["league_id"]}")}")
                league.handleIndexHtml(this)
            }

            leagueEndpoint("/api/getUser", BlasementLeague::apiGetUser)
            leagueEndpoint("/api/getUserRewards", BlasementLeague::apiGetUserRewards)
            leagueEndpoint("/api/getActiveBets", BlasementLeague::apiGetActiveBets)
            leagueEndpoint("/api/getIdols", BlasementLeague::apiGetIdols)
            leagueEndpoint("/api/getTribute", BlasementLeague::apiGetTributes)
            leagueEndpoint("/api/getRisingStars", BlasementLeague::apiGetRisingStars)

            leagueEndpoint("/database/feed/global", BlasementLeague::databaseGlobalFeed)
            leagueEndpoint("/database/feed/game", BlasementLeague::databaseGameFeed)
            leagueEndpoint("/database/feed/team", BlasementLeague::databaseTeamFeed)
            leagueEndpoint("/database/feed/player", BlasementLeague::databasePlayerFeed)
            leagueEndpoint("/database/feed/story", BlasementLeague::databaseStoryFeed)
            leagueEndpoint("/database/feedByPhase", BlasementLeague::databaseFeedByPhase)
            leagueEndpoint("/database/feedbyphase", BlasementLeague::databaseFeedByPhase)

            leagueEndpoint("/database/globalEvents", BlasementLeague::databaseGlobalEvents)
            leagueEndpoint("/database/shopSetup", BlasementLeague::databaseShopSetup)
            leagueEndpoint("/database/playerNamesIds", BlasementLeague::databasePlayerNames)
            leagueEndpoint("/database/players", BlasementLeague::databasePlayers)
            leagueEndpoint("/database/offseasonSetup", BlasementLeague::databaseOffseasonSetup)
            leagueEndpoint("/database/offseasonRecap", BlasementLeague::databaseOffseasonRecap)
            leagueEndpoint("/database/vault", BlasementLeague::databaseVault)
            leagueEndpoint("/database/sunsun", BlasementLeague::databaseSunSun)

            leagueEndpoint("/database/allDivisions", BlasementLeague::databaseAllDivisions)
            leagueEndpoint("/database/allTeams", BlasementLeague::databaseAllTeams)
            leagueEndpoint("/database/communityChestProgress", BlasementLeague::databaseCommunityChestProgress)
            leagueEndpoint("/database/bonusResults", BlasementLeague::databaseBonusResults)
            leagueEndpoint("/database/decreeResults", BlasementLeague::databaseDecreeResults)
            leagueEndpoint("/database/eventResults", BlasementLeague::databaseEventResults)

            leagueEndpoint("/database/gameById/{id}", BlasementLeague::databaseGameById)
            leagueEndpoint("/database/getPreviousChamp", BlasementLeague::databaseGetPreviousChamp)
            leagueEndpoint("/database/giftProgress", BlasementLeague::databaseGiftProgress)

            leagueEndpoint("/database/items", BlasementLeague::databaseItems)
            leagueEndpoint("/database/playersByItemId", BlasementLeague::databasePlayersByItemId)
            leagueEndpoint("/database/playoffs", BlasementLeague::databasePlayoffs)
            leagueEndpoint("/database/renovationProgress", BlasementLeague::databaseRenovationProgress)
            leagueEndpoint("/database/renovations", BlasementLeague::databaseRenovations)

            leagueEndpoint("/database/subleague", BlasementLeague::databaseSubleague)
            leagueEndpoint("/database/team", BlasementLeague::databaseTeam)
            leagueEndpoint("/database/teamElectionStats", BlasementLeague::databaseTeamElectionStats)

            getLeague("/events/streamData", BlasementLeague::handleEventsStreamData)
            putLeague("/events/streamData", BlasementLeague::eventsStreamData)
            webSocketLeague("/events/streamData", BlasementLeague::handleEventsStreamDataWebsocket)

            webSocketLeague("/api/time", BlasementLeague::handleApiTime)
//
            get("/database/{...}") {
                println("Endpoint: /database/${call.request.uri.substringAfter("/database/")}")
                call.respondRedirect("https://api.sibr.dev/corsmechanics/www.blaseball.com/database/${call.request.uri.substringAfter("/database/")}", false)
            }

            get("/api/{...}") {
                println("Endpoint: /api/${call.request.uri.substringAfter("/api/")}")
                call.respondRedirect("https://api.sibr.dev/corsmechanics/www.blaseball.com/api/${call.request.uri.substringAfter("/api/")}", false)
            }

            getLeague("/main.js", BlasementLeague::handleMainJs)
            getLeague("/main.css", BlasementLeague::handleMainCss)
            getLeague("/2.js", BlasementLeague::handle2Js)
        }
        root.route("/api") {
            route("/leagues") {
                get("/public") {
                    call.respondJsonObject {
                        leagues.values
                            .filter { league -> league.visibility == EnumVisibilityStatus.PUBLIC }
                            .forEach { league -> put(league.leagueID, league.describe()) }
                    }
                }
                route("/public") {
                    handle { call.respond(HttpStatusCode.MethodNotAllowed, EmptyContent) }
                }

                get("/protected") {
                    val authToken = call.request.header(HttpHeaders.Authorization)
                                    ?: return@get call.respond(HttpStatusCode.BadRequest, "No Authorization header")

                    call.respondJsonObject {
                        leagues.values
                            .filter { league ->
                                league.visibility == EnumVisibilityStatus.PROTECTED
                                && matchesAuth(authToken, league)
                            }.forEach { league -> put(league.leagueID, league.describe()) }
                    }
                }
                get("/private") {
                    val authToken = call.request.header(HttpHeaders.Authorization)
                                    ?: return@get call.respond(HttpStatusCode.BadRequest, "No Authorization header")

                    call.respondJsonObject {
                        leagues.values
                            .filter { league ->
                                league.visibility == EnumVisibilityStatus.PRIVATE
                                && matchesAuth(authToken, league)
                            }.forEach { league -> put(league.leagueID, league.describe()) }
                    }
                }
                get("/viewable") {
                    val authToken = call.request.header(HttpHeaders.Authorization)

                    call.respondJsonObject {
                        leagues.values
                            .filter { league ->
                                league.visibility == EnumVisibilityStatus.PUBLIC
                                || (authToken != null && matchesAuth(authToken, league))
                            }.forEach { league -> put(league.leagueID, league.describe()) }
                    }
                }

                post<JsonObject>("/new") { config ->
                    registerLeague(
                        config,
                        argon2.encode(
                            call.request.header(HttpHeaders.Authorization)
                            ?: return@post call.respond(HttpStatusCode.BadRequest, "No Authorization header")
                        )
                    )
                        .doOnSuccess { call.respond(HttpStatusCode.Created, EmptyContent) }
                        .doOnFailure { call.respond(HttpStatusCode.BadRequest, it.toString()) }
                }

                get("/{league_id}") {
                    val authToken = call.request.header(HttpHeaders.Authorization)

                    val league = leagues[call.parameters.getOrFail("league_id")]
                                 ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                                     put("error", "League not found")
                                 }

                    if (league.visibility == EnumVisibilityStatus.PRIVATE) {
                        if (authToken == null || !matchesAuth(authToken, league))
                            return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                                put("error", "League not found")
                            }
                    }

                    call.respond(league.describe())
                }
                delete("/{league_id}") {
                    val authToken = call.request.header(HttpHeaders.Authorization)
                                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "No Authorization header")

                    val league = leagues[call.parameters.getOrFail("league_id")]
                                 ?: return@delete call.respondJsonObject(HttpStatusCode.NotFound) {
                                     put("error", "League not found")
                                 }

                    val matches = matchesAuth(authToken, league)
                    if (league.visibility == EnumVisibilityStatus.PRIVATE) {
                        if (!matches) return@delete call.respondJsonObject(HttpStatusCode.NotFound) {
                            put("error", "League not found")
                        }
                    }

                    if (!matches) return@delete call.respondJsonObject(HttpStatusCode.Unauthorized) {
                        put("error", "Invalid auth token")
                    }

                    datablase.await()
                        .client
                        .sql("DELETE FROM blasement_instances WHERE instance_id = $1")
                        .bind("$1", league.leagueID)
                        .await()

                    leagues.remove(league.leagueID)

                    call.respond(HttpStatusCode.OK, league.describe())
                }
                put<JsonObject>("/{league_id}") { config ->
                    registerLeague(
                        config,
                        argon2.encode(
                            call.request.header(HttpHeaders.Authorization)
                            ?: return@put call.respond(HttpStatusCode.BadRequest, "No Authorization header")
                        ),
                        leagueID = call.parameters["league_id"]
                    )
                        .doOnSuccess { call.respond(HttpStatusCode.Created, EmptyContent) }
                        .doOnFailure { call.respond(HttpStatusCode.BadRequest, it.toString()) }
                }
            }
        }

        root.get("/static/{...}") {
            call.respondRedirect("https://d35iw2jmbg6ut8.cloudfront.net${call.request.uri}", false)
        }
    }
}