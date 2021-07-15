package dev.brella.blasement.data

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
            leagues[call.parameters.getOrFail("id")]
                ?.let { body(it, this) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.webSocketLeague(path: String, crossinline body: suspend BlasementLeague.(WebSocketServerSession) -> Unit): Unit =
        webSocket(path) {
            leagues[call.parameters.getOrFail("id")]
                ?.let { body(it, this) }
            ?: return@webSocket call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.getWithLeagueBody(crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(league: BlasementLeague) -> Unit): Route =
        get {
            leagues[call.parameters.getOrFail("id")]
                ?.let { body(this, it) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    @ContextDsl
    private inline fun Route.getWithLeagueBody(path: String, crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(league: BlasementLeague) -> Unit): Route =
        get(path) {
            leagues[call.parameters.getOrFail("id")]
                ?.let { body(this, it) }
            ?: return@get call.respondJsonObject(HttpStatusCode.NotFound) {
                put("error", "No league found")
            }
        }

    public fun setupRouting(root: Route) {
        root.route("/leagues/{id}") {
            getWithLeagueBody { league ->
                println("Endpoint: ${call.request.uri.trim().substringAfter("/leagues/underground")}")
                league.handleIndexHtml(this)
            }

            getWithLeagueBody("/{...?}") { league ->
                println("Endpoint: ${call.request.uri.trim().substringAfter("/leagues/underground")}")
                league.handleIndexHtml(this)
            }

            getLeague("/api/getUser", BlasementLeague::handleApiGetUser)
            getLeague("/api/getUserRewards", BlasementLeague::handleApiGetUserRewards)
            getLeague("/api/getActiveBets", BlasementLeague::handleApiGetActiveBets)
            getLeague("/api/getIdols", BlasementLeague::handleApiGetIdols)
            getLeague("/api/getTribute", BlasementLeague::handleApiGetTributes)

            getLeague("/database/feed/global", BlasementLeague::handleDatabaseFeedGlobal)
            getLeague("/database/feed/game", BlasementLeague::handleDatabaseFeedGame)
            getLeague("/database/feed/team", BlasementLeague::handleDatabaseFeedTeam)
            getLeague("/database/feed/player", BlasementLeague::handleDatabaseFeedPlayer)
            getLeague("/database/feed/story", BlasementLeague::handleDatabaseFeedStory)

            getLeague("/database/globalEvents", BlasementLeague::handleDatabaseGlobalEvents)
            getLeague("/database/shopSetup", BlasementLeague::handleDatabaseShopSetup)
            getLeague("/database/playerNamesIds", BlasementLeague::handleDatabasePlayerNamesIds)
            getLeague("/database/players", BlasementLeague::handleDatabasePlayers)
            getLeague("/database/offseasonSetup", BlasementLeague::handleDatabaseOffseasonSetup)
            getLeague("/database/vault", BlasementLeague::handleDatabaseVault)
            getLeague("/database/sunsun", BlasementLeague::handleDatabaseSunSun)

            getLeague("/events/streamData", BlasementLeague::handleEventsStreamData)

            webSocketLeague("/api/time", BlasementLeague::handleApiTime)
//
            get("/database/{...}") {
                call.respondRedirect("https://api.sibr.dev/corsmechanics/www.blaseball.com/database/${call.request.uri.substringAfter("/database/")}", false)
            }

            get("/api/{...}") {
                call.respondRedirect("https://api.sibr.dev/corsmechanics/www.blaseball.com/api/${call.request.uri.substringAfter("/api/")}", false)
            }

            getLeague("/main.js", BlasementLeague::handleMainJs)
            getLeague("/main.css", BlasementLeague::handleMainCss)
            getLeague("/2.js", BlasementLeague::handle2Js)
        }
    }
}