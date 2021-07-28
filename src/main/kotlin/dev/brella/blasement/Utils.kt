package dev.brella.blasement

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.response.*
import io.r2dbc.spi.Row
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

inline fun property(key: String) = System.getProperty(key) ?: System.getenv(key)

public suspend inline fun HttpClient.getChroniclerVersionsBefore(type: String, at: Instant, builder: HttpRequestBuilder.() -> Unit = {}) =
    getChroniclerVersionsBefore(type, at.toString(), builder)

public suspend inline fun HttpClient.getChroniclerVersionsBefore(type: String, at: String, builder: HttpRequestBuilder.() -> Unit = {}) =
    (get<JsonObject>("https://api.sibr.dev/chronicler/v2/versions") {
        parameter("type", type)
        parameter("before", at)
        parameter("order", "desc")
        parameter("count", 50)

        builder()
    }["items"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.getJsonObjectOrNull("data") }

public suspend inline fun HttpClient.getChroniclerEntity(type: String, at: Instant, builder: HttpRequestBuilder.() -> Unit = {}) =
    getChroniclerEntity(type, at.toString(), builder)

public suspend inline fun HttpClient.getChroniclerEntity(type: String, at: String, builder: HttpRequestBuilder.() -> Unit = {}) =
    ((get<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
        parameter("type", type)
        parameter("at", at)

        builder()
    }["items"] as? JsonArray)?.firstOrNull() as? JsonObject)?.get("data")

public suspend inline fun HttpClient.getChroniclerEntityList(type: String, at: Instant, builder: HttpRequestBuilder.() -> Unit = {}) =
    getChroniclerEntityList(type, at.toString(), builder)

public suspend inline fun HttpClient.getChroniclerEntityList(type: String, at: String, builder: HttpRequestBuilder.() -> Unit = {}) =
    (get<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
        parameter("type", type)
        parameter("at", at)

        builder()
    }["items"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.get("data") }

public suspend inline fun ApplicationCall.respondJsonObject(statusCode: HttpStatusCode = HttpStatusCode.OK, crossinline builder: JsonObjectBuilder.() -> Unit) =
    respondText(ContentType.Application.Json, statusCode) { buildJsonObject(builder).toString() }

@OptIn(ExperimentalTime::class)
suspend fun <T> T.loopEvery(time: Duration, `while`: suspend T.() -> Boolean, block: suspend () -> Unit) {
    while (`while`()) {
        val timeTaken = measureTime {
            try {
                block()
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
//        println("Took ${timeTaken.inSeconds}s, waiting ${(time - timeTaken).inSeconds}s")
        delay((time - timeTaken).toLongMilliseconds().coerceAtLeast(0L))
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

public inline fun JsonObject.getInt(key: String) =
    getValue(key).jsonPrimitive.int

public inline fun JsonObject.getLong(key: String) =
    getValue(key).jsonPrimitive.long


public inline fun JsonObject.getJsonObjectOrNull(key: String) =
    get(key) as? JsonObject

public inline fun JsonObject.getJsonArrayOrNull(key: String) =
    get(key) as? JsonArray

public inline fun JsonObject.getJsonPrimitiveOrNull(key: String) =
    (get(key) as? JsonPrimitive)

public inline fun JsonObject.getStringOrNull(key: String) =
    (get(key) as? JsonPrimitive)?.contentOrNull

public inline fun JsonObject.getIntOrNull(key: String) =
    (get(key) as? JsonPrimitive)?.intOrNull

public inline fun JsonObject.getLongOrNull(key: String) =
    (get(key) as? JsonPrimitive)?.longOrNull

public inline fun JsonObject.getDoubleOrNull(key: String) =
    (get(key) as? JsonPrimitive)?.doubleOrNull

public inline fun JsonObject.getDoubleOrNull(vararg keys: String) =
    keys.firstNotNullOfOrNull { (get(it) as? JsonPrimitive)?.doubleOrNull }

public inline fun JsonObject.getBooleanOrNull(key: String) =
    (get(key) as? JsonPrimitive)?.booleanOrNull

public inline fun <reified T> Row.get(name: String): T? =
    get(name, T::class.java)

public inline fun <reified T> Row.getValue(name: String): T =
    get(name, T::class.java)
