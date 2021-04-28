package dev.brella.blasement

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.hours as klockHours
import com.soywiz.klock.milliseconds as klockMilliseconds
import com.soywiz.klock.parse
import dev.brella.blasement.common.events.FanID
import dev.brella.blasement.common.events.TimeRange
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.FeedID
import dev.brella.kornea.blaseball.base.common.ItemID
import dev.brella.kornea.blaseball.base.common.ModificationID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballDatabasePlayer
import dev.brella.kornea.blaseball.base.common.beans.BlaseballFeedEvent
import dev.brella.kornea.blaseball.base.common.beans.BlaseballGlobalEvent
import dev.brella.kornea.blaseball.base.common.beans.BlaseballIdols
import dev.brella.kornea.blaseball.base.common.beans.BlaseballItem
import dev.brella.kornea.blaseball.base.common.beans.BlaseballMod
import dev.brella.kornea.blaseball.base.common.beans.BlaseballSimulationData
import dev.brella.kornea.blaseball.base.common.beans.BlaseballTribute
import dev.brella.kornea.blaseball.base.common.joinParams
import dev.brella.kornea.blaseball.base.common.json.EventuallyFeedList
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.flatMap
import dev.brella.kornea.errors.common.map
import dev.brella.ktornea.common.getAsResult
import getJsonArray
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.seconds

abstract class BlasebackMachineSource(val http: HttpClient, val blaseball: BlaseballApi, val format: Json, val from: String, val to: String) : BlasementDataSource {
    companion object : CoroutineScope {
        override val coroutineContext: CoroutineContext = SupervisorJob()
    }

    val validRange = TimeRange.fromChronicler(from, to)

    abstract val liveFeedFlow: SharedFlow<String>

    fun launchLiveFeedFlow(): SharedFlow<String> =
        flow {
            http.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
                parameter("type", "stream")
                parameter("at", BLASEBALL_TIME_PATTERN.format(now()))
            }.doOnSuccess { json ->
                json.getJsonArray("items")
                    .firstOrNull()
                    ?.jsonObject
                    ?.let { baseJson ->
                        var json = baseJson

                        while (isActive) {
                            emit(format.encodeToString(json.getValue("data")))

                            val validTo = json["validTo"]?.jsonPrimitive?.contentOrNull

                            if (validTo != null) {
                                wait(BLASEBALL_TIME_PATTERN.parse(validTo))

                                http.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
                                    parameter("type", "stream")
                                    parameter("at", validTo)
                                }.doOnSuccess { response ->
                                    response.getJsonArray("items")
                                        .firstOrNull()?.jsonObject
                                        ?.let { json = it }
                                }
                            }
                        }
                    }
            }
        }.shareIn(BlasebackMachineSource, SharingStarted.Eagerly, 1)

    suspend inline fun <reified T> chroniclerV2(type: String, at: String = BLASEBALL_TIME_PATTERN.format(now()), builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<T> =
        http.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
            parameter("type", type)
            parameter("at", at)

            builder()
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

    suspend inline fun <reified T> chroniclerV2List(type: String, at: String = BLASEBALL_TIME_PATTERN.format(now()), builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<List<T>> =
        http.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
            parameter("type", type)
            parameter("at", at)

            builder()
        }.flatMap { json ->
            try {
                KorneaResult.successOrEmpty(
                    json.getJsonArray("items")
                        .mapNotNull { element -> element.jsonObject["data"]?.let { this.format.decodeFromString(this.format.encodeToString(it)) } }
                )
            } catch (th: Throwable) {
                KorneaResult.thrown(th)
            }
        }

    @OptIn(ExperimentalTime::class)
    suspend inline fun <reified T> chroniclerV2Pagination(type: String, at: String = BLASEBALL_TIME_PATTERN.format(now()), delay: Duration = 5.seconds, limit: Int = 5, maximumDelay: Duration = 5.seconds): KorneaResult<Flow<T>> =
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

                                    if (validTo != null) {
                                        wait(BLASEBALL_TIME_PATTERN.parse(validTo))

                                        http.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
                                            parameter("type", type)
                                            parameter("at", validTo)
                                        }.doOnSuccess { response ->
                                            response.getJsonArray("items")
                                                .firstOrNull()?.jsonObject
                                                ?.let { json = it }
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

    override suspend fun getFeedByPhase(phase: Int, season: Int): KorneaResult<List<BlaseballFeedEvent>> =
        http.getAsResult<List<EventuallyFeedEvent>>("https://api.sibr.dev/eventually/events") {
//            parameter("after", BLASEBALL_TIME_PATTERN.format(now()))
            parameter("before", now().utc.unixMillisLong / 1000)
            parameter("phase_min", phase)
            parameter("phase_max", phase)
            parameter("seasons", season)
        }.map { list -> list.map { it.toBlaseball() } }

    override suspend fun getGlobalFeed(category: Int?, limit: Int, type: Int?, sort: Int?, start: String?, upNuts: Map<FeedID, Set<FanID>>, fanID: FanID?): KorneaResult<List<BlaseballFeedEvent>> {
        val result = http.getAsResult<EventuallyFeedList>("https://api.sibr.dev/eventually/events") {
//            parameter("after", (start?.let(BLASEBALL_TIME_PATTERN::parse) ?: now()).utc.unixMillisLong / 1000)
            parameter("before", now().utc.unixMillisLong / 1000)
            parameter("limit", limit)

            if (category != null) parameter("category", category)
            if (type != null) parameter("type", type)
            if (start != null) parameter("offset", start)

            when (sort) {
                0 -> parameter("sortorder", "desc")
                1 -> parameter("sortorder", "asc")
            }
        }.let { result ->
            if (fanID == null) {
                result.map { feedList ->
                    feedList.map { event ->
                        upNuts[event.id]?.let { event.nuts += it.size }
                        event
                    }
                }
            } else {
                result.map { feedList ->
                    feedList.map { event ->
                        upNuts[event.id]?.let {
                            if (fanID in it) event.metadata?.upnut = true

                            event.nuts += it.size
                        }
                        event
                    }
                }
            }
        }

        if (sort == 2 || sort == 3) return result.map { list -> list.sortedByDescending(BlaseballFeedEvent::nuts) }

        return result
    }

    override suspend fun getPlayerFeed(id: PlayerID, category: Int?, limit: Int, type: Int?, sort: Int?, start: String?, upNuts: Map<FeedID, Set<FanID>>, fanID: FanID?): KorneaResult<List<BlaseballFeedEvent>> {
        val result = http.getAsResult<EventuallyFeedList>("https://api.sibr.dev/eventually/events") {
//            parameter("after", (start?.let(BLASEBALL_TIME_PATTERN::parse) ?: now()).utc.unixMillisLong / 1000)
            parameter("before", now().utc.unixMillisLong / 1000)
            parameter("limit", limit)
            parameter("playerTags", id.id)

            if (category != null) parameter("category", category)
            if (type != null) parameter("type", type)
            if (start != null) parameter("offset", start)

            when (sort) {
                0 -> parameter("sortorder", "desc")
                1 -> parameter("sortorder", "asc")
            }
        }.let { result ->
            if (fanID == null) {
                result.map { feedList ->
                    feedList.map { event ->
                        upNuts[event.id]?.let { event.nuts += it.size }
                        event
                    }
                }
            } else {
                result.map { feedList ->
                    feedList.map { event ->
                        upNuts[event.id]?.let {
                            if (fanID in it) event.metadata?.upnut = true

                            event.nuts += it.size
                        }
                        event
                    }
                }
            }
        }

        if (sort == 2 || sort == 3) return result.map { list -> list.sortedByDescending(BlaseballFeedEvent::nuts) }

        return result
    }

    override suspend fun getTeamFeed(id: TeamID, category: Int?, limit: Int, type: Int?, sort: Int?, start: String?, upNuts: Map<FeedID, Set<FanID>>, fanID: FanID?): KorneaResult<List<BlaseballFeedEvent>> {
        val result = http.getAsResult<EventuallyFeedList>("https://api.sibr.dev/eventually/events") {
//            parameter("after", (start?.let(BLASEBALL_TIME_PATTERN::parse) ?: now()).utc.unixMillisLong / 1000)
            parameter("before", now().utc.unixMillisLong / 1000)
            parameter("limit", limit)
            parameter("teamTags", id.id)

            if (category != null) parameter("category", category)
            if (type != null) parameter("type", type)
            if (start != null) parameter("offset", start)

            when (sort) {
                0 -> parameter("sortorder", "desc")
                1 -> parameter("sortorder", "asc")
//                null -> {
//                }
//                else -> parameter("sortorder", sort)
            }
        }.let { result ->
            if (fanID == null) {
                result.map { feedList ->
                    feedList.map { event ->
                        upNuts[event.id]?.let { event.nuts += it.size }
                        event
                    }
                }
            } else {
                result.map { feedList ->
                    feedList.map { event ->
                        upNuts[event.id]?.let {
                            if (fanID in it) event.metadata?.upnut = true

                            event.nuts += it.size
                        }
                        event
                    }
                }
            }
        }

        if (sort == 2 || sort == 3) return result.map { list -> list.sortedByDescending(BlaseballFeedEvent::nuts) }

        return result
    }

    override suspend fun getIdolBoard(): KorneaResult<BlaseballIdols> =
        chroniclerV2("idols")

    override suspend fun getHallOfFlamePlayers(): KorneaResult<List<BlaseballTribute>> =
        chroniclerV2("tributes")

    override suspend fun getBloodTypes(bloodIDs: Iterable<String>): KorneaResult<List<String>> = blaseball.getBloodTypes(bloodIDs)
    override suspend fun getCoffeePreferences(coffeeIDs: Iterable<String>): KorneaResult<List<String>> = blaseball.getCoffeePreferences(coffeeIDs)

    override suspend fun getItems(itemIDs: Iterable<ItemID>): KorneaResult<List<BlaseballItem>> =
        blaseball.getItems(itemIDs)

    override suspend fun getModifications(modIDs: Iterable<ModificationID>): KorneaResult<List<BlaseballMod>> =
        blaseball.getModifications(modIDs)

    override suspend fun getPlayers(playerIDs: Iterable<PlayerID>): KorneaResult<List<BlaseballDatabasePlayer>> =
        chroniclerV2List("player") { parameter("id", playerIDs.joinParams()) }

    override suspend fun getGlobalEvents(): KorneaResult<List<BlaseballGlobalEvent>> =
        chroniclerV2("globalevents")

    override suspend fun getSimulationData(): KorneaResult<BlaseballSimulationData> =
        chroniclerV2("sim")

    override suspend fun getLiveDataStream(): KorneaResult<Flow<String>> =
        KorneaResult.success(liveFeedFlow)
}

@OptIn(ExperimentalTime::class)
class BlasebackMachineAccelerated(http: HttpClient, blaseball: BlaseballApi, json: Json, from: String, to: String, val ratePerSecond: Duration) : BlasebackMachineSource(http, blaseball, json, from, to) {
    companion object {
        fun collections(http: HttpClient, blaseball: BlaseballApi, json: Json, ratePerSecond: Duration) =
            BlasebackMachineAccelerated(http, blaseball, json, "2021-04-19T00:00:00Z", "2021-04-26T00:00:00Z", ratePerSecond)

        fun massProduction(http: HttpClient, blaseball: BlaseballApi, json: Json, ratePerSecond: Duration) =
            BlasebackMachineAccelerated(http, blaseball, json, "2021-04-12T00:00:00Z", "2021-04-19T00:00:00Z", ratePerSecond)

        fun liveBait(http: HttpClient, blaseball: BlaseballApi, json: Json, ratePerSecond: Duration) =
            BlasebackMachineAccelerated(http, blaseball, json, "2021-04-05T00:00:00Z", "2021-04-12T00:00:00Z", ratePerSecond)
    }

    val beginning = BLASEBALL_TIME_PATTERN.parse(from) + 24.klockHours + 12.klockHours + 3.klockHours

    val start = TimeSource.Monotonic.markNow()
    override val liveFeedFlow: SharedFlow<String> = launchLiveFeedFlow()

    override fun now(): DateTimeTz =
        beginning + (ratePerSecond * start.elapsedNow().inSeconds).toLongMilliseconds().klockMilliseconds

    override suspend fun wait(until: DateTimeTz) {
        delay((((until - now()).seconds / ratePerSecond.inSeconds) * 1_000L).roundToLong())
    }
}

/**
 *
 */