package dev.brella.blasement.utils

import CHRONICLER_HOST
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.parse
import dev.brella.blasement.common.getJsonArray
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.json.BlaseballDateTimeSerialiser
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.flatMap
import dev.brella.ktornea.common.getAsResult
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@Serializable
data class ChroniclerV2Data<T>(val nextPage: String? = null, val items: List<ChroniclerV2Item<T>>)
@Serializable
data class ChroniclerV2Item<T>(val entityId: String, val hash: String, val validFrom: @Serializable(BlaseballDateTimeSerialiser::class) DateTimeTz?, val validTo: @Serializable(BlaseballDateTimeSerialiser::class) DateTimeTz?, val data: T)

suspend inline fun <reified T> HttpClient.chroniclerEntity(type: String, at: String, builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<T> =
    getAsResult<ChroniclerV2Data<T>>("$CHRONICLER_HOST/v2/entities") {
        parameter("type", type)
        parameter("at", at)

        builder()
    }.flatMap { json ->
        try {
            KorneaResult.successOrEmpty(json.items.firstOrNull()?.data)
        } catch (th: Throwable) {
            KorneaResult.thrown(th)
        }
    }

suspend inline fun <reified T> HttpClient.chroniclerEntityList(type: String, at: String, builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<List<T>> =
    getAsResult<ChroniclerV2Data<T>>("$CHRONICLER_HOST/v2/entities") {
        parameter("type", type)
        parameter("at", at)

        builder()
    }.flatMap { json ->
        try {
            KorneaResult.successOrEmpty(json.items.map(ChroniclerV2Item<T>::data))
        } catch (th: Throwable) {
            KorneaResult.thrown(th)
        }
    }

@OptIn(ExperimentalTime::class)
suspend inline fun <reified T> HttpClient.chroniclerEntityPagination(type: String, at: String, crossinline wait: suspend (time: DateTimeTz) -> Unit, delay: Duration = Duration.seconds(5), limit: Int = 5, maximumDelay: Duration = Duration.seconds(5)): KorneaResult<Flow<T>> =
    getAsResult<ChroniclerV2Data<T>>("$CHRONICLER_HOST/v2/entities") {
        parameter("type", type)
        parameter("at", at)
    }.flatMap { json ->
        try {
            KorneaResult.successOrEmpty(
                json.items
                    .firstOrNull()
                    ?.let {
                        flow<T> {
                            var element = it

                            repeat(limit) {
                                emit(element.data)

                                element.validTo?.let { validTo ->
                                    wait(validTo)

                                    getAsResult<ChroniclerV2Data<T>>("https://api.sibr.dev/chronicler/v2/entities") {
                                        parameter("type", type)
                                        parameter("at", validTo)
                                    }.doOnSuccess { response ->
                                        response.items.firstOrNull()?.let { element = it }
                                    }
                                }
                            }
                        }
                    }

            )
        } catch (th: Throwable) {
            KorneaResult.thrown(th)
        }
    }
