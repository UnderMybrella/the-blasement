package dev.brella.blasement.client.jvm

import dev.brella.blasement.common.events.ClientEvent
import dev.brella.blasement.common.events.ServerEvent
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import io.ktor.client.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.replay
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString

class BlasementClientelle(val format: SerialFormat, val httpClient: HttpClient, val blaseballApi: BlaseballApi, val chroniclerApi: ChroniclerApi, val websocket: WebSocketSession) {
    val incomingFrames = websocket.incoming.receiveAsFlow().shareIn(websocket, SharingStarted.Eagerly)

    val incomingEvents = incomingFrames.map { frame ->
        val event: ServerEvent = when (format) {
            is StringFormat -> {
                when (frame.frameType) {
                    FrameType.TEXT -> format.decodeFromString(frame.readBytes().decodeToString())
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

    val better = BlasementClientBetter(this)

    suspend fun receive(event: ServerEvent) {

    }

    suspend fun send(event: ClientEvent) {
        when (format) {
            is StringFormat -> websocket.send(format.encodeToString(event))
            is BinaryFormat -> websocket.send(format.encodeToByteArray(event))
            else -> throw IllegalArgumentException("Unknown format type ${format::class} (${format::class.java.interfaces.joinToString()}")
        }
    }

    suspend fun join() {
        serverEventJobs.join()
    }
}