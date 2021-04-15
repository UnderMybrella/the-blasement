import dev.brella.kornea.toolkit.common.UNINITIALISED_VALUE
import dev.brella.kornea.toolkit.coroutines.ReadWriteSemaphore
import dev.brella.kornea.toolkit.coroutines.withReadPermit
import dev.brella.kornea.toolkit.coroutines.withWritePermit
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class ValueCache<T, V>(val semaphore: ReadWriteSemaphore = ReadWriteSemaphore(16), val process: suspend (key: T) -> V) {
    constructor(permitCount: Int, process: suspend (key: T) -> V): this(ReadWriteSemaphore(permitCount), process)

    private var lastCache: Pair<T, V>? = null

    suspend fun get(value: T): V = semaphore.withReadPermit {
        val lastCache = lastCache
        if (lastCache == null || lastCache.first != value) {
            semaphore.releaseReadPermit()

            return@withReadPermit try {
                semaphore.withWritePermit {
                    println("Hitting cache!")
                    val result = process(value)
                    this.lastCache = Pair(value, result)
                    result
                }
            } finally {
                semaphore.acquireReadPermit()
            }
        } else {
            lastCache.second
        }
    }
}