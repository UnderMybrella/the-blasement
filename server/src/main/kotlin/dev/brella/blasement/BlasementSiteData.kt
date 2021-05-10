package dev.brella.blasement

import TheBlasement
import com.soywiz.klock.DateTimeTz
import compressGz
import dev.brella.blasement.blaseback.BlasementDataSource
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.json.BlaseballDateTimeSerialiser
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.doOnFailure
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.getOrNull
import dev.brella.kornea.errors.common.map
import dev.brella.kornea.errors.common.switchIfFailure
import dev.brella.ktornea.common.getAsResult
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import readAllBytes
import java.io.File
import kotlin.coroutines.CoroutineContext

class BlasementSiteData(val blasement: TheBlasement, val source: BlasementDataSource, val base: String) {
    companion object : CoroutineScope {
        override val coroutineContext: CoroutineContext = SupervisorJob()

        val INDEX_HTML_REGEX = "/".toRegex()
        val TWO_CHUNK_JS_REGEX = ".*2.*.js".toRegex()
        val MAIN_CHUNK_JS_REGEX = ".*main.*js".toRegex()
        val MAIN_CHUNK_CSS_REGEX = ".*main.*css".toRegex()

        //o.a.createElement("a",{className:"Auth-SocialAuth",href:"auth/facebook?redirectUrl=".concat(g)},o.a.createElement("div",{className:"Auth-SocialAuth-Icon-Container"},o.a.createElement(pu.e,null))," Continue with Facebook")
        val CONTINUE_WITH_FACEBOOK_REGEX = "(\\w+\\.\\w+).createElement\\(\"a\",\\{className:\"Auth-SocialAuth\",href:\"auth/facebook\\?redirectUrl=\"\\.concat\\((\\w+)\\)},(?:\\w+.\\w+).createElement\\(\"div\",\\{className:\"Auth-SocialAuth-Icon-Container\"},(?:\\w+.\\w+).createElement\\((\\w+).(\\w),null\\)\\),\" Continue with Facebook\"\\),".toRegex()
        val CONTINUE_WITH_DISCORD_REPLACEMENT = { result: MatchResult ->
            "${result.value}${result.groupValues[1]}.createElement(\"a\",{className:\"Auth-SocialAuth\",href:\"auth/discord?redirectUrl=\".concat(${result.groupValues[2]})},${result.groupValues[1]}.createElement(\"div\",{className:\"Auth-SocialAuth-Icon-Container\"},${result.groupValues[1]}.createElement(${result.groupValues[3]}.${result.groupValues[4][0].dec()},null)),\" Continue with Discord\"),"
        }

//        const val FACEBOOK_SVG_PATH =
//            "({tag:\"svg\",attr:{viewBox:\"0 0 320 512\"},child:[{tag:\"path\",attr:{d:\"M279.14 288l14.22-92.66h-88.91v-60.13c0-25.35 12.42-50.06 52.24-50.06h40.42V6.26S260.43 0 225.36 0c-73.22 0-121.08 44.38-121.08 124.72v70.62H22.89V288h81.39v224h100.17V288z\"}}]})"
//        const val FACEBOOK_ICON_DISPLAY_NAME = ".displayName=\"FaFacebookF\""
//
//        const val DISCORD_SVG_PATH =
//            "({tag:\"svg\",attr:{viewBox:\"0 0 448 512\"},child:[{tag:\"path\",attr:{d:\"M297.216 243.2c0 15.616-11.52 28.416-26.112 28.416-14.336 0-26.112-12.8-26.112-28.416s11.52-28.416 26.112-28.416c14.592 0 26.112 12.8 26.112 28.416zm-119.552-28.416c-14.592 0-26.112 12.8-26.112 28.416s11.776 28.416 26.112 28.416c14.592 0 26.112-12.8 26.112-28.416.256-15.616-11.52-28.416-26.112-28.416zM448 52.736V512c-64.494-56.994-43.868-38.128-118.784-107.776l13.568 47.36H52.48C23.552 451.584 0 428.032 0 398.848V52.736C0 23.552 23.552 0 52.48 0h343.04C424.448 0 448 23.552 448 52.736zm-72.96 242.688c0-82.432-36.864-149.248-36.864-149.248-36.864-27.648-71.936-26.88-71.936-26.88l-3.584 4.096c43.52 13.312 63.744 32.512 63.744 32.512-60.811-33.329-132.244-33.335-191.232-7.424-9.472 4.352-15.104 7.424-15.104 7.424s21.248-20.224 67.328-33.536l-2.56-3.072s-35.072-.768-71.936 26.88c0 0-36.864 66.816-36.864 149.248 0 0 21.504 37.12 78.08 38.912 0 0 9.472-11.52 17.152-21.248-32.512-9.728-44.8-30.208-44.8-30.208 3.766 2.636 9.976 6.053 10.496 6.4 43.21 24.198 104.588 32.126 159.744 8.96 8.96-3.328 18.944-8.192 29.44-15.104 0 0-12.8 20.992-46.336 30.464 7.68 9.728 16.896 20.736 16.896 20.736 56.576-1.792 78.336-38.912 78.336-38.912z\"}}]})"
//        const val DISCORD_ICON_DISPLAY_NAME = ".displayName=\"FaDiscord\""
    }

    inline val http get() = blasement.httpClient

    @Serializable
    data class ChroniclerSiteDataWrapper(val nextPage: String? = null, val data: List<ChroniclerSiteData>? = null)

    @Serializable
    data class ChroniclerSiteData(val timestamp: @Serializable(BlaseballDateTimeSerialiser::class) DateTimeTz, val path: String, val hash: String, val size: Long, val downloadUrl: String)

    private val _indexHtml: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())
    private val _mainCss: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())
    private val _mainJs: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())
    private val _twoJs: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())

    var siteDataJob: Job? = null
    val cacheDir = File(File("cache"), base).also { it.mkdirs() }

    suspend fun downloadData(data: ChroniclerSiteData) =
        http.getAsResult<HttpResponse>("https://blasement-b2.undermybrella.workers.dev${data.downloadUrl}")
            .map { it.readAllBytes() }
            .switchIfFailure {
                http.getAsResult<HttpResponse>("https://api.sibr.dev/chronicler/v1${data.downloadUrl}") {
//                            timeout {
//                                socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                                connectTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                            }
                }.map { response ->
                    val bytes = response.readAllBytes()
                    blasement.b2?.uploadData(bytes, response.contentType() ?: ContentType.parse("b2/x-auto"), data.downloadUrl)
                    bytes
                }
            }

    fun launch(scope: CoroutineScope, context: CoroutineContext = scope.coroutineContext) {
        siteDataJob?.cancel()

        siteDataJob = scope.launch(context) {
            var after: String? = null
            while (isActive) {
                val now = BLASEBALL_TIME_PATTERN.format(source.now())
                val results = http.getAsResult<ChroniclerSiteDataWrapper>("https://api.sibr.dev/chronicler/v1/site/updates") {
                    parameter("order", "desc")
                    parameter("count", 50)
                    parameter("before", now)
                    if (after != null) parameter("after", after)
                }.doOnFailure { println("Site retrieval failed: $it") }.getOrNull()?.data

                if (results == null) {
                    delay(500)
                    continue
                }

                if (results.isEmpty()) {
                    delay(60_000)
                    continue
                }


                after = now

                var newIndexHtml: Pair<ChroniclerSiteData, ByteArray>? = null
                var new2Js: Pair<ChroniclerSiteData, ByteArray>? = null
                var newMainJs: Pair<ChroniclerSiteData, ByteArray>? = null
                var newMainCss: Pair<ChroniclerSiteData, ByteArray>? = null

                for (data in results) {
                    if (newIndexHtml == null && data.path.matches(INDEX_HTML_REGEX)) {
                        val cacheFile = File(cacheDir, "${data.hash}.html")
                        if (cacheFile.exists()) {
                            newIndexHtml = withContext(Dispatchers.IO) { data to cacheFile.readBytes() }
                        } else {
                            downloadData(data).doOnSuccess { bytes ->
                                withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                                newIndexHtml = data to bytes
                            }
                        }
                    } else if (new2Js == null && data.path.matches(TWO_CHUNK_JS_REGEX)) {
                        val cacheFile = File(cacheDir, "${data.hash}.js")
                        if (cacheFile.exists()) {
                            new2Js = withContext(Dispatchers.IO) { data to cacheFile.readBytes() }
                        } else {
                            downloadData(data).doOnSuccess { bytes ->
                                withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                                new2Js = data to bytes
                            }
                        }
                    } else if (newMainJs == null && data.path.matches(MAIN_CHUNK_JS_REGEX)) {
                        val cacheFile = File(cacheDir, "${data.hash}.js")
                        if (cacheFile.exists()) {
                            newMainJs = withContext(Dispatchers.IO) { data to cacheFile.readBytes() }
                        } else {
                            downloadData(data).doOnSuccess { bytes ->
                                withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                                newMainJs = data to bytes
                            }
                        }
                    } else if (newMainCss == null && data.path.matches(MAIN_CHUNK_CSS_REGEX)) {
                        val cacheFile = File(cacheDir, "${data.hash}.css")
                        if (cacheFile.exists()) {
                            newMainCss = withContext(Dispatchers.IO) { data to cacheFile.readBytes() }
                        } else {
                            downloadData(data).doOnSuccess { bytes ->
                                withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                                newMainCss = data to bytes
                            }
                        }
                    }

                    if (newIndexHtml != null && newMainCss != null && newMainJs != null && new2Js != null) break
                }

                newIndexHtml?.let { (_, response) ->
                    _indexHtml.value = response.decodeToString()
                        .replace("\"https://d35iw2jmbg6ut8.cloudfront.net/static/js/main\\..+\\.chunk\\.js\"".toRegex(), "\"$base/main.js\"")
                        .replace("\"https://d35iw2jmbg6ut8.cloudfront.net/static/js/2\\..+\\.chunk\\.js\"".toRegex(), "\"$base/2.js\"")
                        .replace("\"https://d35iw2jmbg6ut8.cloudfront.net/static/css/main\\..+\\.chunk\\.css\"".toRegex(), "\"$base/main.css\"")
                        .encodeToByteArray()
//                        .compressGz()
                }

                new2Js?.let { (_, response) ->
                    _twoJs.value = response
//                        .compressGz()
                }

                newMainJs?.let { (_, response) ->
                    _mainJs.value = response.decodeToString()
                        .replace("\"/", "\"$base/")
                        .replace(CONTINUE_WITH_FACEBOOK_REGEX, CONTINUE_WITH_DISCORD_REPLACEMENT)
                        .replace("auth/facebook", "auth/discord")
                        .replace("new Date()", "time()")
                        .replace("new Date([^(])".toRegex()) { match -> "time()${match.groupValues[1]}" }
                        //Event Source
//                        .plus(";let source=new EventSource(\"$base/api/time/sse\");let currentTime = new Date();source.onmessage=function(event){currentTime=new Date(event.data);console.log(new Date(event.data));};function time(){return currentTime;}")
                        //WebSocket
                        .plus(";let loc=window.location,new_uri;const source=new WebSocket((loc.protocol === \"https:\"?\"wss://\":\"ws://\")+loc.host+\"$base/api/time\");source.addEventListener('message',function(event){window.blasementTime=event.data});function time(){return window.blasementTime?new Date(window.blasementTime):new Date();}")
                        .encodeToByteArray()
//                        .compressGz()
                }

                newMainCss?.let { (_, response) ->
                    _mainCss.value = response
                }
            }
        }
    }

    suspend fun respondIndexHtml(call: ApplicationCall) {
        val data = _indexHtml.first(ByteArray::isNotEmpty)

//        call.response.header(HttpHeaders.ContentEncoding, "gzip")
        call.respondBytes(data, contentType = ContentType.Text.Html)
    }

    suspend fun respondMainCss(call: ApplicationCall) {
        val data = _mainCss.first(ByteArray::isNotEmpty)

//        call.response.header(HttpHeaders.ContentEncoding, "gzip")
        call.respondBytes(data, contentType = ContentType.Text.CSS)
    }

    suspend fun respondMainJs(call: ApplicationCall) {
        val data = _mainJs.first(ByteArray::isNotEmpty)

//        call.response.header(HttpHeaders.ContentEncoding, "gzip")
        call.respondBytes(data, contentType = ContentType.Application.JavaScript)
    }

    suspend fun respond2Js(call: ApplicationCall) {
        val data = _twoJs.first(ByteArray::isNotEmpty)

//        call.response.header(HttpHeaders.ContentEncoding, "gzip")
        call.respondBytes(data, contentType = ContentType.Application.JavaScript)
    }
}