package websocket

import TheBlasement
import dev.brella.blasement.common.events.BlasementBetter
import dev.brella.blasement.common.events.ClientEvent
import dev.brella.blasement.common.events.ServerEvent
import dev.brella.blasement.common.events.toPayload
import dev.brella.kornea.blaseball.beans.BlaseballTeam
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import newBlaseballGal
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class BlasementDweller(val blasement: TheBlasement, val format: SerialFormat, val websocket: DefaultWebSocketServerSession) {
    companion object : CoroutineScope {
        override val coroutineContext: CoroutineContext = Executors.newCachedThreadPool().asCoroutineDispatcher()
        var teams: List<BlaseballTeam> = emptyList()
    }

    lateinit var better: BlasementBetter
    val teamsJob = launch {
        if (teams.isEmpty()) {
            teams = blasement.blaseballApi.getAllTeams()
        }

        better = newBlaseballGal(name = "Zanna Testingbird", favouriteTeam = teams.random().id)
    }

    val receivingJob = websocket.incoming.receiveAsFlow().onEach(this::onMessage).launchIn(websocket)


    suspend fun onMessage(frame: Frame) {
        teamsJob.join()

        val event: ClientEvent = when (format) {
            is StringFormat -> {
                when (frame.frameType) {
                    FrameType.TEXT -> format.decodeFromString(frame.readBytes().decodeToString().also { str -> println("Received [$str]") })
                    FrameType.BINARY -> {
                        println("WARN: Received binary from $websocket in $frame; could be compressed text?")
                        format.decodeFromString(frame.readBytes().decodeToString())
                    }
                    else -> throw IllegalStateException("Cannot parse text from frame type ${frame.frameType} for $frame in $websocket")
                }
            }

            is BinaryFormat ->
                if (frame.frameType == FrameType.BINARY) format.decodeFromByteArray(frame.readBytes())
                else throw IllegalStateException("Cannot parse binary data from frame type ${frame.frameType} for $frame in $websocket")

            else -> throw IllegalArgumentException("Unknown format type ${format::class} (${format::class.java.interfaces.joinToString()}")
        }

        println("Received $event from $websocket")

        when (event) {
            is ClientEvent.SubscribeToGame -> blasement.liveData.getLocalGame(event.game)
                ?.updateLog
                ?.onEach { schedule -> sendEvent(ServerEvent.GameUpdate(schedule)) }
                ?.launchIn(websocket)

            is ClientEvent.SubscribeToGlobalFeed -> blasement.globalFeed
                .flow
                .onEach { feedEvent -> sendEvent(ServerEvent.GlobalFeedEvent(feedEvent)) }
                .launchIn(websocket)

            is ClientEvent.SubscribeToGlobalFeedEvents ->
                event.types.forEach { type ->
                    blasement.globalFeed
                        .flowByType(type)
                        .onEach { sendEvent(ServerEvent.GlobalFeedEvent(it)) }
                        .launchIn(websocket)
                }

            is ClientEvent.GetDate -> sendEvent(blasement.today().let { (season, day) -> ServerEvent.CurrentDate(season, day) })
            is ClientEvent.GetTodaysGames -> sendEvent(blasement.today().let { (season, day) -> ServerEvent.GameList(season, day, blasement.gamesToday()) })
            is ClientEvent.GetTomorrowsGames -> sendEvent(blasement.today().let { (season, day) -> ServerEvent.GameList(season, day + 1, blasement.gamesTomorrow()) })

            is ClientEvent.GetBetter -> sendEvent(ServerEvent.BetterPayload(better.toPayload()))

            is ClientEvent.PerformBetterAction -> {
                println("Performing Better Action: ${event}")

                when (event) {
                    is ClientEvent.PerformBetterAction.Beg -> sendEvent(ServerEvent.BetterActionResponse.Beg(better.beg()))
                    is ClientEvent.PerformBetterAction.PurchaseMembershipCard -> sendEvent(ServerEvent.BetterActionResponse.PurchaseShopMembershipCard(better.purchaseShopMembershipCard()))
                    is ClientEvent.PerformBetterAction.PurchaseItem -> sendEvent(ServerEvent.BetterActionResponse.PurchaseItem(better.purchase(event.amount, event.item)))
                }
            }

            else -> println("Unknown event $event")
        }
    }

    suspend fun join() {
        receivingJob.join()
    }

    suspend fun sendEvent(event: ServerEvent) {
        when (format) {
            is StringFormat -> websocket.send(format.encodeToString(event))
            is BinaryFormat -> websocket.send(format.encodeToByteArray(event))
            else -> throw IllegalArgumentException("Unknown format type ${format::class} (${format::class.java.interfaces.joinToString()}")
        }
    }
}