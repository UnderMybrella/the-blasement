import com.github.benmanes.caffeine.cache.Caffeine
import com.soywiz.klock.DateTimeTz
import dev.brella.blasement.common.events.BlaseballFeedEventWithContext
import dev.brella.blasement.common.events.TimeRange
import dev.brella.blasement.data.ChroniclerGameUpdateWrapper
import dev.brella.blasement.data.ChroniclerResponseWrapper
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.BlaseballFeedEventType
import dev.brella.kornea.blaseball.base.common.FeedID
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.base.common.beans.BlaseballFeedEvent
import dev.brella.kornea.blaseball.base.common.beans.BlaseballFeedMetadata
import dev.brella.kornea.blaseball.base.common.joinParams
import dev.brella.kornea.blaseball.base.common.json.EventuallyFeedList
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.doOnFailure
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.doOnThrown
import dev.brella.kornea.errors.common.filter
import dev.brella.kornea.errors.common.getOrNull
import dev.brella.kornea.errors.common.map
import dev.brella.ktornea.common.getAsResult
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

typealias GameStepFunc = suspend (gameIDs: Iterable<GameID>) -> Map<GameID, Map<Int, BlaseballDatabaseGame>>

@OptIn(ExperimentalTime::class)
sealed class BlaseballFeed(
    val type: String,
    val loopDuration: Duration = DEFAULT_LOOP_DURATION,
    val getTime: suspend () -> DateTimeTz,
    val timeRange: TimeRange,
    val scope: CoroutineScope,
    val context: CoroutineContext = scope.coroutineContext
) {
    companion object {
        val DEFAULT_LOOP_DURATION = Duration.seconds(4)
    }

    abstract class GameStepsFunc(
        type: String,
        val getGameSteps: GameStepFunc,
        loopDuration: Duration = DEFAULT_LOOP_DURATION,
        getTime: suspend () -> DateTimeTz,
        timeRange: TimeRange,
        scope: CoroutineScope,
        context: CoroutineContext = scope.coroutineContext
    ) : BlaseballFeed(
        type,
        loopDuration,
        getTime,
        timeRange,
        scope,
        context
    ) {
        override suspend fun getGameSteps(gameIDs: Iterable<GameID>): Map<GameID, Map<Int, BlaseballDatabaseGame>> = getGameSteps.invoke(gameIDs)
    }

    class Global(
        val blaseballApi: BlaseballApi,
        getGameSteps: GameStepFunc,
        loopDuration: Duration = DEFAULT_LOOP_DURATION,
        getTime: suspend () -> DateTimeTz,
        range: TimeRange,
        scope: CoroutineScope,
        context: CoroutineContext = scope.coroutineContext
    ) : BlaseballFeed.GameStepsFunc(
        "GLOBAL",
        getGameSteps,
        loopDuration,
        getTime,
        range,
        scope,
        context
    ) {
        constructor(blasement: TheBlasement, loopDuration: Duration = DEFAULT_LOOP_DURATION, getTime: suspend () -> DateTimeTz, range: TimeRange, scope: CoroutineScope = blasement, context: CoroutineContext = scope.coroutineContext) : this(
            blasement.blaseballApi,
            blasement.liveData::getGames,
            loopDuration,
            getTime,
            range,
            scope,
            context
        )

        override suspend fun getFeed(limit: Int, offset: Int?, start: DateTimeTz?): KorneaResult<List<BlaseballFeedEvent>> =
            blaseballApi.getGlobalFeed(limit = limit, sort = 1, start = offset?.toString())
    }

    class GlobalMachine(
        val json: Json,
        val client: HttpClient,
        loopDuration: Duration = DEFAULT_LOOP_DURATION,
        getTime: suspend () -> DateTimeTz,
        range: TimeRange,
        scope: CoroutineScope,
        context: CoroutineContext = scope.coroutineContext
    ) : BlaseballFeed(
        "MACHINE_GLOBAL",
        loopDuration,
        getTime,
        range,
        scope,
        context
    ) {

        val gameStepCache = Caffeine.newBuilder()
            .maximumSize(24)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build<GameID, Map<Int, BlaseballDatabaseGame>>()

        override suspend fun getGameSteps(gameIDs: Iterable<GameID>): Map<GameID, Map<Int, BlaseballDatabaseGame>> {
//            println(">> Hitting Game Steps >>")
            val map: MutableMap<GameID, MutableMap<Int, BlaseballDatabaseGame>> = HashMap()

            gameStepCache.getAllPresent(gameIDs)
                .forEach { (gameID, gameSteps) ->
                    try {
                        map[gameID] = gameSteps.toMutableMap()
                    } catch (th: Throwable) {
                        th.printStackTrace()
                    }
                }

            val newGameIDs = gameIDs.filterNot(map::containsKey)
            var page: String? = null

            val gameParams = newGameIDs.joinParams()
            val limit = (200 * newGameIDs.size).coerceAtMost(1000)

            while (true) {
//                println(">> Game Steps Loop; Retrieving $limit >>")
                yield()

                val response = client.getAsResult<ChroniclerResponseWrapper<ChroniclerGameUpdateWrapper>>("$CHRONICLER_HOST/v1/games/updates") {
                    page?.let { parameter("page", it) }
                    parameter("game", gameParams)
                    parameter("count", limit)

//                    println("Requesting ${url.clone().buildString()}")
                }.getOrNull() ?: break

//                println(">> Got ${response.data?.size} Results >>")

                if (response.data?.isNotEmpty() != true) break

                page = response.nextPage

                response.data.forEach { game ->
                    map.computeIfAbsent(GameID(game.gameId)) { HashMap() }
                        .putIfAbsent(game.data.playCount, game.data)
                }
            }

            map.forEach { (gameID, gameSteps) -> gameStepCache.put(gameID, gameSteps) }

            return map
        }

        override suspend fun getFeed(limit: Int, offset: Int?, start: DateTimeTz?): KorneaResult<List<BlaseballFeedEvent>> =
            client.getAsResult<EventuallyFeedList>("$UPNUTS_HOST/events") {
//            parameter("after", (start?.let(BLASEBALL_TIME_PATTERN::parse) ?: now()).utc.unixMillisLong / 1000)
//            parameter("before", now().utc.unixMillisLong / 1000)

                val time = (start ?: getTime()).utc.unixMillisLong / 1000

                parameter("after", time)

                parameter("limit", limit)
                parameter("time", time)

//                if (category != null) parameter("category", category)
//                if (type != null) parameter("type", type)
//                if (start != null) parameter("offset", start)
//                if (sort != null) parameter("sort", sort)
                parameter("sortorder", "asc")


                offset?.let { parameter("offset", it) }
                timeout {
                    socketTimeoutMillis = 60_000L
                }

//                println("Requesting ${url.clone().buildString()}")
            }//.also { println(it) }
    }

    abstract suspend fun getGameSteps(gameIDs: Iterable<GameID>): Map<GameID, Map<Int, BlaseballDatabaseGame>>

    private val feedFlow = MutableSharedFlow<BlaseballFeedEventWithContext>()
    private val feedFlowByType: MutableMap<Int, MutableSharedFlow<BlaseballFeedEventWithContext>> = HashMap()
    val flow: SharedFlow<BlaseballFeedEventWithContext> by ::feedFlow

    fun flowByType(type: Int): SharedFlow<BlaseballFeedEventWithContext> = feedFlowByType.computeIfAbsent(type) { MutableSharedFlow() }

    abstract suspend fun getFeed(limit: Int, offset: Int? = null, start: DateTimeTz? = null): KorneaResult<List<BlaseballFeedEvent>>

    @OptIn(ExperimentalTime::class)
    val collector = scope.launch(context) {
        val lastFile = File("$type.start")
        var lastID: DateTimeTz = getTime()

        val totalFeed: MutableList<BlaseballFeedEvent> = ArrayList(100)
        val lastIDSet: MutableSet<FeedID> = HashSet()

        val minLimit = 100
        val maxLimit = 800
        var limit = 100

        loopEvery(loopDuration, `while` = { isActive }) {
            var offset: Int? = null
            val start = lastID
            do {
                getFeed(limit = limit, offset = offset, start = start)
                    .map { list -> list.filter { it.created in timeRange && it.id !in lastIDSet } }
                    .filter { it.isNotEmpty() }
                    .doOnSuccess { feed ->
                        val now = getTime()
                        val createdAfterIndex = feed.indexOfFirst { it.created > now }
                        if (createdAfterIndex == 0)  {
                            offset = null
                        } else {
                            if (createdAfterIndex > 0) {
                                val startList = feed.subList(0, createdAfterIndex)
                                val endList = feed.subList(createdAfterIndex, feed.size)

                                emitAll(startList)

                                val waitingUntil = endList.first().created

//                                println("Waiting until $waitingUntil (${getTime()})")

                                while (isActive && getTime() < waitingUntil) yield()

                                emitAll(endList)
                            } else {
                                emitAll(feed)
                            }

                            lastID = feed.last().created
//                            println("Retrieved ${start} - ${lastID}")
                            lastIDSet.clear()
                            feed.forEach { lastIDSet.add(it.id) }

                            offset = offset?.plus(feed.size) ?: feed.size
                        }

//                    if (feed.size == limit && limit < maxLimit) {
//                        limit = limit shl 1
//                    } else if (feed.size <= limit shr 1 && limit > minLimit) {
//                        limit = limit shr 1
//                    }

//                    lastFile.writeText(lastID?.let(BLASEBALL_TIME_PATTERN::format) ?: "")
                    }.doOnThrown { error -> error.exception.printStackTrace() }
                    .doOnFailure { delay(500L); offset = null }
            } while (offset != null)

//            println("We are ${(getTime() - lastID).seconds}s behind")
        }
    }

    suspend fun emitAll(list: List<BlaseballFeedEvent>) = coroutineScope {
        val games = getGameSteps(list.flatMap { it.gameTags }.distinct())
        val events = list.map { event ->
            val applicableGame = event.gameTags.firstOrNull()?.let(games::get)
            val gameStep: BlaseballDatabaseGame? = (event.metadata as? BlaseballFeedMetadata.WithPlay)?.play?.let { applicableGame?.get(it) }
//            val parent: FeedID? = (event.metadata as? BlaseballFeedMetadata.WithParent)?.parent
//            val children = (event.metadataevent.metadata?.get("children")?.jsonArray?.map { FeedID(it.jsonPrimitive.content) }

//            val subPlay = event.metadata?.get("subPlay")?.jsonPrimitive?.intOrNull?.takeUnless { it == -1 }

            BlaseballFeedEventWithContext(event, gameStep)
        }

        launch {
            events.groupBy { it.event.type }
                .filterKeys { it !in BlaseballFeedEventType }
                .forEach { (k, v) ->
                    println("New feed event type: $k / ${v[0]}")
                }
        }
//        GlobalScope.launch(Dispatchers.IO) {
//            events.forEach { event ->
//                val dir = File("sample data/$type/${BlaseballFeedEventType.textFromType(event.event.type).toUpperCase().replace("[^A-Z0-9]".toRegex(), "_")}")
//                dir.mkdirs()
//                File(dir, "${event.event.id.id}.json").writeText(blasement.json.encodeToString(event))
//            }
//        }
        launch { events.forEach { feedFlow.emit(it) } }
        launch {
            events.forEach {
                feedFlowByType.computeIfAbsent(it.event.type) { MutableSharedFlow() }.emit(it)
            }
        }
    }
}