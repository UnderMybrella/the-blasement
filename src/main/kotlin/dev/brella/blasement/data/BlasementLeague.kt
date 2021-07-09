package dev.brella.blasement.data

import dev.brella.blasement.endpoints.BlaseballGlobalEventsEndpoint
import dev.brella.blasement.endpoints.BlasementGetUserEndpoint
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.CoroutineContext

data class BlasementLeague(
    val leagueID: String,
    val httpClient: HttpClient,
    val getUserEndpoint: BlasementGetUserEndpoint?,
    val getGlobalEventsEndpoint: BlaseballGlobalEventsEndpoint?,
    val timeSource: suspend () -> Instant
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob()
    val siteData = BlasementSiteData(
        httpClient, indexHtmlTransformers = listOf(
            SiteTransformer.InitialTextTransformer.ReplaceStaticAssets("/leagues/underground")
        ),
        mainJsTransformers = listOf(
            SiteTransformer.InitialTextTransformer.ReplaceApiCalls("/leagues/underground"),
            SiteTransformer.FinalTextTransformer.ReplaceTimeWithWebsocket("/leagues/underground")
        ),
        twoJsTransformers = listOf(),
        mainCssTransformers = listOf(),
        timeSource
    )

    init {
        siteData.launch(this)
    }
}

fun Routing.setupLeagues(leagues: Map<String, BlasementLeague>) {
    route("/leagues/{id}") {
        get {
            println("Endpoint: ${call.request.uri.trim().substringAfter("/leagues/underground")}")
            val league = leagues[call.parameters.getOrFail("id")] ?: return@get call.respond(HttpStatusCode.NotFound, buildJsonObject {
                put("error", "No league found")
            })

            league.siteData.respondIndexHtml(call)
        }

        get("/{...}") {
            println("Endpoint: ${call.request.uri.trim().substringAfter("/leagues/underground")}")
            val league = leagues[call.parameters.getOrFail("id")]
                         ?: return@get call.respond(HttpStatusCode.NotFound, buildJsonObject {
                             put("error", "No league found")
                         })

            league.siteData.respondIndexHtml(call)
        }

        get("/api/getUser") {
            val league = leagues[call.parameters.getOrFail("id")]
                         ?: return@get call.respond(HttpStatusCode.NotFound, buildJsonObject {
                             put("error", "No league found")
                         })

            call.respond(league.getUserEndpoint?.getUserFor(call) ?: return@get call.respond(HttpStatusCode.ServiceUnavailable, buildJsonObject {
                put("error", "Get User Endpoint not implemented")
            }))
        }

        get("/database/globalEvents") {
            val league = leagues[call.parameters.getOrFail("id")]
                         ?: return@get call.respond(HttpStatusCode.NotFound, buildJsonObject {
                             put("error", "No league found")
                         })

            call.respond(league.getGlobalEventsEndpoint?.getGlobalEventsFor(league, call) ?: return@get call.respond(HttpStatusCode.ServiceUnavailable, buildJsonObject {
                put("error", "Get Global Events not implemented")
            }))
        }

        get("/events/streamData") {
            call.respondRedirect("https://api.sibr.dev/corsmechanics/www.blaseball.com/events/streamData", false)
        }

        get("/database/{...}") {
            call.respondRedirect("https://api.sibr.dev/corsmechanics/www.blaseball.com/database/${call.request.uri.substringAfter("/database/")}", false)
        }

        get("/api/{...}") {
            call.respondRedirect("https://api.sibr.dev/corsmechanics/www.blaseball.com/api/${call.request.uri.substringAfter("/api/")}", false)
        }

        get("/main.js") {
            val league = leagues[call.parameters.getOrFail("id")]
                         ?: return@get call.respond(HttpStatusCode.NotFound, buildJsonObject {
                             put("error", "No league found")
                         })

            league.siteData.respondMainJs(call)
        }
        get("/main.css") {
            val league = leagues[call.parameters.getOrFail("id")]
                         ?: return@get call.respond(HttpStatusCode.NotFound, buildJsonObject {
                             put("error", "No league found")
                         })

            league.siteData.respondMainCss(call)
        }
        get("/2.js") {
            val league = leagues[call.parameters.getOrFail("id")]
                         ?: return@get call.respond(HttpStatusCode.NotFound, buildJsonObject {
                             put("error", "No league found")
                         })

            league.siteData.respond2Js(call)
        }
    }
}