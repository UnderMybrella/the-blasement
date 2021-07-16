package dev.brella.blasement.data

import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.respondJsonObject
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.reflect.KProperty1

class LeagueRegistry {
    private val leagues: MutableMap<String, BlasementLeague> = HashMap()

    public fun registerLeague(league: BlasementLeague) =
        leagues.put(league.leagueID, league)

    public inline fun registerLeague(block: BlasementLeagueBuilder.() -> Unit) =
        registerLeague(buildBlasementLeague(block))

    public inline fun registerLeague(
        leagueID: String? = null,
        json: Json? = null,
        http: HttpClient? = null,
        clock: BlasementClock? = null,
        block: BlasementLeagueBuilder.() -> Unit
    ) =
        registerLeague(buildBlasementLeague(leagueID, json, http, clock, block))

    @ContextDsl
    private inline fun Route.getLeague(path: String, crossinline body: suspend BlasementLeague.(PipelineContext<Unit, ApplicationCall>) -> Unit): Route =
        get(path) {
            leagues[call.parameters.getOrFail("league_id")]
                ?.let { body(it, this) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.getLeague(path: String, endpoint: KProperty1<BlasementLeague, BlaseballEndpoint?>): Route =
        get(path) {
            leagues[call.parameters.getOrFail("league_id")]
                ?.let { it.handle(this, endpoint.get(it), path) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.webSocketLeague(path: String, crossinline body: suspend BlasementLeague.(WebSocketServerSession) -> Unit): Unit =
        webSocket(path) {
            leagues[call.parameters.getOrFail("league_id")]
                ?.let { body(it, this) }
            ?: return@webSocket call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.getWithLeagueBody(crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(league: BlasementLeague) -> Unit): Route =
        get {
            leagues[call.parameters.getOrFail("league_id")]
                ?.let { body(this, it) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.getWithLeagueBody(path: String, crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(league: BlasementLeague) -> Unit): Route =
        get(path) {
            leagues[call.parameters.getOrFail("league_id")]
                ?.let { body(this, it) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
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
    }
}