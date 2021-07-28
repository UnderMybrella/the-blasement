package dev.brella.blasement.endpoints

import dev.brella.blasement.data.BlasementClock
import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getChroniclerVersionsBefore
import dev.brella.blasement.getIntOrNull
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getJsonObject
import dev.brella.blasement.getJsonObjectOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.blasement.loopEvery
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.cast
import dev.brella.kornea.errors.common.getOrBreak
import dev.brella.kornea.errors.common.successPooled
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.Clock
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.mapValuesTo
import kotlin.collections.set
import kotlin.collections.sortedWith
import kotlin.coroutines.coroutineContext
import kotlin.time.ExperimentalTime

//This is a bit of a misnomer honestly, since we're not setting up an 'endpoint'
interface BlaseballEventsStreamDataEndpoint {
    object Chronicler : BlaseballEventsStreamDataEndpoint {
        val WEATHER = arrayOf(
            "Void",
            "Sun 2",
            "Overcast",
            "Rainy",
            "Sandstorm",
            "Snowy",
            "Acidic",
            "Solar Eclipse",
            "Glitter",
            "Blooddrain",
            "Peanuts",
            "Birds",
            "Feedback",
            "Reverb",
            "Black Hole",
            "Coffee",
            "Coffee 2",
            "Coffee 3s",
            "Flooding",
            "Salmon",
            "Polarity +",
            "Polarity -",
            "???",
            "Sun 90",
            "Sun .1",
            "Sum Sun",
            "????",
            "????",
            "Jazz",
            "Night"
        )

        @OptIn(ExperimentalTime::class)
        override suspend fun setupFlow(league: BlasementLeague): SharedFlow<String> =
            flow<String> {
                //Chronicler is a bit funky with streamData sometimes, so we need to set up a base element, and then populate that
                val core: MutableMap<String, JsonElement> = HashMap()

                while (coroutineContext.isActive) {
                    try {
                        league.httpClient.getChroniclerVersionsBefore("stream", league.clock.getTime())
                            ?.forEach { streamData ->
                                streamData
                                    .getJsonObjectOrNull("value")
                                    ?.forEach { (k, v) -> core.putIfAbsent(k, v) }
                            } ?: continue

                        break
                    } catch (th: Throwable) {
                        th.printStackTrace()
                        continue
                    }
                }

                val stream = buildJsonObject {
                    put("value", JsonObject(core))
                }

                //loopEvery(league.updateInterval
                loopEvery(league.clock.eventStreamUpdateTime, { coroutineContext.isActive }) {
                    (league.httpClient.getChroniclerEntity("stream", league.clock.getTime()) as? JsonObject)
                        ?.getJsonObjectOrNull("value")
                        ?.forEach { (k, v) -> core[k] = v }

/*                    (core["fights"] as? JsonObject)
                        ?.let { fights ->
                            fights.getJsonArrayOrNull("bossFights")
                                ?.let { bossFights ->
                                    bossFights
                                        .firstOrNull()
                                        ?.let json@{ jsonElement ->
                                            val game = jsonElement as? JsonObject ?: return@json

                                            core["games"] = JsonObject((core["games"] as JsonObject) + Pair("schedule", buildJsonArray {
                                                val weather = (game.getIntOrNull("inning") ?: 0) % WEATHER.size
                                                println("Changed weather to $weather (${WEATHER[weather]})")
                                                add(JsonObject(game + arrayOf(
                                                    Pair("weather", JsonPrimitive(weather)),
                                                    Pair("awayTeam", JsonPrimitive("8d87c468-699a-47a8-b40d-cfb73a5660ad")) //Carcinisation
                                                )))
                                            }))

                                            //We need to do some other massaging to get this working

                                            val temporal = (core["temporal"] as JsonObject)
                                            val doc = temporal.getJsonObject("doc")

                                            core["temporal"] = JsonObject(temporal + Pair(
                                                "doc",
                                                 JsonObject(doc + Pair("gamma", JsonPrimitive(2)))
                                            ))
                                        }
                                }
                        }*/

                    emit(stream.deepSort().toString())
                }
            }.shareIn(league, SharingStarted.Eagerly, 1)

        override fun describe(): JsonElement? =
            JsonPrimitive("chronicler")

        private fun JsonElement.deepSort(): JsonElement =
            when (this) {
                is JsonObject -> JsonObject(mapValuesTo(TreeMap()) { (_, v) -> v.deepSort() })
                is JsonArray -> JsonArray(map { it.deepSort() }.sortedWith { a, b -> compare(a, b) })
                is JsonPrimitive -> this
            }

        private fun compare(a: JsonElement, b: JsonElement): Int =
            when (a) {
                is JsonObject -> {
                    val comparingA = a["name"] ?: a["fullName"] ?: a["homeTeam"] ?: a["id"] ?: JsonNull

                    when (b) {
                        is JsonObject -> compare(comparingA, b["name"] ?: b["fullName"] ?: b["homeTeam"] ?: b["id"] ?: JsonNull)
                        is JsonArray -> compare(comparingA, b.firstOrNull() ?: JsonNull)
                        is JsonPrimitive -> compare(comparingA, b)
                    }
                }
                is JsonArray -> when (b) {
                    is JsonObject -> compare(a.firstOrNull() ?: JsonNull, b["name"] ?: b["fullName"] ?: b["homeTeam"] ?: b["id"] ?: JsonNull)
                    is JsonArray -> compare(a.firstOrNull() ?: JsonNull, b.firstOrNull() ?: JsonNull)
                    is JsonPrimitive -> compare(a.firstOrNull() ?: JsonNull, b)
                }
                is JsonPrimitive -> when (b) {
                    is JsonObject -> compare(a, b["name"] ?: b["fullName"] ?: b["homeTeam"] ?: b["id"] ?: JsonNull)
                    is JsonArray -> compare(a, b.firstOrNull() ?: JsonNull)
                    is JsonPrimitive -> a.content.compareTo(b.content)
                }
            }
    }

    object Live : BlaseballEventsStreamDataEndpoint {
        override suspend fun setupFlow(league: BlasementLeague): SharedFlow<String> {
            val session = league.httpClient.webSocketSession {
                url("wss://api.sibr.dev/corsmechanics/www.blaseball.com/events/streamSocket")

                println(url.protocol)
            }

            return session.incoming
                .consumeAsFlow()
                .mapNotNull { frame -> (frame as? Frame.Text)?.readText() }
                .onCompletion { session.close() }
                .shareIn(session, SharingStarted.Eagerly, 1)
        }

        override fun describe(): JsonElement? =
            JsonPrimitive("live")
    }

    data class ChroniclerAtTime(val clock: BlasementClock) : BlaseballEventsStreamDataEndpoint {
        @OptIn(ExperimentalTime::class)
        override suspend fun setupFlow(league: BlasementLeague): SharedFlow<String> =
            flow<String> {
                //Chronicler is a bit funky with streamData sometimes, so we need to set up a base element, and then populate that
                val core: MutableMap<String, JsonElement> = HashMap()

                while (coroutineContext.isActive) {
                    try {
                        league.httpClient.getChroniclerVersionsBefore("stream", clock.getTime())
                            ?.forEach { streamData ->
                                streamData
                                    .getJsonObjectOrNull("value")
                                    ?.forEach { (k, v) -> core.putIfAbsent(k, v) }
                            } ?: continue

                        break
                    } catch (th: Throwable) {
                        th.printStackTrace()
                        continue
                    }
                }

                val stream = buildJsonObject {
                    put("value", JsonObject(core))
                }

                //loopEvery(league.updateInterval
                loopEvery(clock.eventStreamUpdateTime, { coroutineContext.isActive }) {
                    (league.httpClient.getChroniclerEntity("stream", clock.getTime()) as? JsonObject)
                        ?.getJsonObjectOrNull("value")
                        ?.forEach { (k, v) -> core[k] = v }

                    emit(stream.deepSort().toString())
                }
            }.shareIn(league, SharingStarted.Eagerly, 1)

        override fun describe(): JsonElement? =
            buildJsonObject {
                put("type", "chronicler_at_time")
                put("clock", clock.describe() ?: JsonNull)
            }

        private fun JsonElement.deepSort(): JsonElement =
            when (this) {
                is JsonObject -> JsonObject(mapValuesTo(TreeMap()) { (_, v) -> v.deepSort() })
                is JsonArray -> JsonArray(map { it.deepSort() }.sortedWith { a, b -> compare(a, b) })
                is JsonPrimitive -> this
            }

        private fun compare(a: JsonElement, b: JsonElement): Int =
            when (a) {
                is JsonObject -> {
                    val comparingA = a["name"] ?: a["fullName"] ?: a["homeTeam"] ?: a["id"] ?: JsonNull

                    when (b) {
                        is JsonObject -> compare(comparingA, b["name"] ?: b["fullName"] ?: b["homeTeam"] ?: b["id"] ?: JsonNull)
                        is JsonArray -> compare(comparingA, b.firstOrNull() ?: JsonNull)
                        is JsonPrimitive -> compare(comparingA, b)
                    }
                }
                is JsonArray -> when (b) {
                    is JsonObject -> compare(a.firstOrNull() ?: JsonNull, b["name"] ?: b["fullName"] ?: b["homeTeam"] ?: b["id"] ?: JsonNull)
                    is JsonArray -> compare(a.firstOrNull() ?: JsonNull, b.firstOrNull() ?: JsonNull)
                    is JsonPrimitive -> compare(a.firstOrNull() ?: JsonNull, b)
                }
                is JsonPrimitive -> when (b) {
                    is JsonObject -> compare(a, b["name"] ?: b["fullName"] ?: b["homeTeam"] ?: b["id"] ?: JsonNull)
                    is JsonArray -> compare(a, b.firstOrNull() ?: JsonNull)
                    is JsonPrimitive -> a.content.compareTo(b.content)
                }
            }
    }

    data class Static(val data: JsonElement?) : BlaseballEventsStreamDataEndpoint {
        override suspend fun setupFlow(league: BlasementLeague): SharedFlow<String> =
            flow { emit(data.toString()) }
                .shareIn(league, SharingStarted.Lazily, 1)

        override fun describe(): JsonElement =
            buildJsonObject {
                put("type", "static")
                put("data", data ?: JsonNull)
            }
    }

    suspend fun setupFlow(league: BlasementLeague): SharedFlow<String>
    fun describe(): JsonElement?

    companion object {
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballEventsStreamDataEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Chronicler
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler
                            "live" -> Live
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler
                            "chronicler_at_time" -> ChroniclerAtTime(BlasementClock.loadFrom(config["clock"], System.currentTimeMillis(), Clock.systemUTC()).getOrBreak { return it.cast() })
                            "live" -> Live
                            "static" -> Static(config["data"])
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }
}