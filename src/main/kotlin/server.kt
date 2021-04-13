import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.GameID
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import dev.brella.ktornea.apache.KtorneaApache
import dev.brella.ktornea.common.installGranularHttp
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.compression.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.features.*
import io.ktor.html.respondHtml
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.time.Duration

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

fun Application.module(testing: Boolean = false) {
    val json = Json { }

    install(ContentNegotiation) {
        json(json)
    }

    install(CORS) {
        anyHost()
    }

    install(ConditionalHeaders)
    install(StatusPages)

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
        timeout = Duration.ofSeconds(15)
        masking = false
    }

    val client = HttpClient(KtorneaApache) {
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

    val liveData = LiveData(blaseballApi, chroniclerApi, GlobalScope)
    val blaseballFeed = BlaseballFeed(blaseballApi, chroniclerApi, GlobalScope)

    routing {
        get("/random") {
            val simData = blaseballApi.getSimulationData()
            val gamesToday = blaseballApi.getGamesByDate(season = simData.season, day = simData.day)

            call.respondRedirect("/${simData.season + 1}/${simData.day + 1}/${gamesToday.random().id.id}")
        }

        get("/random/listen") {
            val simData = blaseballApi.getSimulationData()
            val gamesToday = blaseballApi.getGamesByDate(season = simData.season, day = simData.day)

            call.respondRedirect("/${simData.season + 1}/${simData.day + 1}/${gamesToday.random().id.id}/listen")
        }

        get("/{season}/{day}/{game}") {
            val season = call.parameters["season"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val day = call.parameters["day"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val game = call.parameters["game"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val games = liveData.getGame(season, day, GameID(game)) ?: return@get call.respond(HttpStatusCode.InternalServerError)

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

        get("/{season}/{day}/{game}/listen") {
            val season = call.parameters["season"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val day = call.parameters["day"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val game = call.parameters["game"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            call.respondText(contentType = ContentType.parse("text/html"), text = """
              <!DOCTYPE html>
              <meta charset="utf-8" />
              <title>WebSocket Test</title>
              <script language="javascript" type="text/javascript">
            
              var wsUri = "ws://" + new URL(document.URL).host + "/$season/$day/$game/listen";
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
            """.trimIndent())
        }

        webSocket("/{season}/{day}/{game}/listen") {
            val season = call.parameters["season"]?.toIntOrNull()
                         ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No season provided"))
            val day = call.parameters["day"]?.toIntOrNull()
                      ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No day provided"))
            val game = call.parameters["game"]
                       ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No game provided"))

            val localGame = liveData.getLocalGame(season, day, GameID(game))
                            ?: return@webSocket close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Game not cached / locally stored"))

            println("New client accepted: $localGame")

            try {
                val globalFeed = blaseballFeed.globalFeedFlow.onEach { feedEvent ->
                    send(json.encodeToString(buildJsonObject {
                        put("type", "FEED_EVENT")
                        put("data", json.decodeFromString(json.encodeToString(feedEvent)))
                    }))
                }.launchIn(this)
                val updateLog = localGame.updateLog.onEach { schedule ->
                    send(json.encodeToString(buildJsonObject {
                        put("type", "GAME_UPDATE")
                        put("data", json.decodeFromString(json.encodeToString(schedule)))
                    }))
                }.launchIn(this)

                globalFeed.join()
                updateLog.join()

                println("Closing client...")
            } catch (th: Throwable) {
                th.printStackTrace()
                throw th
            }
        }
    }
}