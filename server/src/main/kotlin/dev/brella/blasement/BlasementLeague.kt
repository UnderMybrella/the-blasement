package dev.brella.blasement

import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.ItemID
import dev.brella.kornea.blaseball.base.common.ModificationID
import dev.brella.kornea.blaseball.chronicler.EnumOrder
import dev.brella.kornea.errors.common.doOnSuccess
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import redirectInternally
import respond
import respondJsonArray
import respondJsonObject
import respondOnFailure
import set
import java.io.File

class BlasementLeague(val source: BlasementDataSource) {

}

fun Route.blaseballWebpage(client: HttpClient, source: BlasementDataSource) {
    val base = this.toString()
    println("Route: $source @ $base")
    val siteData = BlasementSiteData(client, source, base)
    siteData.launch(GlobalScope)

    get("/") { siteData.respondIndexHtml(call) }

    get("/{...}") {
        println("Tailcard url ${call.request.path()}")

        siteData.respondIndexHtml(call)
    }

    get("/main.js") { siteData.respondMainJs(call) }

    get("/2.js") { siteData.respond2Js(call) }

    get("/main.css") { siteData.respondMainCss(call) }
}

fun Route.blaseball(client: HttpClient, source: BlasementDataSource) {

//    get("/{...}") {
//        val url = "http://localhost:8080${call.request.path()}"
//        println("Returning $url")
//        val response = client.get<HttpResponse>(url)
//
//        call.respondBytes(response.receive(), response.contentType())
//    }

    blaseballWebpage(client, source)

    route("/api") api@{
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
            call.respondJsonObject {
                this["id"] = "00000000-0000-0000-0000-000000000000"
                this["email"] = "example@domain.org"
                this["appleId"] = JsonNull
                this["googleId"] = JsonNull
                this["facebookId"] = JsonNull
                this["name"] = JsonNull
                this["password"] = JsonNull
                this["coins"] = 0
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
//                    this["Max_Bet"] = 1
                }
                this["lightMode"] = false
                this["packSize"] = 1
                this["spread"] = buildJsonArray { }
                this["coffee"] = 0
                this["favNumber"] = 0
                this["snackOrder"] = buildJsonArray {
//                    add("Max_Bet")
                }
                this["trackers"] = buildJsonObject {
                    this["BEGS"] = 0
                    this["BETS"] = 0
                    this["VOTES_CAST"] = 0
                    this["SNACKS_BOUGHT"] = 0
                    this["SNACK_UPGRADES"] = 0
                }
            }
        }
        get("/getUserRewards") {
            call.respondJsonObject {
                this["coins"] = 0
                this["toasts"] = buildJsonArray { }
                this["lightMode"] = false
            }
        }
        get("/getActiveBets") { call.respondJsonArray { } }

        redirectInternally("/idol_board", "/getIdols")
        get("/getIdols") {
            source.getIdolBoard().respond(call)
        }

        redirectInternally("/hall_of_flame", "/getTribute")
        get("/getTribute") {
            source.getHallOfFlamePlayers().respond(call)
        }
    }

    route("/database") {
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
                        sort = call.parameters["sort"]?.let { str ->
                            when (str.toLowerCase()) {
                                "0" -> EnumOrder.DESC
                                "1" -> EnumOrder.ASC
                                "asc" -> EnumOrder.ASC
                                "desc" -> EnumOrder.DESC
                                else -> null
                            }
                        },
                        start = call.parameters["start"]
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

    route("/events") {
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