package dev.brella.blasement.data

import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.BlaseballEventsStreamDataEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetActiveBetsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetIdolsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetRisingStarsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetTributesEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserRewardsEndpoint
import dev.brella.blasement.endpoints.database.*
import dev.brella.blasement.getStringOrNull
import dev.brella.blasement.getValue
import dev.brella.blasement.respondJsonObject
import dev.brella.kornea.errors.common.*
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
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty1
import kotlin.time.ExperimentalTime

class LeagueRegistry(val json: Json, val httpClient: HttpClient, datablaseConfig: JsonObject) : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob()

    private val leagues: MutableMap<String, BlasementLeague> = HashMap()
    private val datablase = BlasementDatabase.asAsync(this, datablaseConfig)
    private val argon2 = Argon2PasswordEncoder()
    private val utc = java.time.Clock.systemUTC()

    public fun registerTemporaryLeague(league: BlasementLeague) =
        leagues.put(league.leagueID, league)

    public inline fun registerTemporaryLeague(block: BlasementLeagueBuilder.() -> Unit) =
        registerTemporaryLeague(buildBlasementLeague(block))

    public inline fun registerTemporaryLeague(
        leagueID: String? = null,
        json: Json? = null,
        http: HttpClient? = null,
        clock: BlasementClock? = null,
        protection: EnumProtectionStatus? = null,
        authentication: String? = null,
        block: BlasementLeagueBuilder.() -> Unit
    ) =
        registerTemporaryLeague(buildBlasementLeague(leagueID, json, http, clock, protection, authentication, block = block))

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
                config.getStringOrNull("league_id")?.let { this.leagueID = it }

                protectionStatus =
                    when (val protectionStatus = config.getStringOrNull("protection")?.lowercase(Locale.getDefault())) {
                        "private" -> EnumProtectionStatus.PRIVATE
                        "protected" -> EnumProtectionStatus.PROTECTED
                        "public" -> EnumProtectionStatus.PUBLIC
                        null -> EnumProtectionStatus.PUBLIC
                        else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown protection status '$protectionStatus'")
                    }

                clock = BlasementClock
                    .loadFrom(config["clock"], createdAt, utc)
                    .consumeOnSuccessGetOrBreak { return it.cast() }

                api {
                    getActiveBets = BlaseballApiGetActiveBetsEndpoint
                        .loadFrom(config["apiGetActiveBets"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    getIdols = BlaseballApiGetIdolsEndpoint
                        .loadFrom(config["apiGetIdols"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    getRisingStars = BlaseballApiGetRisingStarsEndpoint
                        .loadFrom(config["apiGetRisingStars"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }
                    getTribute = BlaseballApiGetTributesEndpoint
                        .loadFrom(config["apiGetTribute"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    getUser = BlaseballApiGetUserEndpoint
                        .loadFrom(config["apiGetUser"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    getUserRewards = BlaseballApiGetUserRewardsEndpoint
                        .loadFrom(config["apiGetUserRewards"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }
                }

                database {
                    feed {
                        global = BlaseballDatabaseFeedEndpoint
                            .loadGlobalFrom(config["databaseFeedGlobal"])
                            .consumeOnSuccessGetOrBreak { return it.cast() }

                        game = BlaseballDatabaseFeedEndpoint
                            .loadGameFrom(config["databaseFeedGame"])
                            .consumeOnSuccessGetOrBreak { return it.cast() }

                        player = BlaseballDatabaseFeedEndpoint
                            .loadPlayerFrom(config["databaseFeedPlayer"])
                            .consumeOnSuccessGetOrBreak { return it.cast() }

                        team = BlaseballDatabaseFeedEndpoint
                            .loadTeamFrom(config["databaseFeedTeam"])
                            .consumeOnSuccessGetOrBreak { return it.cast() }

                        story = BlaseballDatabaseFeedEndpoint
                            .loadStoryFrom(config["databaseFeedStory"])
                            .consumeOnSuccessGetOrBreak { return it.cast() }
                    }

                    allDivisions = BlaseballDatabaseAllDivisionsEndpoint
                        .loadFrom(config["databaseAllDivisions"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    allTeams = BlaseballDatabaseAllTeamsEndpoint
                        .loadFrom(config["databaseAllTeams"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    communityChestProgress = BlaseballDatabaseCommunityChestProgressEndpoint
                        .loadFrom(config["databaseCommunityChestProgress"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    bonusResults = BlaseballDatabaseBonusResultsEndpoint
                        .loadFrom(config["databaseBonusResults"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    decreeResults = BlaseballDatabaseDecreeResultsEndpoint
                        .loadFrom(config["databaseDecreeResults"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    eventResults = BlaseballDatabaseEventResultsEndpoint
                        .loadFrom(config["databaseEventResults"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    feedByPhase = BlaseballDatabaseFeedByPhaseEndpoint
                        .loadFrom(config["databaseFeedByPhase"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    gameById = BlaseballDatabaseGameByIdEndpoint
                        .loadFrom(config["databaseGameById"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    getPreviousChamp = BlaseballDatabaseGetPreviousChampEndpoint
                        .loadFrom(config["databaseGetPreviousChamp"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    giftProgress = BlaseballDatabaseGiftProgressEndpoint
                        .loadFrom(config["databaseGiftProgress"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    globalEvents = BlaseballDatabaseGlobalEventsEndpoint
                        .loadFrom(config["databaseGlobalEvents"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    items = BlaseballDatabaseItemsEndpoint
                        .loadFrom(config["databaseItems"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    offseasonRecap = BlaseballDatabaseOffseasonRecapEndpoint
                        .loadFrom(config["databaseOffseasonRecap"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    offseasonSetup = BlaseballDatabaseOffseasonSetupEndpoint
                        .loadFrom(config["databaseOffseasonSetup"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    playerNamesIds = BlaseballDatabasePlayerNamesEndpoint
                        .loadFrom(config["databasePlayerNamesIds"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    players = BlaseballDatabasePlayersEndpoint
                        .loadFrom(config["databasePlayers"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    playersByItemId = BlaseballDatabasePlayersByItemEndpoint
                        .loadFrom(config["databasePlayersByItemId"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    playoffs = BlaseballDatabasePlayoffsEndpoint
                        .loadFrom(config["databasePlayoffs"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    renovationProgress = BlaseballDatabaseRenovationProgressEndpoint
                        .loadFrom(config["databaseRenovationProgress"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    renovations = BlaseballDatabaseRenovationsEndpoint
                        .loadFrom(config["databaseRenovations"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    shopSetup = BlaseballDatabaseShopSetupEndpoint
                        .loadFrom(config["databaseShopSetup"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    subleague = BlaseballDatabaseSubleagueEndpoint
                        .loadFrom(config["databaseSubleague"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    sunSun = BlaseballDatabaseSunSunEndpoint
                        .loadFrom(config["databaseSunSun"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    team = BlaseballDatabaseTeamEndpoint
                        .loadFrom(config["databaseTeam"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    teamElectionStats = BlaseballDatabaseTeamElectionStatsEndpoint
                        .loadFrom(config["databaseTeamElectionStats"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }

                    vault = BlaseballDatabaseVaultEndpoint
                        .loadFrom(config["databaseVault"])
                        .consumeOnSuccessGetOrBreak { return it.cast() }
                }

                eventsStreamData = BlaseballEventsStreamDataEndpoint
                    .loadFrom(config["eventsStreamData"])
                    .consumeOnSuccessGetOrBreak { return it.cast() }
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
    private inline fun Route.getLeague(path: String, endpoint: KProperty1<BlasementLeague, BlaseballEndpoint?>): Route =
        get(path) {
            league()?.let { it.handle(this, endpoint.get(it), path) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
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

        if (league.protection == EnumProtectionStatus.PRIVATE)
            if (authToken == null || !argon2.matches(authToken, league.authentication)) return null

        return league
    }

    private inline fun WebSocketServerSession.league(): BlasementLeague? {
        val authToken = call.request.header(HttpHeaders.Authorization)

        val league = leagues[call.parameters.getOrFail("league_id")] ?: return null

        if (league.protection == EnumProtectionStatus.PRIVATE)
            if (authToken == null || !argon2.matches(authToken, league.authentication)) return null

        return league
    }

    public fun setupRouting(root: Route) {
        root.route("/leagues/{league_id}") {
            getWithLeagueBody("/") { league ->
                println("Endpoint: ${call.request.uri.trim().substringAfter("/leagues/underground")}")
                league.handleIndexHtml(this)
            }

            getWithLeagueBody("/{...}") { league ->
                println("Endpoint: ${call.request.uri.trim().substringAfter("/leagues/underground")}")
                league.handleIndexHtml(this)
            }

//            getWithLeagueBody("/player/{...?}") { league ->
//                println("Endpoint: ${call.request.uri.trim().substringAfter("/leagues/underground")}")
//                league.handleIndexHtml(this)
//            }

            getLeague("/api/getUser", BlasementLeague::apiGetUser)
            getLeague("/api/getUserRewards", BlasementLeague::apiGetUserRewards)
            getLeague("/api/getActiveBets", BlasementLeague::apiGetActiveBets)
            getLeague("/api/getIdols", BlasementLeague::apiGetIdols)
            getLeague("/api/getTribute", BlasementLeague::apiGetTributes)

            getLeague("/database/feed/global", BlasementLeague::databaseGlobalFeed)
            getLeague("/database/feed/game", BlasementLeague::databaseGameFeed)
            getLeague("/database/feed/team", BlasementLeague::databaseTeamFeed)
            getLeague("/database/feed/player", BlasementLeague::databasePlayerFeed)
            getLeague("/database/feed/story", BlasementLeague::databaseStoryFeed)
            getLeague("/database/feedByPhase", BlasementLeague::databaseFeedByPhase)

            getLeague("/database/globalEvents", BlasementLeague::databaseGlobalEvents)
            getLeague("/database/shopSetup", BlasementLeague::databaseShopSetup)
            getLeague("/database/playerNamesIds", BlasementLeague::databasePlayerNames)
            getLeague("/database/players", BlasementLeague::databasePlayers)
            getLeague("/database/offseasonSetup", BlasementLeague::databaseOffseasonSetup)
            getLeague("/database/vault", BlasementLeague::databaseVault)
            getLeague("/database/sunsun", BlasementLeague::databaseSunSun)

            getLeague("/database/allDivisions", BlasementLeague::databaseAllDivisions)
            getLeague("/database/allTeams", BlasementLeague::databaseAllTeams)
            getLeague("/database/communityChestProgress", BlasementLeague::databaseCommunityChestProgress)
            getLeague("/database/bonusResults", BlasementLeague::databaseBonusResults)
            getLeague("/database/decreeResults", BlasementLeague::databaseDecreeResults)
            getLeague("/database/eventResults", BlasementLeague::databaseEventResults)

            getLeague("/database/gameById/{id}", BlasementLeague::databaseGameById)
            getLeague("/database/getPreviousChamp", BlasementLeague::databaseGetPreviousChamp)
            getLeague("/database/giftProgress", BlasementLeague::databaseGiftProgress)

            getLeague("/database/items", BlasementLeague::databaseItems)
            getLeague("/database/playersByItemId", BlasementLeague::databasePlayersByItemId)
            getLeague("/database/playoffs", BlasementLeague::databasePlayoffs)
            getLeague("/database/renovationProgress", BlasementLeague::databaseRenovationProgress)
            getLeague("/database/renovations", BlasementLeague::databaseRenovations)

            getLeague("/database/subleague", BlasementLeague::databaseSubleague)
            getLeague("/database/team", BlasementLeague::databaseTeam)
            getLeague("/database/teamElectionStats", BlasementLeague::databaseTeamElectionStats)

            getLeague("/events/streamData", BlasementLeague::handleEventsStreamData)

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
                            .filter { league -> league.protection == EnumProtectionStatus.PUBLIC }
                            .forEach { league -> put(league.leagueID, league.describe()) }
                    }
                }

                get("/protected") {
                    val authToken = call.request.header(HttpHeaders.Authorization)
                                    ?: return@get call.respond(HttpStatusCode.BadRequest, "No Authorization header")

                    call.respondJsonObject {
                        leagues.values
                            .filter { league ->
                                league.protection == EnumProtectionStatus.PROTECTED
                                && argon2.matches(authToken, league.authentication)
                            }.forEach { league -> put(league.leagueID, league.describe()) }
                    }
                }
                get("/private") {
                    val authToken = call.request.header(HttpHeaders.Authorization)
                                    ?: return@get call.respond(HttpStatusCode.BadRequest, "No Authorization header")

                    call.respondJsonObject {
                        leagues.values
                            .filter { league ->
                                league.protection == EnumProtectionStatus.PRIVATE
                                && argon2.matches(authToken, league.authentication)
                            }.forEach { league -> put(league.leagueID, league.describe()) }
                    }
                }
                get("/viewable") {
                    val authToken = call.request.header(HttpHeaders.Authorization)

                    call.respondJsonObject {
                        leagues.values
                            .filter { league ->
                                league.protection == EnumProtectionStatus.PUBLIC
                                || (authToken != null && argon2.matches(authToken, league.authentication))
                            }.forEach { league -> put(league.leagueID, league.describe()) }
                    }
                }

                post("/new") {
                    registerLeague(
                        call.receive(),
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

                    if (league.protection == EnumProtectionStatus.PRIVATE) {
                        if (authToken == null || !argon2.matches(authToken, league.authentication))
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

                    val matches = argon2.matches(authToken, league.authentication)
                    if (league.protection == EnumProtectionStatus.PRIVATE) {
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

                post("/{league_id}/new") {
                    registerLeague(
                        call.receive(),
                        argon2.encode(
                            call.request.header(HttpHeaders.Authorization)
                            ?: return@post call.respond(HttpStatusCode.BadRequest, "No Authorization header")
                        ),
                        leagueID = call.parameters["league_id"]
                    )
                        .doOnSuccess { call.respond(HttpStatusCode.Created, EmptyContent) }
                        .doOnFailure { call.respond(HttpStatusCode.BadRequest, it.toString()) }
                }
            }
        }
    }
}