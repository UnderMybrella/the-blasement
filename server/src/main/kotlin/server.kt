import dev.brella.blasement.BlasebackMachineAccelerated
import dev.brella.blasement.BlasementDataSourceWrapper
import dev.brella.blasement.BlasementLeague
import dev.brella.blasement.blaseball
import dev.brella.blasement.convenience
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.FeedID
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import dev.brella.ktornea.common.KorneaHttpResult
import dev.brella.ktornea.common.installGranularHttp
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.compression.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.event.Level
import websocket.BlasementDweller
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.createType
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

fun HTML.index() {
    head {
        title("Hello from Ktor!")
    }
    body {
        div {
            +"Hello from Ktor"
        }
    }
}

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

const val CHRONICLER_HOST = "https://api.sibr.dev/chronicler"
const val UPNUTS_HOST = "https://upnuts.brella.dev"

@OptIn(ExperimentalTime::class)
fun Application.module(testing: Boolean = false) {
    val json = Json {
        ignoreUnknownKeys = true
//        encodeDefaults = true
    }

    install(ContentNegotiation) {
        json(json)
    }

    install(CORS) {
        anyHost()
    }

    install(ConditionalHeaders)
    install(StatusPages) {
        exception<Throwable> { cause -> call.respond(HttpStatusCode.InternalServerError, cause.stackTraceToString()) }
        exception<KorneaResultException> { cause ->
            val result = cause.result
            if (result is KorneaHttpResult) call.response.header("X-Response-Source", result.response.request.url.toString())
            result.respondOnFailure(call)
        }
    }
    install(CallLogging) {
        level = Level.INFO
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
        timeout = Duration.ofSeconds(15)
        masking = false
    }

    val client = HttpClient(OkHttp) {
        installGranularHttp()

        install(ContentEncoding) {
            gzip()
            deflate()
            identity()
        }

        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }

        expectSuccess = false

        defaultRequest {
            userAgent("Mozilla/5.0 (X11; Linux x86_64; rv:85.0) Gecko/20100101 Firefox/85.0")
        }
    }

    val blaseballApi = BlaseballApi(client)
    val chroniclerApi = ChroniclerApi(client)

//    val massProduction = BlasebackMachineAccelerated.massProduction(client, blaseballApi, json, 5.seconds)
//
//    val occurrences: MutableMap<FeedID, Int> = ConcurrentHashMap()
//    massProduction.globalFeed.onEach { event ->
//        occurrences.compute(event.event.id) { _, v -> v?.plus(1) ?: 1 }
//        println("[${occurrences[event.event.id]}] $event")
//    }.launchIn(GlobalScope).let { job ->
//        runBlocking { job.join() }
//    }

    val blasement = TheBlasement(json, client, blaseballApi, chroniclerApi)

    routing {
        route("/blaseball") {
//            route("/current") {
//                blaseball(BlasementLeague(blasement, client, BlasementDataSourceWrapper(blasement)))
//            }
//
//            redirectInternally("/season/17", "/collections")
//            route("/collections") {
//                blaseball(BlasementLeague(blasement, client, BlasebackMachineAccelerated.collections(client, blaseballApi, json, 5.seconds)))
//            }

            redirectInternally("/season/16", "/mass_production")
            route("/mass_production") {
                blaseball(BlasementLeague(blasement, client, BlasebackMachineAccelerated.massProduction(client, blaseballApi, json, 5.seconds)))
            }

//            redirectInternally("/season/15", "/live_bait")
//            route("/live_bait") {
//                blaseball(BlasementLeague(blasement, client, BlasebackMachineAccelerated.liveBait(client, blaseballApi, json, 5.seconds)))
//            }
        }

        route("/sibr") { convenience(client) }

        get("/random/{following...}") {
            val gamesToday = blasement.gamesToday()

            call.respondRedirect("/${gamesToday.random().id.id}${call.parameters.getAll("following")?.joinToString("/", prefix = "/") ?: ""}")
        }

        /*get("/{game}") {
            val game = call.parameters["game"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val games = blasement.liveData.getGame(GameID(game)) ?: return@get call.respond(HttpStatusCode.InternalServerError)

            call.respondHtml {
                body {
                    table {
                        tr {
                            th { +"Play Count" }
                            th { +"Last Update" }
                        }
                        games.entries
                            .sortedBy(Map.Entry<Int, *>::key)
                            .forEach { (play, game) ->
                                tr {
                                    td {
                                        +play.toString()
                                    }

                                    td {
                                        +game.lastUpdate
                                    }
                                }
                            }
                    }
                }
            }
        }

        get("/{game}/listen") {
            val game = call.parameters["game"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            call.respondText(
                contentType = ContentType.parse("text/html"), text = """
              <!DOCTYPE html>
              <meta charset="utf-8" />
              <title>WebSocket Test</title>
              <script language="javascript" type="text/javascript">
            
              var wsUri = "ws://" + new URL(document.URL).host + "/$game/listen";
              var output;
            
              function init()
              {
                output = document.getElementById("output");
                testWebSocket();
              }
            
              function testWebSocket()
              {
                websocket = new WebSocket(wsUri);
                websocket.onopen = function(evt) { onOpen(evt) };
                websocket.onclose = function(evt) { onClose(evt) };
                websocket.onmessage = function(evt) { onMessage(evt) };
                websocket.onerror = function(evt) { onError(evt) };
              }
            
              function onOpen(evt)
              {
                writeToScreen("CONNECTED");
              }
            
              function onClose(evt)
              {
                writeToScreen("DISCONNECTED");
              }
            
              function onMessage(evt)
              {
                writeToScreen('<span style="color: ' + (evt.data.includes("FEED") ? 'green' : 'blue') + ';">' + evt.data + '</span>');
              }
            
              function onError(evt)
              {
                writeToScreen('<span style="color: red;">ERROR:</span> ' + evt.data);
              }
            
              function doSend(message)
              {
                writeToScreen("SENT: " + message);
                websocket.send(message);
              }
            
              function writeToScreen(message)
              {
                var pre = document.createElement("p");
                pre.style.wordWrap = "break-word";
                pre.innerHTML = message;
                output.appendChild(pre);
              }
            
              window.addEventListener("load", init, false);
            
              </script>
            
              <h2>WebSocket Test</h2>
            
              <div id="output"></div>
            """.trimIndent()
            )
        }*/

        val clients: MutableList<BlasementDweller> = ArrayList()

        webSocket("/connect") {
            println("New generic client $this")
            val dweller = BlasementDweller(blasement, json, this)
            try {
                clients.add(dweller)
                dweller.join()
                println("Closed!")
            } catch (th: Throwable) {
                th.printStackTrace()
                throw th
            } finally {
                clients.remove(dweller)
            }
        }

        webSocket("/{game}/listen") {
            val game = call.parameters["game"]
                       ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No game provided"))

            val localGame = blasement.liveData.getLocalGame(GameID(game))
                            ?: return@webSocket close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Game not cached / locally stored"))

            println("New client accepted: $localGame")

            try {
//                val globalFeed = blasement.globalFeed.flow.onEach { feedEvent ->
//                    send(json.encodeToString(buildJsonObject {
//                        put("type", "FEED_EVENT")
//                        put("data", json.decodeFromString(json.encodeToString(feedEvent)))
//                    }))
//                }.launchIn(this)

                val updateLog = localGame.updateLog.onEach { schedule ->
                    send(json.encodeToString(buildJsonObject {
                        put("type", "GAME_UPDATE")
                        put("data", json.decodeFromString(json.encodeToString(schedule)))
                    }))
                }.launchIn(this)

//                globalFeed.join()
                updateLog.join()

                println("Closing client...")
            } catch (th: Throwable) {
                th.printStackTrace()
                throw th
            }
        }
    }
}