import dev.brella.blasement.common.events.BlaseballFeedEventWithContext
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.BlaseballFeedEventType
import dev.brella.kornea.blaseball.FeedID
import dev.brella.kornea.blaseball.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.beans.BlaseballFeedEvent
import dev.brella.kornea.blaseball.beans.BlaseballFeedMetadata
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import dev.brella.kornea.blaseball.writeAsJson
import io.ktor.network.sockets.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

sealed class BlaseballFeed(val type: String, val blasement: TheBlasement, val scope: CoroutineScope, val context: CoroutineContext = scope.coroutineContext) {
    class Global(blasement: TheBlasement, scope: CoroutineScope = blasement, context: CoroutineContext = scope.coroutineContext) : BlaseballFeed("GLOBAL", blasement, scope, context) {
        override suspend fun getFeed(limit: Int, start: String?): List<BlaseballFeedEvent> = blasement.blaseballApi.getGlobalFeed(limit = limit, sort = 1, start = start)
    }

    private val feedFlow = MutableSharedFlow<BlaseballFeedEventWithContext>()
    private val feedFlowByType: MutableMap<Int, MutableSharedFlow<BlaseballFeedEventWithContext>> = HashMap()
    val flow: SharedFlow<BlaseballFeedEventWithContext> by this::feedFlow

    fun flowByType(type: Int): SharedFlow<BlaseballFeedEventWithContext> = feedFlowByType.computeIfAbsent(type) { MutableSharedFlow() }

    abstract suspend fun getFeed(limit: Int, start: String?): List<BlaseballFeedEvent>

    @OptIn(ExperimentalTime::class)
    val collector = scope.launch(context) {
        val lastFile = File("$type.start")
        var lastID: String? = if (lastFile.exists()) lastFile.readText().takeIf(String::isNotBlank) else null
        val totalFeed: MutableList<BlaseballFeedEvent> = ArrayList(100)

        loopEvery(5.seconds, `while` = { isActive }) {
            try {
                val feed = getFeed(limit = 100, start = lastID)
                if (feed.isEmpty()) {
                    delay(500L)
                } else {
                    emitAll(feed)
                    lastID = feed.last().created
                }

                lastFile.writeText(lastID ?: "")
            } catch (timeout: SocketTimeoutException) {
                timeout.printStackTrace()
                delay(500)
            }
        }
    }

    suspend fun emitAll(list: List<BlaseballFeedEvent>) = coroutineScope {
        val events = list.map { event ->
            val applicableGame = event.gameTags.firstOrNull()?.let { blasement.liveData.getGame(it) }
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
        GlobalScope.launch(Dispatchers.IO) {
            events.forEach { event ->
                val dir = File("sample data/$type/${BlaseballFeedEventType.textFromType(event.event.type).toUpperCase().replace("[^A-Z0-9]".toRegex(), "_")}")
                dir.mkdirs()
                File(dir, "${event.event.id.id}.json").writeText(blasement.json.encodeToString(event))
            }
        }
        launch { events.forEach { feedFlow.emit(it) } }
        launch {
            events.forEach {
                feedFlowByType.computeIfAbsent(it.event.type) { MutableSharedFlow() }.emit(it)
            }
        }
    }
}