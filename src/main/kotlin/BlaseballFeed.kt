import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.FeedID
import dev.brella.kornea.blaseball.beans.BlaseballFeedEvent
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

class BlaseballFeed(val blaseballApi: BlaseballApi, val chroniclerApi: ChroniclerApi, val scope: CoroutineScope, val context: CoroutineContext = scope.coroutineContext) {
    val globalFeedFlow = MutableSharedFlow<BlaseballFeedEvent>()
    val collector = scope.launch(context) {
        val lastFile = File(".lastid")
        var lastID: FeedID? = if (lastFile.exists()) lastFile.readText().takeIf(String::isNotBlank)?.let(::FeedID) else null
        val totalFeed: MutableList<BlaseballFeedEvent> = ArrayList(100)

        while (isActive) {
            if (lastID == null) {
                val feed = blaseballApi.getGlobalFeed(limit = 100).asReversed()
                feed.forEach { globalFeedFlow.emit(it) }
                lastID = feed.lastOrNull()?.id ?: lastID
            } else {
                var feed: List<BlaseballFeedEvent>
                var limit = 50
                do {
                    limit = limit shl 1
                    feed = blaseballApi.getGlobalFeed(limit = limit).asReversed()
                } while (limit < 2_000 && feed.subList(0, limit shr 1).none { event -> event.id != lastID })
                val afterLastEvent = feed.takeLastWhile { event -> event.id != lastID }

                afterLastEvent.forEach { globalFeedFlow.emit(it) }
                lastID = afterLastEvent.lastOrNull()?.id ?: lastID
            }

            delay(5_000 - measureTimeMillis { lastFile.writeText(lastID?.id ?: "") })
        }
    }
}