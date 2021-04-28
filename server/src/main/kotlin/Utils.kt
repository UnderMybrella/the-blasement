import dev.brella.blasement.OutgoingReadChannel
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
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.awaitSingleOrNull
import kotlinx.serialization.json.*
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.reflect.jvm.jvmName

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