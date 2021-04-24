package dev.brella.blasement

import com.soywiz.klock.parse
import dev.brella.blasement.common.events.TimeRange
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.beans.BlaseballGlobalEvent
import dev.brella.kornea.blaseball.base.common.beans.BlaseballIdols
import dev.brella.kornea.blaseball.base.common.beans.BlaseballSimulationData
import dev.brella.kornea.blaseball.base.common.beans.BlaseballStreamDataResponse
import dev.brella.kornea.blaseball.base.common.beans.BlaseballTribute
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.doOnFailure
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.flatMap
import dev.brella.kornea.errors.common.map
import dev.brella.ktornea.common.getAsResult
import dev.brella.ktornea.common.streamAsResult
import getJsonArray
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import redirectInternally
import respond
import respondOnFailure
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

interface IBlaseballDataSource {
    suspend fun getIdolBoard(): KorneaResult<BlaseballIdols>
    suspend fun getHallOfFlamePlayers(): KorneaResult<List<BlaseballTribute>>

    suspend fun getBloodTypes(bloodIDs: Iterable<String>): KorneaResult<List<String>>
    suspend fun getCoffeePreferences(coffeeIDs: Iterable<String>): KorneaResult<List<String>>

    suspend fun getGlobalEvents(): KorneaResult<List<BlaseballGlobalEvent>>
    suspend fun getSimulationData(): KorneaResult<BlaseballSimulationData>

    suspend fun getLiveDataStream(): KorneaResult<Flow<String>>
}

class IBlaseballChroniclerDataSource(val http: HttpClient, val blaseball: BlaseballApi, val format: Json, val from: String, val to: String) : IBlaseballDataSource {
    companion object {
        fun collections(http: HttpClient, blaseball: BlaseballApi, json: Json) =
            IBlaseballChroniclerDataSource(http, blaseball, json, "2021-04-19T00:00:00Z", "2021-04-26T00:00:00Z")

        fun massProduction(http: HttpClient, blaseball: BlaseballApi, json: Json) =
            IBlaseballChroniclerDataSource(http, blaseball, json, "2021-04-12T00:00:00Z", "2021-04-19T00:00:00Z")

        fun liveBait(http: HttpClient, blaseball: BlaseballApi, json: Json) =
            IBlaseballChroniclerDataSource(http, blaseball, json, "2021-04-05T00:00:00Z", "2021-04-12T00:00:00Z")
    }

    val validRange = TimeRange.fromChronicler(from, to)

    suspend inline fun <reified T> chroniclerV2(type: String, at: String = to): KorneaResult<T> =
        http.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
            parameter("type", type)
            parameter("at", at)
        }.flatMap { json ->
            try {
                KorneaResult.successOrEmpty(
                    json.getJsonArray("items")
                        .firstOrNull()
                        ?.jsonObject
                        ?.get("data")
                        ?.let { this.format.decodeFromString(this.format.encodeToString(it)) }
                )
            } catch (th: Throwable) {
                KorneaResult.thrown(th)
            }
        }

    @OptIn(ExperimentalTime::class)
    suspend inline fun <reified T> chroniclerV2Pagination(type: String, at: String, delay: Duration = 5.seconds, limit: Int = Int.MAX_VALUE): KorneaResult<Flow<T>> =
        http.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
            parameter("type", type)
            parameter("at", at)
        }.flatMap { json ->
            try {
                KorneaResult.successOrEmpty(
                    json.getJsonArray("items")
                        .firstOrNull()
                        ?.jsonObject
                        ?.let { baseJson ->
                            flow<T> {
                                var json = baseJson

                                repeat(limit) {
                                    emit(format.decodeFromString(format.encodeToString(json.getValue("data"))))

                                    val validTo = json["validTo"]?.jsonPrimitive?.contentOrNull

                                    if (validTo != null && BLASEBALL_TIME_PATTERN.parse(validTo) in validRange) {
                                        http.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
                                            parameter("type", type)
                                            parameter("at", validTo)
                                        }.doOnSuccess { response ->
                                            response.getJsonArray("items")
                                                .firstOrNull()?.jsonObject
                                                ?.let { json = it }

                                            delay(delay.toLongMilliseconds())
                                        }.doOnFailure { delay((delay / 10).toLongMilliseconds()) }
                                    } else {
                                        return@flow
                                    }
                                }
                            }
                        }

                )
            } catch (th: Throwable) {
                KorneaResult.thrown(th)
            }
        }

    override suspend fun getIdolBoard(): KorneaResult<BlaseballIdols> =
        chroniclerV2("idols")

    override suspend fun getHallOfFlamePlayers(): KorneaResult<List<BlaseballTribute>> =
        chroniclerV2("tributes")

    override suspend fun getBloodTypes(bloodIDs: Iterable<String>): KorneaResult<List<String>> = blaseball.getBloodTypes(bloodIDs)
    override suspend fun getCoffeePreferences(coffeeIDs: Iterable<String>): KorneaResult<List<String>> = blaseball.getCoffeePreferences(coffeeIDs)

    override suspend fun getGlobalEvents(): KorneaResult<List<BlaseballGlobalEvent>> =
        chroniclerV2("globalevents")

    override suspend fun getSimulationData(): KorneaResult<BlaseballSimulationData> =
        chroniclerV2("sim")

    override suspend fun getLiveDataStream(): KorneaResult<Flow<String>> =
        chroniclerV2Pagination<BlaseballStreamDataResponse>("stream", from)
            .map { flow -> flow.map { response -> format.encodeToString(response) } }
}

data class IBlaseballDataSourceWrapper(val api: BlaseballApi) : IBlaseballDataSource {
    override suspend fun getIdolBoard(): KorneaResult<BlaseballIdols> =
        api.getIdolBoard()

    override suspend fun getHallOfFlamePlayers(): KorneaResult<List<BlaseballTribute>> =
        api.getHallOfFlamePlayers()

    override suspend fun getBloodTypes(bloodIDs: Iterable<String>): KorneaResult<List<String>> = api.getBloodTypes(bloodIDs)
    override suspend fun getCoffeePreferences(coffeeIDs: Iterable<String>): KorneaResult<List<String>> = api.getCoffeePreferences(coffeeIDs)

    override suspend fun getGlobalEvents(): KorneaResult<List<BlaseballGlobalEvent>> =
        api.getGlobalEvents()

    override suspend fun getSimulationData(): KorneaResult<BlaseballSimulationData> =
        api.getSimulationData()

    override suspend fun getLiveDataStream(): KorneaResult<Flow<String>> =
        api.client.streamAsResult {
            method = HttpMethod.Get
            url("${api.blaseballBaseUrl}/events/streamData")
        }.map { flow ->
            flow.mapNotNull { str ->
                if (str.startsWith("data:")) str.substring(5) else null
            }
        }
}

fun Route.blaseball(source: IBlaseballDataSource) {
    route("/api") api@{
        redirectInternally("/idol_board", "/getIdols")
        get("/getIdols") {
            source.getIdolBoard().respond(call)
        }

        redirectInternally("/hall_of_flame", "/getTribute")
        get("/getTribute") {
            source.getHallOfFlamePlayers().respond(call)
        }
    }

    route("/database") {
        get("/blood") {
            source.getBloodTypes(
                call.parameters.getAll("ids")
                ?: call.parameters["id"]?.let(::listOf)
                ?: emptyList()
            ).respond(call)
        }

        get("/coffee") {
            source.getCoffeePreferences(
                call.parameters.getAll("ids")
                ?: call.parameters["id"]?.let(::listOf)
                ?: emptyList()
            ).respond(call)
        }

        redirectInternally("/ticker", "/globalEvents")
        get("/globalEvents") { source.getGlobalEvents().respond(call) }

        redirectInternally("/sim", "/simulationData")
        get("/simulationData") { source.getSimulationData().respond(call) }
    }

    route("/events") {
        get("/streamData") {
            try {
                source.getLiveDataStream()
                    .doOnSuccess { flow ->
                        call.respondReadChannel(ContentType.parse("text/event-stream")) {
                            val channel = ByteChannel()

                            val job = flow.onEach { data ->
                                channel.writeStringUtf8("data:")
                                channel.writeStringUtf8(data)
                                channel.writeChar('\n')
                                channel.flush()
                            }.launchIn(GlobalScope)

                            job.invokeOnCompletion {
                                channel.close()
                            }

                            channel.attachJob(job)

                            channel
                        }
                    }.respondOnFailure(call)
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
    }
}