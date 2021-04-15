import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.BlaseballFeedEventType
import dev.brella.kornea.blaseball.FeedID
import dev.brella.kornea.blaseball.beans.BlaseballFeedEvent
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import io.ktor.network.sockets.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

sealed class BlaseballFeed(val type: String, val blaseballApi: BlaseballApi, val chroniclerApi: ChroniclerApi, val scope: CoroutineScope, val context: CoroutineContext = scope.coroutineContext) {
    class Global(blaseballApi: BlaseballApi, chroniclerApi: ChroniclerApi, scope: CoroutineScope, context: CoroutineContext = scope.coroutineContext) : BlaseballFeed("GLOBAL", blaseballApi, chroniclerApi, scope, context) {
        override suspend fun getFeed(limit: Int): List<BlaseballFeedEvent> = blaseballApi.getGlobalFeed(limit = limit)
    }

    private val feedFlow = MutableSharedFlow<BlaseballFeedEvent>()
    private val feedFlowByType: MutableMap<Int, MutableSharedFlow<BlaseballFeedEvent>> = HashMap()
    val flow: SharedFlow<BlaseballFeedEvent> by this::feedFlow

    fun flowByType(type: Int): SharedFlow<BlaseballFeedEvent> = feedFlowByType.computeIfAbsent(type) { MutableSharedFlow() }

    suspend abstract fun getFeed(limit: Int): List<BlaseballFeedEvent>

    @OptIn(ExperimentalTime::class)
    val collector = scope.launch(context) {
        val lastFile = File("$type.lastid")
        var lastID: FeedID? = if (lastFile.exists()) lastFile.readText().takeIf(String::isNotBlank)?.let(::FeedID) else null
        val totalFeed: MutableList<BlaseballFeedEvent> = ArrayList(100)

        loopEvery(5.seconds, `while` = { isActive }) {
            try {
                if (lastID == null) {
                    val feed = getFeed(limit = 100).asReversed()
                    emitAll(feed)
                    lastID = feed.lastOrNull()?.id ?: lastID
                } else {
                    var feed: List<BlaseballFeedEvent>
                    var limit = 50
                    do {
                        limit = limit shl 1
                        feed = getFeed(limit = limit).asReversed()
                    } while (limit < 2_000 && feed.subList(0, limit shr 1).none { event -> event.id != lastID })
                    val afterLastEvent = feed.takeLastWhile { event -> event.id != lastID }

                    emitAll(afterLastEvent)
                    lastID = afterLastEvent.lastOrNull()?.id ?: lastID
                }

                lastFile.writeText(lastID?.id ?: "")
            } catch (timeout: SocketTimeoutException) {
                timeout.printStackTrace()
                delay(5_000)
            }
        }
    }

    suspend fun emitAll(list: List<BlaseballFeedEvent>) = coroutineScope {
        launch { list.forEach { feedFlow.emit(it) } }
        launch {
            list.forEach {
                feedFlowByType.computeIfAbsent(it.type) { MutableSharedFlow() }.emit(it)
            }
        }
    }
}