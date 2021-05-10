package dev.brella.blasement

import com.soywiz.klock.DateTimeTz
import compressGz
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.json.BlaseballDateTimeSerialiser
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.doOnFailure
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.getOrNull
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

class BlasementSiteData(val http: HttpClient, val source: BlasementDataSource, val base: String) {
    companion object : CoroutineScope {
        override val coroutineContext: CoroutineContext = SupervisorJob()

        val INDEX_HTML_REGEX = "/".toRegex()
        val TWO_CHUNK_JS_REGEX = ".*2.*.js".toRegex()
        val MAIN_CHUNK_JS_REGEX = ".*main.*js".toRegex()
        val MAIN_CHUNK_CSS_REGEX = ".*main.*css".toRegex()
    }

    @Serializable
    data class ChroniclerSiteDataWrapper(val nextPage: String? = null, val data: List<ChroniclerSiteData>? = null)

    @Serializable
    data class ChroniclerSiteData(val timestamp: @Serializable(BlaseballDateTimeSerialiser::class) DateTimeTz, val path: String, val hash: String, val size: Long, val downloadUrl: String)

    private val _indexHtml: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())
    private val _mainCss: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())
    private val _mainJs: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())
    private val _twoJs: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())

    var siteDataJob: Job? = null
    val cacheDir = File("cache").also { it.mkdirs() }

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
                            http.getAsResult<HttpResponse>("https://api.sibr.dev/chronicler/v1${data.downloadUrl}") {
//                            timeout {
//                                socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                                connectTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                            }
                            }.doOnSuccess {
                                val bytes = it.readAllBytes()
                                withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                                newIndexHtml = data to bytes
                            }
                        }
                    } else if (new2Js == null && data.path.matches(TWO_CHUNK_JS_REGEX)) {
                        val cacheFile = File(cacheDir, "${data.hash}.js")
                        if (cacheFile.exists()) {
                            new2Js = withContext(Dispatchers.IO) { data to cacheFile.readBytes() }
                        } else {
                            http.getAsResult<HttpResponse>("https://api.sibr.dev/chronicler/v1${data.downloadUrl}") {
//                            timeout {
//                                socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                                connectTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                            }
                            }.doOnSuccess {
                                val bytes = it.readAllBytes()
                                withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                                new2Js = data to bytes
                            }
                        }
                    } else if (newMainJs == null && data.path.matches(MAIN_CHUNK_JS_REGEX)) {
                        val cacheFile = File(cacheDir, "${data.hash}.js")
                        if (cacheFile.exists()) {
                            newMainJs = withContext(Dispatchers.IO) { data to cacheFile.readBytes() }
                        } else {
                            http.getAsResult<HttpResponse>("https://api.sibr.dev/chronicler/v1${data.downloadUrl}") {
//                            timeout {
//                                socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                                connectTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                            }
                            }.doOnSuccess {
                                val bytes = it.readAllBytes()
                                withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                                newMainJs = data to bytes
                            }
                        }
                    } else if (newMainCss == null && data.path.matches(MAIN_CHUNK_CSS_REGEX)) {
                        val cacheFile = File(cacheDir, "${data.hash}.css")
                        if (cacheFile.exists()) {
                            newMainCss = withContext(Dispatchers.IO) { data to cacheFile.readBytes() }
                        } else {
                            http.getAsResult<HttpResponse>("https://api.sibr.dev/chronicler/v1${data.downloadUrl}") {
//                            timeout {
//                                socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                                connectTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//                            }
                            }.doOnSuccess {
                                val bytes = it.readAllBytes()
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
//                        .replace("new Date()", "time()")
//                        .replace("new Date([^(])".toRegex()) { match -> "time()${match.groupValues[1]}" }
//                        .plus(";let source=new EventSource(\"$base/api/time/sse\");let currentTime = new Date();source.onmessage=function(event){currentTime=new Date(event.data);console.log(new Date(event.data));};function time(){return currentTime;}")
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