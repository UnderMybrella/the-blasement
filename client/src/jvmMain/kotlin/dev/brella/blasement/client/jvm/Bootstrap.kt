package dev.brella.blasement.client.jvm

import dev.brella.blasement.common.events.ClientEvent
import dev.brella.blasement.common.events.ServerEvent
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.BlaseballFeedEventType
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import dev.brella.ktornea.common.installGranularHttp
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.compression.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.system.measureTimeMillis

suspend fun WebSocketSession.sendEvent(format: SerialFormat, event: ClientEvent) {
    when (format) {
        is StringFormat -> send(format.encodeToString(event))
        is BinaryFormat -> send(format.encodeToByteArray(event))
        else -> throw IllegalArgumentException("Unknown format type ${format::class} (${format::class.java.interfaces.joinToString()}")
    }
}

suspend fun main() {
    val json = Json { }

    val client = HttpClient(OkHttp) {
        installGranularHttp()

        install(ContentEncoding) {
            gzip()
            deflate()
            identity()
        }

        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }

        WebSockets {
            pingInterval = 60_000L
        }

        expectSuccess = false

        defaultRequest {
            userAgent("Mozilla/5.0 (X11; Linux x86_64; rv:85.0) Gecko/20100101 Firefox/85.0")
        }
    }

    val blaseballApi = BlaseballApi(client)
    val chroniclerApi = ChroniclerApi(client)

    client.webSocket("ws://localhost:9897/connect") {
        val format = json

        val receivingJob = incoming.receiveAsFlow().onEach { frame ->
            val event: ServerEvent = when (format) {
                is StringFormat -> {
                    when (frame.frameType) {
                        FrameType.TEXT -> format.decodeFromString(frame.readBytes().decodeToString().also { str -> println("Received [$str]") })
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

            println(event)
        }.launchIn(this)

        val simData = blaseballApi.getSimulationData()
        val latestGames = blaseballApi.getGamesByDate(simData.season, simData.day)
        sendEvent(json, ClientEvent.SubscribeToGlobalFeedEvents(BlaseballFeedEventType.HIT))

        receivingJob.join()
    }
}