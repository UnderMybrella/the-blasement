import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.parse
import dev.brella.blasement.common.events.BlaseballFeedEventWithContext
import dev.brella.blasement.common.events.TimeRange
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.BlaseballFeedEventType
import dev.brella.kornea.blaseball.base.common.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.base.common.beans.BlaseballFeedEvent
import dev.brella.kornea.blaseball.base.common.beans.BlaseballFeedMetadata
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.doOnFailure
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.doOnThrown
import dev.brella.kornea.errors.common.filter
import dev.brella.kornea.errors.common.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

sealed class BlaseballFeed(val type: String, val blasement: TheBlasement, val timeRange: TimeRange, val scope: CoroutineScope, val context: CoroutineContext = scope.coroutineContext) {
    class Global(blasement: TheBlasement, range: TimeRange, scope: CoroutineScope = blasement, context: CoroutineContext = scope.coroutineContext) : BlaseballFeed("GLOBAL", blasement, range, scope, context) {
        override suspend fun getFeed(limit: Int, start: String?): KorneaResult<List<BlaseballFeedEvent>> =
            blasement.blaseballApi.getGlobalFeed(limit = limit, sort = 1, start = start)
    }

    private val feedFlow = MutableSharedFlow<BlaseballFeedEventWithContext>()
    private val feedFlowByType: MutableMap<Int, MutableSharedFlow<BlaseballFeedEventWithContext>> = HashMap()
    val flow: SharedFlow<BlaseballFeedEventWithContext> by this::feedFlow

    fun flowByType(type: Int): SharedFlow<BlaseballFeedEventWithContext> = feedFlowByType.computeIfAbsent(type) { MutableSharedFlow() }

    abstract suspend fun getFeed(limit: Int, start: String?): KorneaResult<List<BlaseballFeedEvent>>

    @OptIn(ExperimentalTime::class)
    val collector = scope.launch(context) {
        val lastFile = File("$type.start")
        var lastID: DateTimeTz? =
            if (lastFile.exists())
                lastFile.readText()
                    .takeIf(String::isNotBlank)
                    ?.let(BLASEBALL_TIME_PATTERN::parse)
                    ?.takeIf { it in timeRange }
                ?: timeRange.start
            else
                timeRange.start
        val totalFeed: MutableList<BlaseballFeedEvent> = ArrayList(100)

        val minLimit = 100
        val maxLimit = 800
        var limit = 100

        loopEvery(4.seconds, `while` = { isActive }) {
            getFeed(limit = limit, start = lastID?.let(BLASEBALL_TIME_PATTERN::format))
                .map { list -> list.filter { it.created in timeRange } }
                .filter { it.isNotEmpty() }
                .doOnSuccess { feed ->
                    emitAll(feed)
                    lastID = feed.last().created

                    if (feed.size == limit && limit < maxLimit) {
                        limit = limit shl 1
                    } else if (feed.size <= limit shr 1 && limit > minLimit) {
                        limit = limit shr 1
                    }

                    lastFile.writeText(lastID?.let(BLASEBALL_TIME_PATTERN::format) ?: "")
                }.doOnThrown { error -> error.exception.printStackTrace() }
                .doOnFailure { delay(500L) }
        }
    }

    suspend fun emitAll(list: List<BlaseballFeedEvent>) = coroutineScope {
        val games = blasement.liveData.getGames(list.flatMap { it.gameTags }.distinct())
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