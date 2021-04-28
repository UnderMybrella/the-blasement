package dev.brella.blasement

import dev.brella.blasement.common.*
import dev.brella.blasement.common.events.FanID
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.FeedID
import dev.brella.kornea.blaseball.base.common.ItemID
import dev.brella.kornea.blaseball.base.common.ModificationID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.errors.common.doOnSuccess
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import redirectInternally
import respond
import respondJsonArray
import respondJsonObject
import set
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class BlasementLeague(val client: HttpClient, val source: BlasementDataSource) {
    val fans: MutableMap<FanID, BlaseballFanPayload> = HashMap()
    val upNuts: MutableMap<FeedID, MutableSet<FanID>> = HashMap()

    fun fanIDFor(key: String): FanID =
        FanID(UUID.nameUUIDFromBytes(key.encodeToByteArray()).toString())

    inline val ApplicationCall.fanID
        get() = fanIDFor(request.origin.remoteHost)

    inline val ApplicationCall.fan
        get() = fans.computeIfAbsent(fanID) { fanID ->
            createFanPayload(fanID, "user@example.com", TeamID("d2634113-b650-47b9-ad95-673f8e28e687"))
        }

    fun routingWebpage(route: Route) =
        with(route) {
            val base = this.toString()
            println("Route: $source @ $base")
            val siteData = BlasementSiteData(client, source, base)
            siteData.launch(GlobalScope)


            handle { siteData.respondIndexHtml(call) }
            get { siteData.respondIndexHtml(call) }
            get("/") { siteData.respondIndexHtml(call) }

            get("/{...}") {
                println("Tailcard url ${call.request.path()}")

                siteData.respondIndexHtml(call)
            }

            get("/main.js") { siteData.respondMainJs(call) }

            get("/2.js") { siteData.respond2Js(call) }

            get("/main.css") { siteData.respondMainCss(call) }
        }

    fun routing(route: Route) =
        with(route) {
            routingWebpage(this)

            route("/api") { api() }
            route("/database") { database() }
            route("/events") { events() }
        }

    fun Route.api() {
        get("/time") {
            call.respondText(BLASEBALL_TIME_PATTERN.format(source.now()))
        }
        get("/time/sse") {
            try {
                call.respondTextWriter(ContentType.Text.EventStream) {
                    while (isActive) {
                        delay(1_000)

                        try {
                            write("data:")
                            write(BLASEBALL_TIME_PATTERN.format(source.now()))
                            write("\n\n")
                            flush()
                        } catch (th: Throwable) {
//                            th.printStackTrace()
                            close()
                        }
                    }
                }
            } catch (th: Throwable) {
//                th.printStackTrace()
            }
        }

        get("/getUser") {
            call.respond(call.fan)

/*            call.respondJsonObject {
                this["id"] = "00000000-0000-0000-0000-000000000000"
                this["email"] = "example@domain.org"
                this["appleId"] = JsonNull
                this["googleId"] = JsonNull
                this["facebookId"] = JsonNull
                this["name"] = JsonNull
                this["password"] = JsonNull
                this["coins"] = 1000
                this["lastActive"] = "1970-01-01T00:00:00Z"
                this["created"] = "1970-01-01T00:00:00Z"
                this["loginStreak"] = 0
                this["favoriteTeam"] = "46358869-dce9-4a01-bfba-ac24fc56f57e"
                this["unlockedShop"] = true
                this["unlockedElection"] = true
                this["peanutsEaten"] = 0
                this["squirrels"] = 0
                this["idol"] = JsonNull
                this["snacks"] = buildJsonObject {
                    this["Max_Bet"] = 1
                    this["Peanuts"] = 1000
                    this["Stadium_Access"] = 1
                }
                this["lightMode"] = false
                this["packSize"] = 3
                this["spread"] = buildJsonArray { }
                this["coffee"] = 0
                this["favNumber"] = 0
                this["snackOrder"] = buildJsonArray {
                    add("Max_Bet")
                    add("Peanuts")
                    add("Stadium_Access")
                }
                this["trackers"] = buildJsonObject {
                    this["BEGS"] = 0
                    this["BETS"] = 0
                    this["VOTES_CAST"] = 0
                    this["SNACKS_BOUGHT"] = 1
                    this["SNACK_UPGRADES"] = 0
                }
            }*/
        }
        get("/getUserRewards") {
            call.respondJsonObject {
                this["coins"] = call.fan.coins
                this["toasts"] = buildJsonArray {}
                this["lightMode"] = false
            }
        }
        get("/getActiveBets") { call.respondJsonArray { } }
        post("/bet") {
            val bet = call.receive<BlaseballBetPayload>()

            println("Betting: $bet")

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        post("/chooseIdol") {
            val idol = call.receive<BlaseballChooseIdolPayload>()

            println("Choosing Idol: $idol")

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        post("/upNut") {
            val payload = call.receive<BlaseballUpNutPayload>()

            val fanID = call.fanID
            val fan = call.fan

            val existingPeanuts = fan.snacks.peanuts

            if (existingPeanuts <= 0) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "You don't have any Peanuts."
            }

            val existing = upNuts.computeIfAbsent(payload.eventId) { HashSet() }
            if (fanID in existing) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "You've already Upshelled that event."
            }

            existing.add(fanID)

            call.respondJsonObject {
                this["message"] = "You Upshelled an event."
            }
        }

        post("/eatADangPeanut") {
            val payload = call.receive<BlaseballEatADangNutPayload>()

            val fanID = call.fanID
            val fan = call.fan

            val existingPeanuts = fan.snacks.peanuts
            val eaten = payload.amount

            if (existingPeanuts < eaten) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "You don't have enough Peanuts."
            } else if (eaten <= 0) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Invalid Peanut count."
            }

            fans[fanID] = fan.copy(peanutsEaten = fan.peanutsEaten + eaten, snacks = fan.snacks.copy(peanuts = existingPeanuts - eaten))

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        post("/updateProfile") {
            val payload = call.receive<BlaseballUpdateProfilePayload>()

            val fanID = call.fanID
            val fan = call.fan

            fans[fanID] = fan.copy(
                favNumber = payload.favNumber,
                coffee = payload.coffee
            )

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        post("/buySnackNoUpgrade") {
            val payload = call.receive<BlaseballBuySnackPayload>()

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        post("/buySnack") {
            val payload = call.receive<BlaseballBuySnackPayload>()

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        post("/sellSnack") {
            val payload = call.receive<BlaseballSellSnackPayload>()

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        post("/buySlot") {

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        post("/sellSlot") {
            val payload = call.receive<BlaseballSellSlotPayload>()

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        redirectInternally("/idol_board", "/getIdols")
        get("/getIdols") {
            source.getIdolBoard().respond(call)
        }

        redirectInternally("/hall_of_flame", "/getTribute")
        get("/getTribute") {
            source.getHallOfFlamePlayers().respond(call)
        }
    }

    fun Route.database() {
        get("/blood") {
            source.getBloodTypes(
                call.parameters.getAll("ids")
                ?: call.parameters["id"]?.let(::listOf)
                ?: emptyList()
            ).respond(call)
        }

        get("/coffee") {
            source.getCoffeePreferences(
                call.parameters.getAll("ids")
                ?: call.parameters["id"]?.let(::listOf)
                ?: emptyList()
            ).respond(call)
        }

        get("/items") {
            source.getItems(
                call.parameters.getAll("ids")?.map(::ItemID)
                ?: call.parameters["id"]?.let(::listOf)?.map(::ItemID)
                ?: emptyList()
            ).respond(call)
        }

        get("/mods") {
            source.getModifications(
                call.parameters.getAll("ids")?.map(::ModificationID)
                ?: call.parameters["id"]?.let(::listOf)?.map(::ModificationID)
                ?: emptyList()
            ).respond(call)
        }

        get("/players") {
            source.getPlayers(
                call.parameters.getAll("ids")?.map(::PlayerID)
                ?: call.parameters["id"]?.let(::listOf)?.map(::PlayerID)
                ?: emptyList()
            ).respond(call)
        }

        redirectInternally("/ticker", "/globalEvents")
        get("/globalEvents") { source.getGlobalEvents().respond(call) }

        redirectInternally("/sim", "/simulationData")
        get("/simulationData") { source.getSimulationData().respond(call) }

        redirectInternally("/feed/phase", "/feedbyphase")
        get("/feedbyphase") {
            try {
                source.getFeedByPhase(
                    phase = call.parameters.getOrFail("phase").toInt(),
                    season = call.parameters.getOrFail("season").toInt()
                ).respond(call)
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }

        route("/feed") {
            get("/global") {
                try {
                    source.getGlobalFeed(
                        category = call.parameters["category"]?.toIntOrNull(),
                        limit = call.parameters["limit"]?.toIntOrNull() ?: 100,
                        type = call.parameters["type"]?.toIntOrNull(),
                        sort = call.parameters["sort"]?.toIntOrNull(),
                        start = call.parameters["start"],
                        upNuts = upNuts,
                        fanID = call.fanID
                    ).respond(call)
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }

            get("/player") {
                try {
                    source.getPlayerFeed(
                        id = PlayerID(call.parameters.getOrFail("id")),
                        category = call.parameters["category"]?.toIntOrNull(),
                        limit = call.parameters["limit"]?.toIntOrNull() ?: 100,
                        type = call.parameters["type"]?.toIntOrNull(),
                        sort = call.parameters["sort"]?.toIntOrNull(),
                        start = call.parameters["start"],
                        upNuts = upNuts,
                        fanID = call.fanID
                    ).respond(call)
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }

            get("/team") {
                try {
                    source.getTeamFeed(
                        id = TeamID(call.parameters.getOrFail("id")),
                        category = call.parameters["category"]?.toIntOrNull(),
                        limit = call.parameters["limit"]?.toIntOrNull() ?: 100,
                        type = call.parameters["type"]?.toIntOrNull(),
                        sort = call.parameters["sort"]?.toIntOrNull(),
                        start = call.parameters["start"],
                        upNuts = upNuts,
                        fanID = call.fanID
                    ).respond(call)
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }
        }

        get("/offseasonSetup") {
            try {
                val response = client.get<HttpResponse>("https://www.blaseball.com/database/offseasonSetup?${call.request.uri.substringAfter('?')}")
                call.respondBytes(response.receive(), response.contentType())
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
    }

    fun Route.events() {
        get("/streamData") {
            try {
                call.respondTextWriter(ContentType.Text.EventStream) {
                    source.getLiveDataStream()
                        .doOnSuccess { flow ->
                            flow.collect { data ->
                                try {
                                    write("data:")
                                    write(data)
                                    write("\n\n")
                                    flush()
                                } catch (th: Throwable) {
//                                    th.printStackTrace()
                                    close()
                                }
                            }
                        }
                }
            } catch (th: Throwable) {
//                th.printStackTrace()
            }
        }
    }
}

fun Route.blaseballWebpage(league: BlasementLeague) = league.routingWebpage(this)
fun Route.blaseball(league: BlasementLeague) = league.routing(this)