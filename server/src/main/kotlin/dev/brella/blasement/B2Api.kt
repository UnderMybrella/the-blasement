package dev.brella.blasement

import dev.brella.kornea.toolkit.coroutines.ReadWriteSemaphore
import dev.brella.kornea.toolkit.coroutines.withReadPermit
import dev.brella.kornea.toolkit.coroutines.withWritePermit
import getString
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.features.compression.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.bouncycastle.util.encoders.Hex
import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow
import kotlin.math.roundToLong

class B2Api(val b2Permits: Int?, val bucketID: String, val applicationKeyId: String, val applicationKey: String) : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob()

    private val client = HttpClient {
        install(ContentEncoding) {
            gzip()
            deflate()
            identity()
        }

        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }

        followRedirects = false
    }

    private var apiUrl: String? = null
    private var authorizationToken: String? = null
    private var delayJob: Job? = null
    private val tokenSemaphore = ReadWriteSemaphore(b2Permits ?: 50)
    private val waitingOnToken = ConcurrentLinkedQueue<Continuation<Unit>>()

    private val logger = LoggerFactory.getLogger("B2")

    private var tokenJob = launch {
        val basicAuth = Base64.getEncoder().encodeToString("$applicationKeyId:$applicationKey".encodeToByteArray())

        while (isActive) {
            try {
                yield()

                val json = tokenSemaphore.withWritePermit {
                    val json = client.get<JsonObject>("https://api.backblazeb2.com/b2api/v2/b2_authorize_account") {
                        header("Authorization", "Basic $basicAuth")
                    }

                    apiUrl = json["apiUrl"]?.jsonPrimitive?.content ?: return@withWritePermit null
                    authorizationToken = json["authorizationToken"]?.jsonPrimitive?.content ?: return@withWritePermit null

                    json
                } ?: continue

                val expiresIn = json["expires_in"]?.jsonPrimitive?.longOrNull?.times(1_000) ?: 3_600_000L

                logger.debug("[B2] Received json: ${Json.encodeToString(json).replace(authorizationToken ?: "", "hunter2")}")
                logger.debug("[B2] Expires in $expiresIn ms")


                //"apiUrl": "https://apiNNN.backblazeb2.com",
                //  "authorizationToken": "4_0022623512fc8f80000000001_0186e431_d18d02_acct_tH7VW03boebOXayIc43-sxptpfA=",
                //  "downloadUrl": "https://f002.backblazeb2.com"

                delayJob = launch { delay(expiresIn - 10_000L) }
                delayJob?.join() ?: delay(1_000)
            } catch (th: Throwable) {
                th.printStackTrace()

                delay(5_000)
            }
        }
    }

    suspend fun uploadData(data: ByteArray, contentType: ContentType, name: String) {
        val uploadJson = getUploadUrl()

        var errors: Throwable? = null
        var authFailed = false

        var uploadUrl = uploadJson.getString("uploadUrl")
        var authorizationToken = uploadJson.getString("authorizationToken")

        for (i in 0 until 4) {
            logger.debug("Upload request/$i")

            try {
                val response = client.post<HttpResponse>(uploadUrl) {
                    header("Authorization", authorizationToken)
                    header("X-Bz-File-Name", name)
                    header("X-Bz-Content-Sha1", MessageDigest.getInstance("SHA-1").digest(data).let(Hex::toHexString))

                    body = ByteArrayContent(data, contentType)
                }

                if (response.status.isSuccess()) {
                    return
                } else {
                    when (response.status) {
                        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                            logger.debug("Auth failed w/ upload url, trying again...")
                            val th = IllegalStateException("Auth failed w/ upload url, trying again...")

                            errors =
                                if (errors == null) th
                                else th.initCause(errors)

                            getUploadUrl().let { uploadJson ->
                                uploadUrl = uploadJson.getString("uploadUrl")
                                authorizationToken = uploadJson.getString("authorizationToken")
                            }

                            continue
                        }
                        HttpStatusCode.BadRequest -> {
                            launch(Dispatchers.IO) {
                                val parent = File("b2/upload/bad_request/")
                                val file = File(parent, buildString {
                                    append(System.currentTimeMillis())
                                    append(".json")
                                })

                                logger.debug("Received a bad request from B2; delaying then trying again (Response written to ${file.name})")

                                parent.mkdirs()
                                file.writeText(response.receive())
                            }

                            delay(2.0.pow(i).roundToLong() * 1_000)
                        }
                        HttpStatusCode.InternalServerError, HttpStatusCode.BadGateway, HttpStatusCode.ServiceUnavailable, HttpStatusCode.GatewayTimeout -> {
                            logger.debug("B2 timed out; delaying then trying again")

                            delay(2.0.pow(i).roundToLong() * 1_000)
                        }

                        else -> {
                            launch(Dispatchers.IO) {
                                val parent = File("b2/upload/${response.status.value}/")
                                val file = File(parent, buildString {
                                    append(System.currentTimeMillis())
                                    append(".json")
                                })

                                logger.debug("Don't know how to handle ${response.status}; delaying then trying again (Response written to ${file.name})")

                                parent.mkdirs()
                                file.writeText(response.receive())
                            }
                        }
                    }
                }
            } catch (th: Throwable) {
                th.printStackTrace()

                errors =
                    if (errors == null) th
                    else th.initCause(errors)
            }
        }

        throw errors!!
    }

    suspend fun getUploadUrl(): JsonObject {
//        if (authToken != globalAuth) return null

        var errors: Throwable? = null
        var authFailed = false

        for (i in 0 until 4) {
            logger.debug("Upload Url request/$i")

            try {
                var delayJob: Job? = null
                val response = tokenSemaphore.withReadPermit {
                    delayJob = this.delayJob
                    client.post<HttpResponse>("$apiUrl/b2api/v2/b2_get_upload_url") {
                        header("Authorization", authorizationToken)
                        header("Content-Type", "application/json")

                        body = mapOf("bucketId" to bucketID)

                        expectSuccess = false
                    }
                }

                if (response.status.isSuccess()) {
                    return response.receive<JsonObject>()
                } else {
                    when (response.status) {
                        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                            if (authFailed) {
                                logger.debug("Auth already failed, we might need a new token")
                                val th = IllegalStateException("Authorization for B2 token failed, obtain new token?")

                                errors =
                                    if (errors == null) th
                                    else th.initCause(errors)

                                continue
                            } else {
                                logger.debug("Token failed, let's give it another shot!")
                                //Authorization failed, let's try again after we get our token working
                                authFailed = true

                                suspendCancellableCoroutine<Unit> { cont ->
                                    waitingOnToken.add(cont)
                                    delayJob?.cancel()
                                }
                            }
                        }
                        HttpStatusCode.BadRequest -> {
                            launch(Dispatchers.IO) {
                                val parent = File("b2/get_upload_url/bad_request/")
                                val file = File(parent, buildString {
                                    append(System.currentTimeMillis())
                                    append(".json")
                                })

                                logger.debug("Received a bad request from B2; delaying then trying again (Response written to ${file.name})")

                                parent.mkdirs()
                                file.writeText(response.receive())
                            }

                            delay(2.0.pow(i).roundToLong() * 1_000)
                        }
                        HttpStatusCode.InternalServerError, HttpStatusCode.BadGateway, HttpStatusCode.ServiceUnavailable, HttpStatusCode.GatewayTimeout -> {
                            logger.debug("B2 timed out; delaying then trying again")

                            delay(2.0.pow(i).roundToLong() * 1_000)
                        }

                        else -> {
                            launch(Dispatchers.IO) {
                                val parent = File("b2/get_upload_url/${response.status.value}/")
                                val file = File(parent, buildString {
                                    append(System.currentTimeMillis())
                                    append(".json")
                                })

                                logger.debug("Don't know how to handle ${response.status}; delaying then trying again (Response written to ${file.name})")

                                parent.mkdirs()
                                file.writeText(response.receive())
                            }
                        }
                    }
                }
            } catch (th: Throwable) {
                th.printStackTrace()

                errors =
                    if (errors == null) th
                    else th.initCause(errors)
            }
        }

        throw errors!!
    }
}