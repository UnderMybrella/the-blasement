package dev.brella.blasement.client.jvm

import dev.brella.blasement.common.events.ClientEvent
import dev.brella.blasement.common.events.ServerEvent
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.BlaseballFeedEventType
import dev.brella.kornea.blaseball.EnumBlaseballItem
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
import kotlinx.coroutines.delay
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.security.MessageDigest
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.measureTimedValue
import kotlin.time.seconds

suspend fun WebSocketSession.sendEvent(format: SerialFormat, event: ClientEvent) {
    when (format) {
        is StringFormat -> send(format.encodeToString(event))
        is BinaryFormat -> send(format.encodeToByteArray(event))
        else -> throw IllegalArgumentException("Unknown format type ${format::class} (${format::class.java.interfaces.joinToString()}")
    }
}

@OptIn(ExperimentalTime::class)
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
        try {
            val clientelle = BlasementClientelle(json, client, blaseballApi, chroniclerApi, this)
            val better = clientelle.better

            println("Hi there! My name is ${better.name}")

            println("Begging: ${better.beg()}")
            println("Membership Card (Before: ${better.hasUnlockedShop}): ${better.purchaseShopMembershipCard()} (After: ${better.hasUnlockedShop})")
            repeat(8) { i ->
                println("Round ${i + 1}")
                println("Coins: ${better.coins}")
                println("hehe slushie time (Before: ${better.inventory[EnumBlaseballItem.SLUSHIE]}): ${better.purchase(1, EnumBlaseballItem.SLUSHIE)} (After: ${better.inventory[EnumBlaseballItem.SLUSHIE]})")
            }

            println("Coins: ${better.coins}")
            println("hehe corn time (Before: ${better.inventory[EnumBlaseballItem.POPCORN]}): ${better.purchase(1, EnumBlaseballItem.POPCORN)} (After: ${better.inventory[EnumBlaseballItem.POPCORN]})")

            println("Oh, what are these? ${better.purchase(1, EnumBlaseballItem.BREAD_CRUMBS)}")
            println("hehe beg time (Before: ${better.coins}): ${better.beg()} (After: ${better.coins})")


            clientelle.join()
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        }
    }
}