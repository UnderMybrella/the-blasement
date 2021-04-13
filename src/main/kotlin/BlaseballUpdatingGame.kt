import dev.brella.kornea.blaseball.beans.BlaseballStreamDataSchedule
import dev.brella.kornea.toolkit.coroutines.ReadWriteSemaphore
import dev.brella.kornea.toolkit.coroutines.withReadPermit
import dev.brella.kornea.toolkit.coroutines.withWritePermit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.roundToInt

class BlaseballUpdatingGame {
    val semaphore = ReadWriteSemaphore(16)
    private var gameUpdates: Array<BlaseballStreamDataSchedule?> = arrayOfNulls(0)

    private val _updateLog: MutableSharedFlow<BlaseballStreamDataSchedule> = MutableSharedFlow()
    val updateLog: SharedFlow<BlaseballStreamDataSchedule>
        get() = _updateLog

    suspend fun getUpdates(): Array<BlaseballStreamDataSchedule?> = semaphore.withReadPermit { gameUpdates.copyOf() }

    suspend fun issueUpdate(schedule: BlaseballStreamDataSchedule) {
        semaphore.withReadPermit { if (schedule.playCount in gameUpdates.indices && gameUpdates[schedule.playCount] != null) return }


        val emitted = semaphore.withWritePermit {
            val emitted = _updateLog.tryEmit(schedule)

            if (schedule.playCount >= gameUpdates.size) {
                //Grow the array
                val newArray = arrayOfNulls<BlaseballStreamDataSchedule>(1 + (schedule.playCount * 1.5).roundToInt())
                gameUpdates.copyInto(newArray)
                gameUpdates = newArray
            }

            gameUpdates[schedule.playCount] = schedule

            return@withWritePermit emitted
        }

        if (!emitted) _updateLog.emit(schedule)
    }
}