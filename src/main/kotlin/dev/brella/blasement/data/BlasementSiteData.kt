package dev.brella.blasement.data

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.response.*
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
import kotlinx.datetime.Instant
import kotlin.coroutines.CoroutineContext

import kotlinx.serialization.Serializable
import java.io.File

class BlasementSiteData(
    val http: HttpClient,
    val indexHtmlTransformers: List<SiteTransformer>,
    val mainJsTransformers: List<SiteTransformer>,
    val twoJsTransformers: List<SiteTransformer>,
    val mainCssTransformers: List<SiteTransformer>,
    val timeSource: suspend () -> Instant
) {
    companion object : CoroutineScope {
        override val coroutineContext: CoroutineContext = SupervisorJob()

        val INDEX_HTML_REGEX = "/".toRegex()
        val TWO_CHUNK_JS_REGEX = ".*/2.*.js".toRegex()
        val MAIN_CHUNK_JS_REGEX = ".*/main.*js".toRegex()
        val MAIN_CHUNK_CSS_REGEX = ".*/main.*css".toRegex()

        //o.a.createElement("a",{className:"Auth-SocialAuth",href:"auth/facebook?redirectUrl=".concat(g)},o.a.createElement("div",{className:"Auth-SocialAuth-Icon-Container"},o.a.createElement(pu.e,null))," Continue with Facebook")

//        const val FACEBOOK_SVG_PATH =
//            "({tag:\"svg\",attr:{viewBox:\"0 0 320 512\"},child:[{tag:\"path\",attr:{d:\"M279.14 288l14.22-92.66h-88.91v-60.13c0-25.35 12.42-50.06 52.24-50.06h40.42V6.26S260.43 0 225.36 0c-73.22 0-121.08 44.38-121.08 124.72v70.62H22.89V288h81.39v224h100.17V288z\"}}]})"
//        const val FACEBOOK_ICON_DISPLAY_NAME = ".displayName=\"FaFacebookF\""
//
//        const val DISCORD_SVG_PATH =
//            "({tag:\"svg\",attr:{viewBox:\"0 0 448 512\"},child:[{tag:\"path\",attr:{d:\"M297.216 243.2c0 15.616-11.52 28.416-26.112 28.416-14.336 0-26.112-12.8-26.112-28.416s11.52-28.416 26.112-28.416c14.592 0 26.112 12.8 26.112 28.416zm-119.552-28.416c-14.592 0-26.112 12.8-26.112 28.416s11.776 28.416 26.112 28.416c14.592 0 26.112-12.8 26.112-28.416.256-15.616-11.52-28.416-26.112-28.416zM448 52.736V512c-64.494-56.994-43.868-38.128-118.784-107.776l13.568 47.36H52.48C23.552 451.584 0 428.032 0 398.848V52.736C0 23.552 23.552 0 52.48 0h343.04C424.448 0 448 23.552 448 52.736zm-72.96 242.688c0-82.432-36.864-149.248-36.864-149.248-36.864-27.648-71.936-26.88-71.936-26.88l-3.584 4.096c43.52 13.312 63.744 32.512 63.744 32.512-60.811-33.329-132.244-33.335-191.232-7.424-9.472 4.352-15.104 7.424-15.104 7.424s21.248-20.224 67.328-33.536l-2.56-3.072s-35.072-.768-71.936 26.88c0 0-36.864 66.816-36.864 149.248 0 0 21.504 37.12 78.08 38.912 0 0 9.472-11.52 17.152-21.248-32.512-9.728-44.8-30.208-44.8-30.208 3.766 2.636 9.976 6.053 10.496 6.4 43.21 24.198 104.588 32.126 159.744 8.96 8.96-3.328 18.944-8.192 29.44-15.104 0 0-12.8 20.992-46.336 30.464 7.68 9.728 16.896 20.736 16.896 20.736 56.576-1.792 78.336-38.912 78.336-38.912z\"}}]})"
//        const val DISCORD_ICON_DISPLAY_NAME = ".displayName=\"FaDiscord\""

        const val SNACKS_LIST_REGEX = "\\{e\\[e.AD=0\\]=\"AD\"(,e\\[e.\\w+=\\d+\\]=\\\"\\w+\\\")+\\}"
    }

    @Serializable
    data class ChroniclerSiteDataWrapper(val nextPage: String? = null, val data: List<ChroniclerSiteData>? = null)

    @Serializable
    data class ChroniclerSiteData(val timestamp: Instant, val path: String, val hash: String, val size: Long, val downloadUrl: String)

    private val _indexHtml: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())
    private val _mainCss: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())
    private val _mainJs: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())
    private val _twoJs: MutableStateFlow<ByteArray> = MutableStateFlow(byteArrayOf())

    var siteDataJob: Job? = null
    val cacheDir = File("cache").also { it.mkdirs() }

    suspend fun downloadData(data: ChroniclerSiteData) =
        try {
            http.get<ByteArray>("https://api.sibr.dev/chronicler/v1${data.downloadUrl}") {
                timeout {
                    socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                }
            }
        } catch (th: Throwable) {
            th.printStackTrace()
            null
        }

    fun transformWith(steps: List<SiteTransformer>, data: ByteArray): ByteArray {
        val initialBinary = steps.fold(data) { data, transformer ->
            if (transformer is SiteTransformer.InitialBinaryTransformer) transformer.transform(data) ?: data
            else data
        }

        val initialText = steps.fold(initialBinary.decodeToString()) { data, transformer ->
            if (transformer is SiteTransformer.InitialTextTransformer) transformer.transform(data) ?: data
            else data
        }

        val finalText = steps.fold(initialText) { data, transformer ->
            if (transformer is SiteTransformer.FinalTextTransformer) transformer.transform(data) ?: data
            else data
        }

        return steps.fold(finalText.encodeToByteArray()) { data, transformer ->
            if (transformer is SiteTransformer.FinalBinaryTransformer) transformer.transform(data) ?: data
            else data
        }
    }

    fun launch(scope: CoroutineScope, context: CoroutineContext = scope.coroutineContext) {
        siteDataJob?.cancel()

        siteDataJob = scope.launch(context) {
            var after: Instant? = null

            loop@ while (isActive) {
                val now = timeSource()
                val results = http.get<ChroniclerSiteDataWrapper>("https://api.sibr.dev/chronicler/v1/site/updates") {
                    parameter("order", "desc")
                    parameter("count", 50)
                    parameter("before", now)
                    if (after != null) parameter("after", after)
                }.data

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
                            val bytes = downloadData(data) ?: continue@loop
                            withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                            newIndexHtml = data to bytes
                        }
                    } else if (new2Js == null && data.path.matches(TWO_CHUNK_JS_REGEX)) {
                        val cacheFile = File(cacheDir, "${data.hash}.js")
                        if (cacheFile.exists()) {
                            new2Js = withContext(Dispatchers.IO) { data to cacheFile.readBytes() }
                        } else {
                            val bytes = downloadData(data) ?: continue@loop
                            withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                            new2Js = data to bytes
                        }
                    } else if (newMainJs == null && data.path.matches(MAIN_CHUNK_JS_REGEX)) {
                        val cacheFile = File(cacheDir, "${data.hash}.js")
                        if (cacheFile.exists()) {
                            newMainJs = withContext(Dispatchers.IO) { data to cacheFile.readBytes() }
                        } else {
                            val bytes = downloadData(data) ?: continue@loop
                            withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                            newMainJs = data to bytes
                        }
                    } else if (newMainCss == null && data.path.matches(MAIN_CHUNK_CSS_REGEX)) {
                        val cacheFile = File(cacheDir, "${data.hash}.css")
                        if (cacheFile.exists()) {
                            newMainCss = withContext(Dispatchers.IO) { data to cacheFile.readBytes() }
                        } else {
                            val bytes = downloadData(data) ?: continue@loop
                            withContext(Dispatchers.IO) { cacheFile.writeBytes(bytes) }

                            newMainCss = data to bytes
                        }
                    }

                    if (newIndexHtml != null && newMainCss != null && newMainJs != null && new2Js != null) break
                }

                newIndexHtml?.let { (_, response) ->
                    _indexHtml.value = transformWith(indexHtmlTransformers, response)
                }

                new2Js?.let { (_, response) ->
                    _twoJs.value = transformWith(twoJsTransformers, response)
                }

                newMainJs?.let { (_, response) ->
                    _mainJs.value = transformWith(mainJsTransformers, response)
                }

                newMainCss?.let { (_, response) ->
                    _mainCss.value = transformWith(mainCssTransformers, response)
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