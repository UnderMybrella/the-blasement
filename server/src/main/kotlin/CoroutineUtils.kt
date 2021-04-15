import kotlinx.coroutines.delay
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
suspend fun <T> T.loopEvery(time: Duration, `while`: suspend T.() -> Boolean, block: suspend () -> Unit) {
    while (`while`()) {
        val timeTaken = measureTime { block() }
//        println("Took ${timeTaken.inSeconds}s, waiting ${(time - timeTaken).inSeconds}s")
        delay(time - timeTaken)
    }
}