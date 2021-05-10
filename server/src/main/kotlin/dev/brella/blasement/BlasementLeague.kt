package dev.brella.blasement

import BlasementEventFeed
import BlasementHostFan
import TheBlasement
import dev.brella.blasement.common.*
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.EnumBlaseballSnack
import dev.brella.kornea.blaseball.base.common.FeedID
import dev.brella.kornea.blaseball.base.common.ItemID
import dev.brella.kornea.blaseball.base.common.ModificationID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.ktornea.common.putAsResult
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.security.SignatureException
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonArray
import redirectInternally
import respond
import respondJsonObject
import respondOnFailure
import set
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import UPNUTS_HOST
import dev.brella.blasement.common.events.*
import dev.brella.kornea.blaseball.base.common.BettingPayouts
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.MissingClaimException
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlin.math.roundToInt

import java.util.UUID as JUUID
import dev.brella.kornea.blaseball.base.common.UUID as KUUID

class BlasementLeague(val blasement: TheBlasement, val client: HttpClient, val source: BlasementDataSource) {
    //    val fans: MutableMap<FanID, BlaseballFanPayload> = HashMap()
    val upNuts: MutableMap<FeedID, MutableSet<FanID>> = HashMap()

//    inline val ApplicationCall.fan
//        get() = fans.computeIfAbsent(fanID) { fanID ->
//            createFanPayload(fanID, "user@example.com", TeamID("d2634113-b650-47b9-ad95-673f8e28e687"))
//        }

//    suspend inline fun ApplicationCall.fan(): BlasementHostFan {
///*        fans.computeIfAbsent(fanID) { fanID ->
//            createFanPayload(fanID, "user@example.com", TeamID("d2634113-b650-47b9-ad95-673f8e28e687"))
//        }*/
//
//        val fanID = this.fanID
//        return blasement.fans.firstOrNull { fan -> fan.id == fanID } ?: blasement.newFan(fanID.id, null, TeamID("d2634113-b650-47b9-ad95-673f8e28e687")).second
//    }

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
            route("/auth") { auth() }
        }

    val authScope = CoroutineScope(SupervisorJob())
    val authRequests: MutableMap<String, Int> = ConcurrentHashMap()

    inline val ApplicationCall.authToken: String?
        get() = request.header("Authorization") ?: request.cookies[TheBlasement.COOKIE_NAME]

    inline val ApplicationCall.authJwt: Jws<Claims>?
        get() = try {
            authToken?.let { blasement.parser.parseClaimsJws(it) }
        } catch (th: Throwable) {
            when (th) {
                is JwtException, is SignatureException, is IllegalArgumentException, is MissingClaimException -> null
                else -> throw th
            }
        }

    inline val ApplicationCall.fan: BlasementHostFan?
        get() = authJwt?.let { blasement.fans[KUUID.fromString(it.body.id)] }

    inline fun authValid(key: String): Boolean {
        if ((authRequests[key] ?: 0) >= 3) return true

        authRequests.compute(key) { _, v -> v?.plus(1) ?: 1 }

        authScope.launch {
            delay(10_000)
            authRequests.compute(key) { _, v -> v?.minus(1) }
        }

        return false
    }

    fun Route.auth() {
        post("/local") {
            if (authValid(call.request.origin.remoteHost)) return@post call.respond(HttpStatusCode.TooManyRequests, EmptyContent)

            val payload = call.receive<BlaseballAuthPayload>()
            if (authValid(payload.username)) return@post call.respond(HttpStatusCode.TooManyRequests, EmptyContent)

            if (payload.isLogin) {
                val result = blasement.login(payload.username, payload.password)

                if (result == null) {
                    call.respondJsonObject(HttpStatusCode.BadRequest) { this["error"] = "Error: Wrong password for user" }
                } else {
                    call.response.cookies.append(TheBlasement.COOKIE_NAME, result.first, httpOnly = true, path = "/")

                    call.respondRedirect(this@auth.toString().substringBeforeLast('/') + "/", permanent = false)
                }
            } else {
                when {
                    payload.password != payload.passwordConfirm ->
                        call.respondJsonObject(HttpStatusCode.BadRequest) { this["error"] = "Passwords do not match" }
                    blasement.fans.any { (_, fan) -> fan.email == payload.username } ->
                        call.respondJsonObject(HttpStatusCode.BadRequest) { this["error"] = "Email already registered" }
                    else -> {
                        val (auth) = blasement.newFanWithEmail(fanID = JUUID.randomUUID(), email = payload.username, password = blasement.passwords.encode(payload.password))

                        call.response.cookies.append(TheBlasement.COOKIE_NAME, auth, httpOnly = true, path = "/")

                        call.respondRedirect(this@auth.toString().substringBeforeLast('/') + "/", permanent = false)
                    }
                }
            }
        }

        get("/logout") {
            call.response.cookies.appendExpired(TheBlasement.COOKIE_NAME, path = "/")

            call.respondRedirect(this@auth.toString().substringBeforeLast('/') + "/", permanent = false)
        }
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
//            call.respond(call.fan().toFrontendPayload())
            val fan = call.fan

            if (fan == null) {
                call.respondJsonObject(HttpStatusCode.Unauthorized) {
                    this["error"] = "Invalid auth token."
                }
            } else {
                call.respond(fan.toFrontendPayload())
            }
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
            val fan = call.fan

            if (fan == null) {
                call.respondJsonObject(HttpStatusCode.Unauthorized) {
                    this["error"] = "Invalid auth token."
                }
            } else {
                call.respondJsonObject {
                    this["coins"] = fan.coins
                    this["toasts"] = JsonArray(fan.getToasts(source.now().utc.unixMillisLong).map(::JsonPrimitive))
                    this["lightMode"] = fan.lightMode
                }
            }
        }
        get("/getActiveBets") {
            val fan = call.fan ?: return@get call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Unauthorized"
            }

            call.respond(fan.currentBets.map { (gameID, bet) ->
                buildJsonObject {
                    this["targets"] = buildJsonArray {
                        add(bet.team.id)
                        add(gameID.id)
                    }

                    this["amount"] = bet.bet
//                    this["type"] = 0
//                    this["userId"] = fan.id.id
                }
            })
        }
        post("/bet") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Unauthorized"
            }

            val bet = call.receive<BlaseballBetPayload>()

//            println("Betting: $bet")

            when (fan.placeBet(onGame = bet.bettingGame, onTeam = bet.bettingTeam, amount = bet.amount)) {
                EnumBetFail.NOT_ENOUGH_COINS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Coins"
                }
                EnumBetFail.BET_TOO_HIGH -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Bet Too High"
                }
                EnumBetFail.CANT_BET_ZERO -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Can't Bet Zero Coins"
                }
                EnumBetFail.INVALID_TEAM -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Invalid Team"
                }
                EnumBetFail.NO_SNAKE_OIL -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Snake Oil"
                }
                null -> call.respondJsonObject(HttpStatusCode.OK) {
                    this["message"] = "Bet Placed"
                }
            }
        }

        post("/chooseIdol") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Invalid auth token."
            }

            val idol = call.receive<BlaseballChooseIdolPayload>()

            //Check if games are running
            fan.setIdol { idol.playerId }

            call.respond(HttpStatusCode.OK, EmptyContent)
        }
        post("/setFavoriteTeam") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Invalid auth token."
            }

            if (fan.favouriteTeam != null)
                return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Team already set (?)"
                }

            val team = call.receive<BlaseballSetFavouriteTeamPayload>()

            fan.setFavouriteTeam { team.teamId }

            call.respond(HttpStatusCode.NoContent, EmptyContent)
        }
        post("/updateFavoriteTeam") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Invalid auth token."
            }

            val team = call.receive<BlaseballUpdateFavouriteTeamPayload>()

            when (fan.changeTeam(team.newTeamId).second) {
                EnumChangeTeamFail.NO_FLUTE -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Flutes in your inventory."
                }
                EnumChangeTeamFail.ALREADY_FAVOURITE_TEAM -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "${team.teamName} is already your favourite team."
                }
                EnumChangeTeamFail.GAME_IN_PROGRESS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Cannot change team while game is in progress"
                }
                null -> call.respond(HttpStatusCode.NoContent, EmptyContent)
            }
        }

        post("/upNut") {
            val payload = call.receive<BlaseballUpNutPayload>()

            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Unauthorized"
            }

            val existingPeanuts = fan.inventory[EnumBlaseballSnack.PEANUTS] ?: 0

            if (existingPeanuts <= 0) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "You don't have any Peanuts."
            }

            client.putAsResult<String>("$UPNUTS_HOST/${payload.eventId.id}/3de20b85-df54-4894-8d23-057796cd1a3b") {
                call.authToken?.let { header("Authorization", it) }

                parameter("source", fan.id.id)
                parameter("time", BLASEBALL_TIME_PATTERN.format(source.now()))
            }.doOnSuccess {
                fan.removeItemQuantity(EnumBlaseballSnack.PEANUTS) { 1 }

                call.respondJsonObject {
                    this["message"] = "You Upshelled an event."
                }
            }.respondOnFailure(call)
        }
        post("/eatADangPeanut") {
            val payload = call.receive<BlaseballEatADangNutPayload>()

            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Unauthorized"
            }

            val existingPeanuts = fan.inventory[EnumBlaseballSnack.PEANUTS] ?: 0
            val eaten = payload.amount

            if (existingPeanuts < eaten) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "You don't have enough Peanuts."
            } else if (eaten <= 0) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Invalid Peanut count."
            }

//            fans[fanID] = fan.copy(peanutsEaten = fan.peanutsEaten + eaten, snacks = fan.snacks.copy(peanuts = existingPeanuts - eaten))
//            fan.peanutsEaten
            fan.removeItemQuantity(EnumBlaseballSnack.PEANUTS) { eaten }

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        post("/buyUnlockShop") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Invalid auth token."
            }

            //*Technically*, this responds with a 200 every time - however, it also takes 20 coins from the player even if they have the shop unlocked
            //TODO: Compat mode?
            //NOTE: Possible race condition?
            val (_, error) = fan.purchaseShopMembershipCard()

            when (error) {
                null -> call.respondJsonObject {
                    this["message"] = "Shop Unlocked"
                }

                EnumUnlockFail.NOT_ENOUGH_COINS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Coins"
                }
                EnumUnlockFail.ALREADY_UNLOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Shop Already Unlocked"
                }
            }
        }
        post("/buyUnlockElection") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Invalid auth token."
            }

            //*Technically*, this responds with a 200 every time - however, it also takes 20 coins from the player even if they have the shop unlocked
            //TODO: Compat mode?
            //NOTE: Possible race condition?
            val (_, error) = fan.purchaseVotingRights()

            when (error) {
                null -> call.respondJsonObject {
                    this["message"] = "Voting Rights Unlocked"
                }

                EnumUnlockFail.NOT_ENOUGH_COINS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Coins"
                }
                EnumUnlockFail.ALREADY_UNLOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Voting Rights Already Unlocked"
                }
            }
        }

        post("/updateProfile") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Unauthorized"
            }

            val payload = call.receive<BlaseballUpdateProfilePayload>()

            fan.setFavNumber { payload.favNumber }
            fan.setCoffee { payload.coffee }

            call.respond(HttpStatusCode.OK, EmptyContent)
        }
        post("/updateSettings") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Unauthorized"
            }

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        redirectInternally("/buySnackNoUpgrade", "/buySnack")
        post("/buySnack") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Invalid auth token."
            }

            val payload = call.receive<BlaseballBuySnackPayload>()

            val item = EnumBlaseballSnack.fromID(payload.snackId)
                       ?: return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                           this["error"] = "Unknown Snack"
                       }

            val (_, error) = fan.buySnack(1, item)

            when (error) {
                EnumPurchaseItemFail.MEMBERSHIP_LOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Shop Membership"
                }
                EnumPurchaseItemFail.NOT_ENOUGH_COINS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Coins"
                }
                EnumPurchaseItemFail.TIER_TOO_HIGH -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Tier Too High"
                }
                EnumPurchaseItemFail.INVENTORY_FULL -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Inventory Full"
                }
                EnumPurchaseItemFail.ITEM_COUNT_FULL -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Item Count Full"
                }
                null -> call.respondJsonObject(HttpStatusCode.OK) {
                    this["message"] = "You Bought 1 ${item.name.split('_').joinToString(" ") { it.toLowerCase().capitalize() }}"
                }
            }
        }
        post("/sellSnack") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Invalid auth token."
            }

            val payload = call.receive<BlaseballSellSnackPayload>()

            val item = EnumBlaseballSnack.fromID(payload.snackId)
                       ?: return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                           this["error"] = "Unknown Snack"
                       }

            val (profit, error) = fan.sell(payload.amount, item)

            when (error) {
                EnumSellItemFail.MEMBERSHIP_LOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Shop Membership"
                }
                EnumSellItemFail.ITEM_NOT_IN_INVENTORY -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Item Not in Inventory"
                }
                EnumSellItemFail.NOT_ENOUGH_ITEMS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Items"
                }
                null -> call.respondJsonObject(HttpStatusCode.OK) {
                    this["message"] = "You Sold ${payload.amount} ${item.name.split('_').joinToString(" ") { it.toLowerCase().capitalize() }}"
                }
            }
        }

        post("/buySlot") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Invalid auth token."
            }

            val (_, error) = fan.purchaseSlot()

            when (error) {
                EnumPurchaseSlotFail.MEMBERSHIP_LOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Shop Membership"
                }
                EnumPurchaseSlotFail.NOT_ENOUGH_COINS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Coins"
                }
                EnumPurchaseSlotFail.TOO_MANY_SLOTS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Too Many Slots"
                }
                EnumPurchaseSlotFail.INVALID_AMOUNT -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Invalid Amount To Buy"
                }

                null -> call.respondJsonObject(HttpStatusCode.OK) {
                    this["message"] = "You bought a snack slot."
                }
            }

            call.respond(HttpStatusCode.OK, EmptyContent)
        }
        post("/sellSlot") {
            val fan = call.fan ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                this["error"] = "Invalid auth token."
            }

            val payload = call.receive<BlaseballSellSlotPayload>()

            //We don't really care about indices atm
            val (_, error) = fan.sellSlot()

            when (error) {
                EnumSellSlotFail.MEMBERSHIP_LOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Shop Membership"
                }
                EnumSellSlotFail.NO_EMPTY_SLOTS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Empty Slots"
                }
                EnumSellSlotFail.NOT_ENOUGH_SLOTS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Slots"
                }
                EnumSellSlotFail.INVALID_AMOUNT -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Invalid Amount to Sell"
                }

                null -> call.respondJsonObject(HttpStatusCode.OK) {
                    this["message"] = "You bought a snack slot."
                }
            }
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
                call.parameters.getAll("ids")?.flatMap { it.split(',') }?.map(::PlayerID)
                ?: call.parameters["id"]?.let(::listOf)?.flatMap { it.split(',') }?.map(::PlayerID)
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
                        fanID = call.fan?.id
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
                        fanID = call.fan?.id
                    ).respond(call)
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }

            get("/team") {
                try {
                    val parameters = call.request.queryParameters
                    source.getTeamFeed(
                        id = TeamID(parameters.getOrFail("id")),
                        category = parameters["category"]?.toIntOrNull(),
                        limit = parameters["limit"]?.toIntOrNull() ?: 100,
                        type = parameters["type"]?.toIntOrNull(),
                        sort = parameters["sort"]?.toIntOrNull(),
                        start = parameters["start"],
                        upNuts = upNuts,
                        fanID = call.fan?.id
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

    /** Rewards Calculation */
    val globalEventFeed = BlasementEventFeed(source.globalFeed, blasement)
    val fans get() = blasement.fans

    val trace = source.globalFeed.onEach { event ->
//        println(event.event)
    }.launchIn(blasement)

    val bettingRewards = globalEventFeed.onGameEnd.onEach { event ->
//        println("Game Ending: ${event.gameStep.id} (${event.gameStep.homeTeamName} vs ${event.gameStep.awayTeamName}; ${event.winner} won)")
        val gameID = event.gameStep.id
        val winner = event.winner

        val time = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            val bet = fan.gameCompleted(gameID) ?: return@forEach

            if (bet.team == winner) {
                val returns = when (bet.team) {
                    event.gameStep.homeTeam -> BettingPayouts.currentSeason(bet.bet, event.gameStep.homeOdds)
                    event.gameStep.awayTeam -> BettingPayouts.currentSeason(bet.bet, event.gameStep.awayOdds)
                    else -> bet.bet
                }

                fan.setCoins { it + returns }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.WonBet(gameID, bet.team, bet.bet, returns))

                fan.addToast("You bet ${bet.bet} on the {tnn}${bet.team.id} and won ${returns} coins.", time)
            } else {
                fan.fanEvents.emit(ServerEvent.FanActionResponse.LostBet(gameID, bet.team, bet.bet))

                fan.addToast("You bet ${bet.bet} on the {tnn}${bet.team.id} and lost.", time)
            }
        }
    }.launchIn(blasement)

    val popcornRewards = globalEventFeed.onGameEnd.onEach { event ->
        val winner = event.winner
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.favouriteTeam == winner) {
                val popcornCount = fan.inventory[EnumBlaseballSnack.POPCORN]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.POPCORN[popcornCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.POPCORN))
                fan.addToast("You earned $coinPayout coins from 1 {tnn}${winner} win.", eventTime)
            }
        }
    }.launchIn(blasement)

    val stalePopcornRewards = globalEventFeed.onGameEnd.onEach { event ->
        val loser = event.gameStep.homeTeam.takeUnless { it == event.winner } ?: event.gameStep.awayTeam
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.favouriteTeam == loser) {
                val snackCount = fan.inventory[EnumBlaseballSnack.STALE_POPCORN]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.STALE_POPCORN[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.STALE_POPCORN))
                fan.addToast("You earned $coinPayout coins from 1 {tnn}${loser} loss.", eventTime)
            }
        }
    }.launchIn(blasement)

//    val breakfastRewards =
    /** Earn coins every time your Team shames another Team. */
    val taffyRewards = globalEventFeed.onTeamShames.onEach { event ->
        val team = event.team
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.favouriteTeam == team) {
                val snackCount = fan.inventory[EnumBlaseballSnack.TAFFY]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.TAFFY[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.TAFFY))
                fan.addToast("You earned $coinPayout coins from {tnn}${team} shaming 1 time.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Earn coins every time your Team gets shamed. */
    val lemonadeRewards = globalEventFeed.onTeamShamed.onEach { event ->
        val team = event.team
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.favouriteTeam == team) {
                val snackCount = fan.inventory[EnumBlaseballSnack.LEMONADE]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.LEMONADE[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.LEMONADE))
                fan.addToast("You earned $coinPayout coins from {tnn}${team} being shamed 1 time.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Crisp. Earn 3 coins when your Idol strikes a batter out. */
    val chipRewards = globalEventFeed.onStrikeout.onEach { event ->
        val pitcher = event.gameStep.homePitcher ?: event.gameStep.awayPitcher!!
        val pitcherName = if (pitcher == event.gameStep.homePitcher) event.gameStep.homePitcherName else event.gameStep.awayPitcherName

        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.idol == pitcher) {
                val snackCount = fan.inventory[EnumBlaseballSnack.CHIPS]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.CHIPS[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.CHIPS))
//                fan.addToast("You earned $coinPayout coins from ", eventTime)
                fan.addToast("You earned $coinPayout coins from $pitcherName striking a batter out.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Medium Rare. Earn coins when your Idol pitches a shutout. */
    val burgerRewards = globalEventFeed.onShutout.onEach { event ->
        val pitcher = event.gameStep.homePitcher ?: event.gameStep.awayPitcher!!
        val pitcherName = if (pitcher == event.gameStep.homePitcher) event.gameStep.homePitcherName else event.gameStep.awayPitcherName
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.idol == pitcher) {
                val snackCount = fan.inventory[EnumBlaseballSnack.BURGER]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.BURGER[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.BURGER))
                fan.addToast("You earned $coinPayout coins from $pitcherName pitching a shutout.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Uh oh. Earn coins when a batter hits a home run off of your Idol's pitch. */
    val meatballRewards = globalEventFeed.onHomeRun.onEach { event ->
        val pitcher = event.gameStep.homePitcher ?: event.gameStep.awayPitcher!!
        val pitcherName = if (pitcher == event.gameStep.homePitcher) event.gameStep.homePitcherName else event.gameStep.awayPitcherName
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.idol == pitcher) {
                val snackCount = fan.inventory[EnumBlaseballSnack.MEATBALL]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.MEATBALL[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.MEATBALL))
                fan.addToast("You earned $coinPayout coins from a batter hitting a home run off of $pitcherName's pitch'.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Hot Dog! Earn coins when your Idol hits a home run. */
    val hotDogRewards = globalEventFeed.onHomeRun.onEach { event ->
        val batter = event.gameStep.homeBatter ?: event.gameStep.awayBatter!!
        val batterName = if (batter == event.gameStep.homeBatter) event.gameStep.homeBatterName else event.gameStep.awayBatterName
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.idol == batter) {
                val snackCount = fan.inventory[EnumBlaseballSnack.HOT_DOG]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.HOT_DOG[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.HOT_DOG))
                fan.addToast("You earned $coinPayout coins from $batterName hitting a home run.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Ptooie. Earn 5 coins when your Idol gets a hit. */
    val sunflowerSeedRewards = globalEventFeed.onHit.onEach { event ->
        val batter = event.gameStep.homeBatter ?: event.gameStep.awayBatter!!
        val batterName = if (batter == event.gameStep.homeBatter) event.gameStep.homeBatterName else event.gameStep.awayBatterName
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.idol == batter) {
                val snackCount = fan.inventory[EnumBlaseballSnack.SUNFLOWER_SEEDS]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.SUNFLOWER_SEEDS[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.SUNFLOWER_SEEDS))
                fan.addToast("You earned $coinPayout coins from $batterName getting a hit.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Ptooie. Earn coins every time your Idol steals a base. */
    val pickleRewards = globalEventFeed.onStolenBase.onEach { event ->
        val batter = event.event.playerTags.firstOrNull() ?: return@onEach
        val batterName = if (batter == event.gameStep.homeBatter) event.gameStep.homeBatterName else event.gameStep.awayBatterName
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.idol == batter) {
                val snackCount = fan.inventory[EnumBlaseballSnack.PICKLES]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.PICKLES[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.PICKLES))
                fan.addToast("You earned $coinPayout coins from $batterName stealing a base.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Ptooie. Earn coins for every baserunner swept away by Flooding weather across the league. */
    val slushieRewards = globalEventFeed.onFlood.onEach { event ->
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            val snackCount = fan.inventory[EnumBlaseballSnack.SLUSHIE]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballSnack.SLUSHIE[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = ((payoutRate * event.playersFlooded.size) * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.SLUSHIE))
            fan.addToast("You earned $coinPayout coins from ${event.playersFlooded.size} Baserunners being cleared in Flood weather.", eventTime)
        }
    }.launchIn(blasement)

    /** Refreshing. Earn coins every time a Player is incinerated. */
    val sundaeRewards = globalEventFeed.onIncineration.onEach { event ->
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            val snackCount = fan.inventory[EnumBlaseballSnack.SUNDAE]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballSnack.SUNDAE[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.SUNDAE))
            fan.addToast("You earned $coinPayout coins from 1 Incinerations; RIV.", eventTime)
        }
    }.launchIn(blasement)

    /** Earn coins for every time the Black Hole swallows a Win from any Team. */
    val wetPretzelRewards = globalEventFeed.onBlackHole.onEach { event ->
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            val snackCount = fan.inventory[EnumBlaseballSnack.WET_PRETZEL]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballSnack.WET_PRETZEL[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.WET_PRETZEL))
            fan.addToast("You earned $coinPayout coins from Black Holes being activated 1 time.", eventTime)
        }
    }.launchIn(blasement)

    /** Earn coins for every time Sun 2 sets a Win on a Team. */
    val doughnutRewards = globalEventFeed.onSun2.onEach { event ->
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            val snackCount = fan.inventory[EnumBlaseballSnack.DOUGHNUT]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballSnack.DOUGHNUT[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.DOUGHNUT))
            fan.addToast("You earned $coinPayout coins from Sun 2 being activated 1 time.", eventTime)
        }
    }.launchIn(blasement)
}

fun Route.blaseballWebpage(league: BlasementLeague) = league.routingWebpage(this)
fun Route.blaseball(league: BlasementLeague) = league.routing(this)