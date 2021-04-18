import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.beans.BlaseballFeedEvent
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

class TheBlasement(val json: Json, val httpClient: HttpClient, val blaseballApi: BlaseballApi, val chroniclerApi: ChroniclerApi) : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default

    val liveData = LiveData(blaseballApi, chroniclerApi, this)
    val globalFeed = BlaseballFeed.Global(this)

    /** Sub Event Feeds */

    private val gamesToday = ValueCache { date: BlaseballDate -> blaseballApi.getGamesByDate(date.season, date.day) }
    private val gamesTomorrow = ValueCache { date: BlaseballDate -> blaseballApi.getGamesByDate(date.season, date.day + 1) }

    suspend fun today(): BlaseballDate =
        liveData.date ?: blaseballApi.getSimulationData().run { BlaseballDate(season, day) }

    suspend fun gamesToday(): List<BlaseballDatabaseGame> =
        gamesToday.get(today())

    suspend fun gamesTomorrow(): List<BlaseballDatabaseGame> =
        gamesTomorrow.get(today())
}