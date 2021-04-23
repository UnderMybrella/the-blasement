package dev.brella.blasement.client.jvm

import dev.brella.blasement.HashCashToken
import dev.brella.blasement.common.events.BlasementFan
import dev.brella.blasement.common.events.ClientEvent
import dev.brella.blasement.common.events.ServerEvent
import dev.brella.blasement.common.events.doThenWaitFor
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import io.ktor.client.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString

class BlasementClientele(val format: SerialFormat, val httpClient: HttpClient, val blaseballApi: BlaseballApi, val chroniclerApi: ChroniclerApi, val websocket: WebSocketSession) {
    val incomingFrames = websocket.incoming.receiveAsFlow().shareIn(websocket, SharingStarted.Eagerly)

    val debug: Boolean = false

    inline fun debugText(str: String): String {
        if (debug) println("Incoming JSON: $str")
        return str
    }

    val incomingEvents = incomingFrames.map { frame ->
        val event: ServerEvent = when (format) {
            is StringFormat -> {
                when (frame.frameType) {
                    FrameType.TEXT -> format.decodeFromString(debugText(frame.readBytes().decodeToString()))
                    FrameType.BINARY -> {
                        println("WARN: Received binary in $frame; could be compressed text?")
                        format.decodeFromString(frame.readBytes().decodeToString())
                    }
                    else -> throw IllegalStateException("Cannot parse text from frame type ${frame.frameType} for $frame")
                }
            }

            is BinaryFormat ->
                if (frame.frameType == FrameType.BINARY) format.decodeFromByteArray(frame.readBytes())
                else throw IllegalStateException("Cannot parse binary data from frame type ${frame.frameType} for $frame")

            else -> throw IllegalArgumentException("Unknown format type ${format::class} (${format::class.java.interfaces.joinToString()}")
        }

        return@map event
    }

    val serverEventJobs = incomingEvents.onEach(this::receive).launchIn(websocket)

    var fan: BlasementClientFan? = null

    suspend fun receive(event: ServerEvent) {
        if (debug) println("Event: $event")

        when (event) {
            is ServerEvent.FanActionResponse.WonBet -> {
                println("Won Bet!")
                fan?.apply {
                    currentBets.remove(event.onGame)
                    coins += event.returns
                }
            }

            is ServerEvent.FanActionResponse.LostBet -> {
                println("Lost Bet :(")
                fan?.currentBets?.remove(event.onGame)
            }

            is ServerEvent.FanActionResponse.GainedMoney -> {
                fan?.apply {
                    println("Gained Coins! (${coins} -> ${coins + event.amount}")
                    coins += event.amount
                }
            }

            else -> {}
        }
    }

    suspend fun send(event: ClientEvent) {
        when (format) {
            is StringFormat -> websocket.send(format.encodeToString(event))
            is BinaryFormat -> websocket.send(format.encodeToByteArray(event))
            else -> throw IllegalArgumentException("Unknown format type ${format::class} (${format::class.java.interfaces.joinToString()}")
        }
    }

    suspend fun authenticate(authToken: String): BlasementFan? =
        withTimeout(10_000) {
            val event = incomingEvents.doThenWaitFor({ send(ClientEvent.AuthenticateFan(authToken)) }) { it is ServerEvent.InvalidAuthToken || it is ServerEvent.FanPayload }

            if (event !is ServerEvent.FanPayload) return@withTimeout null

            println("Authenticated Fan: $event")
            fan = event.payload.toClient(this@BlasementClientele)

            return@withTimeout fan
        }

    suspend fun createNewUser(name: String, favouriteTeam: TeamID): BlasementFan? = withTimeout(10_000) {
        val event = incomingEvents.doThenWaitFor({ send(ClientEvent.CreateNewFan(HashCashToken.generate(name, 16).encode(), name, favouriteTeam)) }) { it is ServerEvent.FanCreationTooExpensive || it is ServerEvent.FanCreated }

        if (event !is ServerEvent.FanCreated) return@withTimeout null

        println("Created Fan: $event")
        fan = event.payload.toClient(this@BlasementClientele)
        return@withTimeout fan
    }

    suspend fun join() {
        serverEventJobs.join()
    }
}