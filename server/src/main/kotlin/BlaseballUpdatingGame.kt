
import dev.brella.kornea.blaseball.base.common.beans.BlaseballStreamDataGame
import dev.brella.kornea.toolkit.coroutines.ReadWriteSemaphore
import dev.brella.kornea.toolkit.coroutines.withReadPermit
import dev.brella.kornea.toolkit.coroutines.withWritePermit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.roundToInt

class BlaseballUpdatingGame {
    val semaphore = ReadWriteSemaphore(16)
    private var gameUpdates: Array<BlaseballStreamDataGame?> = arrayOfNulls(0)

    private val _updateLog: MutableSharedFlow<BlaseballStreamDataGame> = MutableSharedFlow()
    val updateLog: SharedFlow<BlaseballStreamDataGame>
        get() = _updateLog

    suspend fun getUpdates(): Array<BlaseballStreamDataGame?> = semaphore.withReadPermit { gameUpdates.copyOf() }

    suspend fun issueUpdate(schedule: BlaseballStreamDataGame) {
        semaphore.withReadPermit { if (schedule.playCount in gameUpdates.indices && gameUpdates[schedule.playCount] != null) return }

        val emitted = semaphore.withWritePermit {
            val emitted = _updateLog.tryEmit(schedule)

            if (schedule.playCount >= gameUpdates.size) {
                //Grow the array
                val newArray = arrayOfNulls<BlaseballStreamDataGame>(1 + (schedule.playCount * 1.5).roundToInt())
                gameUpdates.copyInto(newArray)
                gameUpdates = newArray
            }

            gameUpdates[schedule.playCount] = schedule

            return@withWritePermit emitted
        }

        if (!emitted) _updateLog.emit(schedule)
    }
}