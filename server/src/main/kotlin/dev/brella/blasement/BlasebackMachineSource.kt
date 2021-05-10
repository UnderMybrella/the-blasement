package dev.brella.blasement

import CHRONICLER_HOST
import UPNUTS_HOST
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.minutes as klockMinutes
import com.soywiz.klock.hours as klockHours
import com.soywiz.klock.milliseconds as klockMilliseconds
import com.soywiz.klock.parse
import dev.brella.blasement.blaseback.BlasementDataSource
import dev.brella.blasement.common.events.BlaseballFeedEventWithContext
import dev.brella.blasement.common.events.FanID
import dev.brella.blasement.common.events.TimeRange
import dev.brella.blasement.utils.chroniclerEntity
import dev.brella.blasement.utils.chroniclerEntityList
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
import io.ktor.client.features.*
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.DurationUnit
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
                            for (i in 0 until 5) {
                                val jsonStr = Json.encodeToString(json.getValue("data"))
                                try {
                                    Json.decodeFromString<JsonObject>(jsonStr)
                                    emit(jsonStr)
                                    break
                                } catch (th: SerializationException) {
//                                    println("Str failed $i times, trying again")
                                    delay(10)
                                }
                            }

                            val validTo = json["validTo"]?.jsonPrimitive?.contentOrNull

                            if (validTo != null) {
                                wait(BLASEBALL_TIME_PATTERN.parse(validTo))

                                http.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
                                    parameter("type", "stream")
                                    parameter("at", BLASEBALL_TIME_PATTERN.format(now()))
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

    @OptIn(ExperimentalTime::class)
    private val _globalFeed by lazy { BlaseballFeed.GlobalMachine(format, http, Duration.seconds(1), this::now, validRange, BlasebackMachineSource) }
    override val globalFeed: SharedFlow<BlaseballFeedEventWithContext>
        get() = _globalFeed.flow

    override suspend fun getFeedByPhase(phase: Int, season: Int): KorneaResult<List<BlaseballFeedEvent>> =
        http.getAsResult<List<EventuallyFeedEvent>>("$UPNUTS_HOST/events") {
//            parameter("after", BLASEBALL_TIME_PATTERN.format(now()))
            parameter("before", now().utc.unixMillisLong / 1000)
            parameter("phase_min", phase)
            parameter("phase_max", phase)
            parameter("seasons", season)

            parameter("time", BLASEBALL_TIME_PATTERN.format(now()))
        }.map { list -> list.map { it.toBlaseball() } }

    override suspend fun getGlobalFeed(
        category: Int?,
        limit: Int,
        type: Int?,
        sort: Int?,
        start: String?,
        fanID: FanID?
    ): KorneaResult<List<BlaseballFeedEvent>> =
        http.getAsResult<EventuallyFeedList>("$UPNUTS_HOST/feed/global") {
//            parameter("after", (start?.let(BLASEBALL_TIME_PATTERN::parse) ?: now()).utc.unixMillisLong / 1000)
//            parameter("before", now().utc.unixMillisLong / 1000)

            parameter("limit", limit)
            parameter("time", BLASEBALL_TIME_PATTERN.format(now()))

            fanID?.let { parameter("player", it.id) }

            if (category != null) parameter("category", category)
            if (type != null) parameter("type", type)
            if (start != null) parameter("offset", start)
            if (sort != null) parameter("sort", sort)

            timeout {
                socketTimeoutMillis = 60_000L
            }
        }

    override suspend fun getPlayerFeed(
        id: PlayerID,
        category: Int?,
        limit: Int,
        type: Int?,
        sort: Int?,
        start: String?,
        fanID: FanID?
    ): KorneaResult<List<BlaseballFeedEvent>> =
        http.getAsResult<EventuallyFeedList>("$UPNUTS_HOST/feed/player") {
//            parameter("after", (start?.let(BLASEBALL_TIME_PATTERN::parse) ?: now()).utc.unixMillisLong / 1000)
//            parameter("before", now().utc.unixMillisLong / 1000)
            parameter("time", BLASEBALL_TIME_PATTERN.format(now()))
            parameter("limit", limit)

            parameter("id", id.id)

            if (category != null) parameter("category", category)
            if (type != null) parameter("type", type)
            if (start != null) parameter("offset", start)
            if (sort != null) parameter("sort", sort)

            fanID?.let { parameter("player", it.id) }

            when (sort) {
                0 -> parameter("sortorder", "desc")
                1 -> parameter("sortorder", "asc")
            }
        }

    override suspend fun getTeamFeed(
        id: TeamID,
        category: Int?,
        limit: Int,
        type: Int?,
        sort: Int?,
        start: String?,
        fanID: FanID?
    ): KorneaResult<List<BlaseballFeedEvent>> =
        http.getAsResult<EventuallyFeedList>("$UPNUTS_HOST/feed/team") {
//            parameter("after", (start?.let(BLASEBALL_TIME_PATTERN::parse) ?: now()).utc.unixMillisLong / 1000)
//            parameter("before", now().utc.unixMillisLong / 1000)

            parameter("time", BLASEBALL_TIME_PATTERN.format(now()))
            parameter("limit", limit)
            parameter("id", id.id)
//            else parameter("teamTags", id.id)

            if (category != null) parameter("category", category)
            if (type != null) parameter("type", type)
            if (start != null) parameter("offset", start)
            if (sort != null) parameter("sort", sort)

            fanID?.let { parameter("player", it.id) }

            when (sort) {
                0 -> parameter("sortorder", "desc")
                1 -> parameter("sortorder", "asc")
//                null -> {
//                }
//                else -> parameter("sortorder", sort)
            }
        }

    override suspend
    fun getIdolBoard(): KorneaResult<BlaseballIdols> =
        http.chroniclerEntity("idols", at = BLASEBALL_TIME_PATTERN.format(now()))

    override suspend
    fun getHallOfFlamePlayers(): KorneaResult<List<BlaseballTribute>> =
        http.chroniclerEntity("tributes", at = BLASEBALL_TIME_PATTERN.format(now()))

    override suspend
    fun getBloodTypes(bloodIDs: Iterable<String>): KorneaResult<List<String>> = blaseball.getBloodTypes(bloodIDs)

    override suspend
    fun getCoffeePreferences(coffeeIDs: Iterable<String>): KorneaResult<List<String>> = blaseball.getCoffeePreferences(coffeeIDs)

    override suspend
    fun getItems(itemIDs: Iterable<ItemID>): KorneaResult<List<BlaseballItem>> =
        blaseball.getItems(itemIDs)

    override suspend
    fun getModifications(modIDs: Iterable<ModificationID>): KorneaResult<List<BlaseballMod>> =
        blaseball.getModifications(modIDs)

    override suspend
    fun getPlayers(playerIDs: Iterable<PlayerID>): KorneaResult<List<BlaseballDatabasePlayer>> =
        http.chroniclerEntityList("player", at = BLASEBALL_TIME_PATTERN.format(now())) { parameter("id", playerIDs.joinParams()) }

    override suspend
    fun getGlobalEvents(): KorneaResult<List<BlaseballGlobalEvent>> =
        http.chroniclerEntity("globalevents", at = BLASEBALL_TIME_PATTERN.format(now()))

    override suspend
    fun getSimulationData(): KorneaResult<BlaseballSimulationData> =
        http.chroniclerEntity("sim", at = BLASEBALL_TIME_PATTERN.format(now()))

    override suspend
    fun getLiveDataStream(): KorneaResult<Flow<String>> =
        KorneaResult.success(liveFeedFlow)
}

@OptIn(ExperimentalTime::class)
class BlasebackMachineAccelerated(http: HttpClient, blaseball: BlaseballApi, json: Json, from: String, to: String, val ratePerSecond: Duration, startTime: TimeSpan? = null) : BlasebackMachineSource(http, blaseball, json, from, to) {
    companion object {
        fun collections(http: HttpClient, blaseball: BlaseballApi, json: Json, ratePerSecond: Duration, startTime: TimeSpan? = null) =
            BlasebackMachineAccelerated(http, blaseball, json, "2021-04-19T00:00:00Z", "2021-04-26T00:00:00Z", ratePerSecond, startTime)

        fun massProduction(http: HttpClient, blaseball: BlaseballApi, json: Json, ratePerSecond: Duration, startTime: TimeSpan? = null) =
            BlasebackMachineAccelerated(http, blaseball, json, "2021-04-12T00:00:00Z", "2021-04-19T00:00:00Z", ratePerSecond, startTime)

        fun liveBait(http: HttpClient, blaseball: BlaseballApi, json: Json, ratePerSecond: Duration, startTime: TimeSpan? = null) =
            BlasebackMachineAccelerated(http, blaseball, json, "2021-04-05T00:00:00Z", "2021-04-12T00:00:00Z", ratePerSecond, startTime)

        inline fun startAtDay(day: Int) = (1.klockHours * day)
    }

    val beginning = BLASEBALL_TIME_PATTERN.parse(from) + (startTime ?: 0.klockMinutes) //12.klockHours + 3.klockHours + startAtDay(23)

    val start = TimeSource.Monotonic.markNow()
    override val liveFeedFlow: SharedFlow<String> = launchLiveFeedFlow()

    override fun now(): DateTimeTz =
        beginning + (ratePerSecond * start.elapsedNow().toDouble(DurationUnit.SECONDS)).inWholeMilliseconds.klockMilliseconds

    override suspend fun wait(until: DateTimeTz) {
        delay((((until - now()).seconds / ratePerSecond.toDouble(DurationUnit.SECONDS)) * 1_000L).roundToLong())
    }
}

@OptIn(ExperimentalTime::class)
class BlasebackMachineConcurrent(http: HttpClient, blaseball: BlaseballApi, json: Json, from: String, to: String) : BlasebackMachineSource(http, blaseball, json, from, to) {
    companion object {
        fun collections(http: HttpClient, blaseball: BlaseballApi, json: Json) =
            BlasebackMachineConcurrent(http, blaseball, json, "2021-04-19T00:00:00Z", "2021-04-26T00:00:00Z")

        fun massProduction(http: HttpClient, blaseball: BlaseballApi, json: Json) =
            BlasebackMachineConcurrent(http, blaseball, json, "2021-04-12T00:00:00Z", "2021-04-19T00:00:00Z")

        fun liveBait(http: HttpClient, blaseball: BlaseballApi, json: Json) =
            BlasebackMachineConcurrent(http, blaseball, json, "2021-04-05T00:00:00Z", "2021-04-12T00:00:00Z")
    }

    val beginning = BLASEBALL_TIME_PATTERN.parse(from)

    val start = TimeSource.Monotonic.markNow()
    override val liveFeedFlow: SharedFlow<String> = launchLiveFeedFlow()

    val startOfWeekShift = DateTime.now().let { utc -> utc.utc - utc.time.encoded - (24.klockHours * (utc.dayOfWeekInt - 1)) }

    override fun now(): DateTimeTz =
        beginning + (DateTime.now().utc - startOfWeekShift)

    override suspend fun wait(until: DateTimeTz) {
        delay((until - now()).millisecondsLong)
    }

    init {
        println(startOfWeekShift)
        println(now())
        println((DateTime.now().utc - startOfWeekShift).hours)
        println(DateTime.now().utc)
        println(beginning)
        println(beginning + (DateTime.now().utc - startOfWeekShift))
    }
}

/**
 *
 */