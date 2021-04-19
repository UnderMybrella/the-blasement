import dev.brella.kornea.toolkit.coroutines.ReadWriteSemaphore
import kotlinx.coroutines.delay
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
suspend fun <T> T.loopEvery(time: Duration, `while`: suspend T.() -> Boolean, block: suspend () -> Unit) {
    while (`while`()) {
        val timeTaken = measureTime { block() }
//        println("Took ${timeTaken.inSeconds}s, waiting ${(time - timeTaken).inSeconds}s")
        delay((time - timeTaken).toLongMilliseconds().coerceAtLeast(0L))
    }
}

/**
 * Executes the given [action], releasing a read permit from this semaphore at the beginning and acquiring it after
 * the [action] is completed.
 *
 * @return the return value of the [action].
 */
@OptIn(ExperimentalContracts::class)
public suspend inline fun <T> ReadWriteSemaphore.loanReadPermit(action: () -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }

    releaseReadPermit()
    try {
        return action()
    } finally {
        acquireReadPermit()
    }
}