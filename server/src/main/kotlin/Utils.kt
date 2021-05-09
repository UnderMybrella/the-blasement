import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.brella.blasement.OutgoingReadChannel
import dev.brella.kornea.blaseball.base.common.BlaseballUUID
import dev.brella.kornea.blaseball.base.common.UUID
import dev.brella.kornea.blaseball.base.common.jvm
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.doOnFailure
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.ktornea.common.KorneaHttpResult
import io.ktor.application.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.r2dbc.h2.H2Connection
import io.r2dbc.h2.H2Statement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.awaitSingleOrNull
import kotlinx.serialization.json.*
import org.reactivestreams.Publisher
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.KeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.jvm.jvmName

import java.util.UUID as JUUID
import dev.brella.kornea.blaseball.base.common.UUID as KUUID

inline fun <reified T> H2Statement.bindNullable(index: Int, value: T?) =
    if (value != null) bind(index, value!!) else bindNull(index, T::class.java)

inline fun <reified T> H2Statement.bindNullable(name: String, value: T?) =
    if (value != null) bind(name, value!!) else bindNull(name, T::class.java)

public suspend inline fun <R> Publisher<H2Connection>.use(block: (connection: H2Connection) -> R): R {
    val connection = awaitSingle()

    try {
        return block(connection)
    } catch (e: Throwable) {
        e.printStackTrace()
        throw e
    } finally {
        connection.close().awaitSingleOrNull()
    }
}

public suspend inline fun <R> H2Connection.use(block: (connection: H2Connection) -> R): R {
    try {
        return block(this)
    } catch (e: Throwable) {
        e.printStackTrace()
        throw e
    } finally {
        close().awaitSingleOrNull()
    }
}

public inline fun JsonObject.getJsonObject(key: String) =
    getValue(key).jsonObject

public inline fun JsonObject.getJsonArray(key: String) =
    getValue(key).jsonArray

public inline fun JsonObject.getJsonPrimitive(key: String) =
    getValue(key).jsonPrimitive

public inline fun JsonObject.getString(key: String) =
    getValue(key).jsonPrimitive.content

public inline fun JsonObject.getLong(key: String) =
    getValue(key).jsonPrimitive.long


@ContextDsl
public fun Route.redirectInternally(from: String, to: String, pathSelector: String = "$from{...}"): Route {
    return route(pathSelector) {
        handle {
            val cp = object : RequestConnectionPoint by call.request.local {
                override val uri: String = call.request.uri.replace(from, to)
            }
            val req = object : ApplicationRequest by call.request {
                override val local: RequestConnectionPoint = cp
            }
            val call = object : ApplicationCall by call {
                override val request: ApplicationRequest = req
            }

            this.application.execute(call, Unit)
        }
    }
}

suspend fun ApplicationCall.redirectInternally(path: String) {
    val cp = object : RequestConnectionPoint by this.request.local {
        override val uri: String = path
    }
    val req = object : ApplicationRequest by this.request {
        override val local: RequestConnectionPoint = cp
    }
    val call = object : ApplicationCall by this {
        override val request: ApplicationRequest = req
    }

    this.application.execute(call, Unit)
}

suspend inline fun KorneaResult<*>.respondOnFailure(call: ApplicationCall) =
    this.doOnFailure { failure ->
        when (failure) {
            is KorneaHttpResult<*> -> {
                call.response.header("X-Call-URL", failure.response.request.url.toURI().toASCIIString())
                call.respondBytesWriter(failure.response.contentType(), failure.response.status) {
                    failure.response.content.copyTo(this)
                }
            }
            else -> {
                call.respond(HttpStatusCode.InternalServerError, buildJsonObject {
                    put("error_type", failure::class.jvmName)
                    put("error", failure.toString())
                })
            }
        }
    }

suspend inline fun <reified T : Any> KorneaResult<T>.respond(call: ApplicationCall) =
    this.doOnSuccess { call.respond(it) }
        .respondOnFailure(call)

suspend inline fun <T, reified R : Any> KorneaResult<T>.respond(call: ApplicationCall, transform: (T) -> R) =
    this.doOnSuccess { call.respond(transform(it)) }
        .respondOnFailure(call)


public suspend inline fun ApplicationCall.respondJsonObject(statusCode: HttpStatusCode = HttpStatusCode.OK, producer: JsonObjectBuilder.() -> Unit) =
    respond(statusCode, buildJsonObject(producer))

public suspend inline fun ApplicationCall.respondJsonArray(statusCode: HttpStatusCode = HttpStatusCode.OK, producer: JsonArrayBuilder.() -> Unit) =
    respond(statusCode, buildJsonArray(producer))

public inline operator fun JsonObjectBuilder.set(key: String, value: String?) =
    put(key, value)

public inline operator fun JsonObjectBuilder.set(key: String, value: Number?) =
    put(key, value)

public inline operator fun JsonObjectBuilder.set(key: String, value: Boolean?) =
    put(key, value)

public inline operator fun JsonObjectBuilder.set(key: String, value: JsonElement) =
    put(key, value)

public suspend inline fun HttpResponse.readAllBytes() =
    receive<Input>().readBytes()

public fun String.compressGz(): ByteArray {
    val baos = ByteArrayOutputStream()
    GZIPOutputStream(baos).use { gzip -> gzip.write(this.encodeToByteArray()) }
    return baos.toByteArray()
}

fun RSAPrivateKey(str: String): PrivateKey = KeyFactory.getInstance("RSA").generatePrivate(RSAPrivateKeySpec(str))

fun RSAPublicKey(str: String): PublicKey = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(str))

fun RSAPrivateKeySpec(str: String): KeySpec = RSAPrivateKeySpec(
    Base64.getDecoder().decode(
        str
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")
    )
)

fun RSAPublicKeySpec(str: String): KeySpec = RSAPublicKeySpec(
    Base64.getDecoder().decode(
        str
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s+".toRegex(), "")
    )
)

fun RSAPrivateKey(data: ByteArray): PrivateKey = KeyFactory.getInstance("RSA").generatePrivate(RSAPrivateKeySpec(data))

fun RSAPublicKey(data: ByteArray): PublicKey = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(data))

fun RSAPrivateKeySpec(data: ByteArray): KeySpec = PKCS8EncodedKeySpec(data)

fun RSAPublicKeySpec(data: ByteArray): KeySpec = X509EncodedKeySpec(data)

sealed class KorneaResponseResult: KorneaResult.Failure {
    abstract val httpResponseCode: HttpStatusCode
    abstract val contentType: ContentType?

    class UserErrorJson(override val httpResponseCode: HttpStatusCode, val json: JsonElement): KorneaResponseResult() {
        override val contentType: ContentType = ContentType.Application.Json

        override suspend fun writeTo(call: ApplicationCall) =
            call.respond(httpResponseCode, json)
    }

    override fun get(): Nothing = throw IllegalStateException("Failed Response @ $this")

    abstract suspend fun writeTo(call: ApplicationCall)
}

inline fun buildUserErrorJsonObject(httpResponseCode: HttpStatusCode = HttpStatusCode.BadRequest, builder: JsonObjectBuilder.() -> Unit) =
    KorneaResponseResult.UserErrorJson(httpResponseCode, buildJsonObject(builder))

class KorneaResultException(val result: KorneaResult<*>) : Throwable((result as? KorneaResult.WithException<*>)?.exception)

inline fun <T> KorneaResult<T>.getOrThrow(): T =
    if (this is KorneaResult.Success<T>) get()
    else throw KorneaResultException(this)

inline fun <T> KorneaResult<T>.getAsStage(): CompletionStage<T> =
    if (this is KorneaResult.Success<T>) CompletableFuture.completedStage(get())
    else CompletableFuture.failedStage(KorneaResultException(this))

inline val JUUID.kornea get() = KUUID(mostSigBits = mostSignificantBits.toULong(), leastSigBits = leastSignificantBits.toULong())


val DISPATCHER_CACHE = Caffeine.newBuilder()
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .buildAsync<Executor, CoroutineDispatcher>(Executor::asCoroutineDispatcher)

typealias KotlinCache<K, V> = Cache<K, Deferred<KorneaResult<V>>>

inline fun <K, V> Caffeine<Any, Any>.buildKotlin(): KotlinCache<K, V> = build()

suspend fun <K, V> KotlinCache<K, V>.getAsync(key: K, scope: CoroutineScope = GlobalScope, context: CoroutineContext = scope.coroutineContext, mappingFunction: suspend (key: K) -> KorneaResult<V>): KorneaResult<V> {
    try {
        val result = get(key) { k -> scope.async(context) { mappingFunction(k) } }.await()

        return result.doOnFailure { invalidate(key) }
    } catch (th: Throwable) {
        invalidate(key)
        throw th
    }
}