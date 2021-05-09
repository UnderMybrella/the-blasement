package websocket

import BlasementHostFan
import TheBlasement
import dev.brella.blasement.HashCashToken
import dev.brella.blasement.common.events.ClientEvent
import dev.brella.blasement.common.events.EnumBetFail
import dev.brella.blasement.common.events.ServerEvent
import dev.brella.blasement.common.events.toPayload
import dev.brella.kornea.blaseball.base.common.BettingPayouts
import dev.brella.kornea.errors.common.doOnSuccess
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import java.time.Clock
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext


import java.util.UUID as JUUID
import dev.brella.kornea.blaseball.base.common.UUID as KUUID

class BlasementDweller(val blasement: TheBlasement, val format: SerialFormat, val websocket: DefaultWebSocketServerSession) {
    companion object : CoroutineScope {
        override val coroutineContext: CoroutineContext = Executors.newCachedThreadPool().asCoroutineDispatcher()
    }

    var fan: BlasementHostFan? = null
    var fanJob: Job? = null
    val incoming = websocket.incoming.receiveAsFlow().map { frame ->
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

        return@map event
    }.shareIn(websocket, SharingStarted.Eagerly)

    val receivingJob = incoming.onEach(this::onMessage).launchIn(websocket)

    suspend fun onMessage(event: ClientEvent) {
        println("Received $event from $websocket")

        when (event) {
            is ClientEvent.SubscribeToGame -> blasement.liveData.getLocalGame(event.game)
                ?.updateLog
                ?.onEach { schedule -> sendEvent(ServerEvent.GameUpdate(schedule)) }
                ?.launchIn(websocket)

//            is ClientEvent.SubscribeToGlobalFeed -> blasement.globalFeed
//                .flow
//                .onEach { feedEvent -> sendEvent(ServerEvent.GlobalFeedEvent(feedEvent)) }
//                .launchIn(websocket)
//
//            is ClientEvent.SubscribeToGlobalFeedEvents ->
//                event.types.forEach { type ->
//                    blasement.globalFeed
//                        .flowByType(type)
//                        .onEach { sendEvent(ServerEvent.GlobalFeedEvent(it)) }
//                        .launchIn(websocket)
//                }

            is ClientEvent.GetDate -> sendEvent(blasement.today().let { (season, day) -> ServerEvent.CurrentDate(season, day) })
            is ClientEvent.GetTodaysGames -> sendEvent(blasement.today().let { (season, day) -> ServerEvent.GameList(season, day, blasement.gamesToday()) })
            is ClientEvent.GetTomorrowsGames -> sendEvent(blasement.today().let { (season, day) -> ServerEvent.GameList(season, day + 1, blasement.gamesTomorrow()) })

            is ClientEvent.GetSelf -> sendEvent(fan?.let { ServerEvent.FanPayload(it.toPayload()) } ?: ServerEvent.Unauthenticated)

            is ClientEvent.CreateNewFan -> {
                val token = HashCashToken(event.hashcashToken)
                val now = ZonedDateTime.now(Clock.systemUTC())
                val dateTime = "${now.year.toString().takeLast(2)}${now.monthValue.toString().takeLast(2)}${now.dayOfMonth.toString().takeLast(2)}"

                if (!token.date.startsWith(dateTime)) return sendEvent(ServerEvent.FanCreationTooExpensive)

                if (token.res != event.name) return sendEvent(ServerEvent.FanCreationTooExpensive)

                val value = token.value()

                if (value < 16) return sendEvent(ServerEvent.FanCreationTooExpensive)

                val (authToken, newFan) = blasement.newFan(JUUID.randomUUID(), event.name, event.favouriteTeam)

                this.fanJob?.cancel()

                this.fan = newFan
                this.fanJob = newFan.fanEvents.onEach(this::sendEvent).launchIn(websocket)

                sendEvent(ServerEvent.FanCreated(newFan.id, authToken, newFan.toPayload()))

//                val authToken =
//                val fan = newBlaseballGal(name = event.name, favouriteTeam = event.favouriteTeam)
//
//                this.fan = fan
//                blasement.fans.add(fan)
//
//                sendEvent(ServerEvent.FanCreated(fan.id, authToken, fan.toPayload()))
            }

            is ClientEvent.AuthenticateFan -> {
                val jwt = runCatching { blasement.parser.parseClaimsJws(event.authToken) }
                if (jwt.isFailure) sendEvent(ServerEvent.InvalidAuthToken)
                else {
                    val jwt = jwt.getOrThrow()
                    val fanID = jwt.body.id
                    val fan = blasement.fans[KUUID.fromString(fanID)] ?: return sendEvent(ServerEvent.InvalidAuthToken)
                    println("Authenticated w/ $fan")

                    this.fanJob?.cancel()

                    this.fan = fan
                    this.fanJob = fan.fanEvents.onEach(this::sendEvent).launchIn(websocket)

                    sendEvent(ServerEvent.FanPayload(fan.toPayload()))
                }
            }

            is ClientEvent.BettingReturns -> {
                blasement.blaseballApi.getGameById(event.onGame).doOnSuccess { game ->
                    if (game.homeTeam == event.onTeam) sendEvent(ServerEvent.BettingReturns(BettingPayouts.currentSeason(event.amount, game.homeOdds)))
                    else if (game.awayTeam == event.onTeam) sendEvent(ServerEvent.BettingReturns(BettingPayouts.currentSeason(event.amount, game.awayOdds)))
                    else sendEvent(ServerEvent.BettingReturns(null))
                }
            }

            is ClientEvent.PerformFanAction -> {
                println("Performing Better Action: $event")

                val better = this.fan

                if (better == null) {
                    sendEvent(ServerEvent.Unauthenticated)
                } else {
                    when (event) {
                        is ClientEvent.PerformFanAction.Beg -> sendEvent(ServerEvent.FanActionResponse.Beg(better.beg()))
                        is ClientEvent.PerformFanAction.PurchaseMembershipCard -> sendEvent(ServerEvent.FanActionResponse.PurchaseShopMembershipCard(better.purchaseShopMembershipCard()))
                        is ClientEvent.PerformFanAction.PurchaseItem -> sendEvent(ServerEvent.FanActionResponse.PurchaseItem(better.buySnack(event.amount, event.item)))
                        is ClientEvent.PerformFanAction.SellItem -> sendEvent(ServerEvent.FanActionResponse.SoldItem(better.sell(event.amount, event.item)))
                        is ClientEvent.PerformFanAction.PurchaseSlot -> sendEvent(ServerEvent.FanActionResponse.PurchaseSlot(better.purchaseSlot(event.amount)))
                        is ClientEvent.PerformFanAction.SellSlot -> sendEvent(ServerEvent.FanActionResponse.SoldSlot(better.sellSlot(event.amount)))
                        is ClientEvent.PerformFanAction.PlaceBet -> {
                            blasement.blaseballApi.getGameById(event.onGame).doOnSuccess { game ->
                                when {
                                    game.homeTeam == event.onTeam -> sendEvent(ServerEvent.FanActionResponse.BetOnGame(BettingPayouts.currentSeason(event.amount, game.homeOdds), better.placeBet(event.onGame, event.onTeam, event.amount)))
                                    game.awayTeam == event.onTeam -> sendEvent(ServerEvent.FanActionResponse.BetOnGame(BettingPayouts.currentSeason(event.amount, game.awayOdds), better.placeBet(event.onGame, event.onTeam, event.amount)))

                                    else -> sendEvent(ServerEvent.FanActionResponse.BetOnGame(null, EnumBetFail.INVALID_TEAM))
                                }
                            }
                        }

                        else -> {
                        }
                    }
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