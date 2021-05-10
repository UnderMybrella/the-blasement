import dev.brella.blasement.convenience
import dev.brella.kornea.blaseball.BlaseballApi
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
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.websocket.*
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.title
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import websocket.BlasementDweller
import java.time.Duration
import kotlin.time.ExperimentalTime

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
        blasement.routing(this)

        route("/sibr") { convenience(client) }

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
    }
}