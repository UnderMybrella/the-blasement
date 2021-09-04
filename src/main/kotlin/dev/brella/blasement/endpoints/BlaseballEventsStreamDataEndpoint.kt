package dev.brella.blasement.endpoints

import dev.brella.blasement.data.BlasementClock
import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.getBooleanOrNull
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getChroniclerVersionsBefore
import dev.brella.blasement.getIntOrNull
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getJsonObjectOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.blasement.loopEvery
import dev.brella.blasement.plugins.json
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.cast
import dev.brella.kornea.errors.common.getOrBreak
import dev.brella.kornea.errors.common.getOrNull
import dev.brella.kornea.errors.common.successPooled
import io.ktor.application.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.Clock
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.time.ExperimentalTime

//This is a bit of a misnomer honestly, since we're not setting up an 'endpoint'
interface BlaseballEventsStreamDataEndpoint : BlaseballEndpoint {
    data class Chronicler(val transformers: List<JsonTransformer> = emptyList()) : BlaseballEventsStreamDataEndpoint {
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

                while (currentCoroutineContext().isActive) {
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
                loopEvery(league.clock.eventStreamUpdateTime, { currentCoroutineContext().isActive }) {
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

                    emit(transformers(stream.deepSort()).toString())
                }
            }.shareIn(league, SharingStarted.Eagerly, 1)

        override fun describe(): JsonElement =
            buildJsonObject {
                put("type", "chronicler")
                put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
            }
    }

    data class Live(val transformers: List<JsonTransformer> = emptyList()) : BlaseballEventsStreamDataEndpoint {
        override suspend fun setupFlow(league: BlasementLeague): SharedFlow<String> {
            val session = league.httpClient.webSocketSession {
                url("wss://api.sibr.dev/corsmechanics/www.blaseball.com/events/streamSocket")
            }

            return session.incoming
                .consumeAsFlow()
                .mapNotNull { frame -> transformers((frame as? Frame.Text)?.readText()?.let(json::parseToJsonElement) ?: JsonNull).toString() }
                .onCompletion { session.close() }
                .shareIn(session, SharingStarted.Eagerly, 1)
        }

        override fun describe(): JsonElement =
            buildJsonObject {
                put("type", "live")
                put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
            }
    }

    data class ChroniclerAtTime(val clock: BlasementClock, val transformers: List<JsonTransformer> = emptyList()) : BlaseballEventsStreamDataEndpoint {
        @OptIn(ExperimentalTime::class)
        override suspend fun setupFlow(league: BlasementLeague): SharedFlow<String> =
            flow<String> {
                //Chronicler is a bit funky with streamData sometimes, so we need to set up a base element, and then populate that
                val core: MutableMap<String, JsonElement> = HashMap()

                while (currentCoroutineContext().isActive) {
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
                loopEvery(clock.eventStreamUpdateTime, { currentCoroutineContext().isActive }) {
                    (league.httpClient.getChroniclerEntity("stream", clock.getTime()) as? JsonObject)
                        ?.getJsonObjectOrNull("value")
                        ?.forEach { (k, v) -> core[k] = v }

                    emit(transformers(stream.deepSort()).toString())
                }
            }.shareIn(league, SharingStarted.Eagerly, 1)

        override fun describe(): JsonElement? =
            buildJsonObject {
                put("type", "chronicler_at_time")
                put("clock", clock.describe() ?: JsonNull)
                put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
            }
    }

    data class Static(var data: JsonElement?, var index: Int = 0, val loopOnCompletion: Boolean = false, val transformers: List<JsonTransformer> = emptyList()) : BlaseballEventsStreamDataEndpoint, BlaseballUpdatableEndpoint {
        override suspend fun setupFlow(league: BlasementLeague): SharedFlow<String> =
            flow<String> {
                loopEvery(league.clock.eventStreamUpdateTime, { currentCoroutineContext().isActive }) {
                    data.let {
                        if (it is JsonArray) {
                            emit(it.getOrNull(if (loopOnCompletion) index++ % it.size else index++)?.deepSort()?.let(transformers::invoke)?.toString() ?: return@loopEvery)
                        } else {
                            emit(it?.deepSort()?.let(transformers::invoke)?.toString() ?: return@loopEvery)
                        }
                    }
                }
            }.shareIn(league, SharingStarted.Eagerly, 1)

        override suspend fun updateDataFor(league: BlasementLeague, call: ApplicationCall) {
            val append = call.request.queryParameters["append"]?.toBooleanStrictOrNull() ?: false
            val fold = call.request.queryParameters["fold"]?.toBooleanStrictOrNull() ?: false
            val newData = call.receiveOrNull<JsonElement>()

            data = data.let {
                if (append) {
                    if (it is JsonArray) {
                        if (newData is JsonArray) {
                            if (fold) {
                                val list: MutableList<JsonElement> = ArrayList(it)
                                newData.forEach { element -> list.add(list.lastOrNull()?.mergeWith(element) ?: element) }
                                JsonArray(list)
                            } else {
                                JsonArray(it + newData)
                            }
                        } else {
                            if (fold) {
                                val list: MutableList<JsonElement> = ArrayList(it)
                                list.add(list.lastOrNull()?.mergeWith(newData ?: JsonNull) ?: newData ?: JsonNull)
                                JsonArray(list)
                            } else {
                                JsonArray(it + (newData ?: JsonNull))
                            }
                        }
                    } else if (newData is JsonArray) {
                        if (fold) {
                            val list: MutableList<JsonElement> = arrayListOf(it ?: JsonNull)
                            newData.forEach { element -> list.add(list.lastOrNull()?.mergeWith(element) ?: element) }
                            JsonArray(list)
                        } else {
                            JsonArray(mutableListOf(it ?: JsonNull) + newData)
                        }
                    } else {
                        JsonArray(listOf(it ?: JsonNull, it?.mergeWith(newData ?: JsonNull) ?: newData ?: JsonNull))
                    }
                } else {
                    index = 0

                    if (it is JsonArray) {
                        if (newData is JsonArray) {
                            if (fold) {
                                val list: MutableList<JsonElement> = ArrayList()
                                it.lastOrNull()?.let(list::add)
                                newData.forEach { element -> list.add(list.lastOrNull()?.mergeWith(element) ?: element) }
                                JsonArray(list.drop(1))
                            } else {
                                newData
                            }
                        } else {
                            if (fold) {
                                it.lastOrNull()?.mergeWith(newData ?: JsonNull) ?: newData
                            } else {
                                newData
                            }
                        }
                    } else if (newData is JsonArray) {
                        if (fold) {
                            val list: MutableList<JsonElement> = arrayListOf(it ?: JsonNull)
                            newData.forEach { element -> list.add(list.lastOrNull()?.mergeWith(element) ?: element) }
                            JsonArray(list.drop(1))
                        } else {
                            newData
                        }
                    } else if (fold) {
                        it?.mergeWith(newData ?: JsonNull)
                    } else {
                        newData
                    }
                }
            }
        }

        override suspend fun updateDataForWebSocket(league: BlasementLeague, session: WebSocketServerSession, call: ApplicationCall) {
            val append = call.request.queryParameters["append"]?.toBooleanStrictOrNull() ?: false
            val fold = call.request.queryParameters["fold"]?.toBooleanStrictOrNull() ?: false

            session.incoming.receiveAsFlow()
                .filterIsInstance<Frame.Text>()
                .onEach { frame ->
                    val newData = json.parseToJsonElement(frame.readText())

                    data = data.let {
                        if (append) {
                            if (it is JsonArray) {
                                if (newData is JsonArray) {
                                    if (fold) {
                                        val list: MutableList<JsonElement> = ArrayList(it)
                                        newData.forEach { element -> list.add(list.lastOrNull()?.mergeWith(element) ?: element) }
                                        JsonArray(list)
                                    } else {
                                        JsonArray(it + newData)
                                    }
                                } else {
                                    if (fold) {
                                        val list: MutableList<JsonElement> = ArrayList(it)
                                        list.add(list.lastOrNull()?.mergeWith(newData ?: JsonNull) ?: newData ?: JsonNull)
                                        JsonArray(list)
                                    } else {
                                        JsonArray(it + (newData ?: JsonNull))
                                    }
                                }
                            } else if (newData is JsonArray) {
                                if (fold) {
                                    val list: MutableList<JsonElement> = arrayListOf(it ?: JsonNull)
                                    newData.forEach { element -> list.add(list.lastOrNull()?.mergeWith(element) ?: element) }
                                    JsonArray(list)
                                } else {
                                    JsonArray(mutableListOf(it ?: JsonNull) + newData)
                                }
                            } else {
                                JsonArray(listOf(it ?: JsonNull, it?.mergeWith(newData ?: JsonNull) ?: newData ?: JsonNull))
                            }
                        } else {
                            index = 0

                            if (it is JsonArray) {
                                if (newData is JsonArray) {
                                    if (fold) {
                                        val list: MutableList<JsonElement> = ArrayList()
                                        it.lastOrNull()?.let(list::add)
                                        newData.forEach { element -> list.add(list.lastOrNull()?.mergeWith(element) ?: element) }
                                        JsonArray(list.drop(1))
                                    } else {
                                        newData
                                    }
                                } else {
                                    if (fold) {
                                        it.lastOrNull()?.mergeWith(newData ?: JsonNull) ?: newData
                                    } else {
                                        newData
                                    }
                                }
                            } else if (newData is JsonArray) {
                                if (fold) {
                                    val list: MutableList<JsonElement> = arrayListOf(it ?: JsonNull)
                                    newData.forEach { element -> list.add(list.lastOrNull()?.mergeWith(element) ?: element) }
                                    JsonArray(list.drop(1))
                                } else {
                                    newData
                                }
                            } else {
                                newData
                            }
                        }
                    }
                }.launchIn(session)
                .join()
        }

        override fun describe(): JsonElement =
            buildJsonObject {
                put("type", "static")
                put("data", data ?: JsonNull)
                put("index", if (loopOnCompletion) index % ((data as? JsonArray)?.size ?: 1) else index.coerceAtMost((data as? JsonArray)?.size ?: 1))
                put("loop_on_completion", loopOnCompletion)
                put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
            }
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
        league.streamData
            .await()
            .firstOrNull()
            ?.let(Json::parseToJsonElement)

    suspend fun setupFlow(league: BlasementLeague): SharedFlow<String>
    override fun describe(): JsonElement?

    companion object {
        private fun JsonElement.deepSort(): JsonElement = this
//            when (this) {
//                is JsonObject -> JsonObject(mapValuesTo(TreeMap()) { (_, v) -> v.deepSort() })
//                is JsonArray -> JsonArray(map { it.deepSort() }.sortedWith { a, b -> compare(a, b) })
//                is JsonPrimitive -> this
//            }

        @OptIn(ExperimentalStdlibApi::class)
        private fun JsonElement.mergeWith(other: JsonElement): JsonElement =
            when {
                this is JsonObject && other is JsonObject -> JsonObject(this.toMutableMap().apply {
                    forEach { (k, v) -> other[k]?.let { put(k, v.mergeWith(it)) } }
                    other.forEach { (k, v) -> putIfAbsent(k, v) }
                })
                this is JsonArray && other is JsonArray ->
                    JsonArray(mapIndexedTo(ArrayList()) { i, v ->
                        other.getOrNull(i)?.let { v.mergeWith(it) } ?: v
                    }.apply { addAll(other.drop(size)) })

                else -> other
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

        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballEventsStreamDataEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Chronicler(emptyList())
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler(emptyList())
                            "live" -> Live(emptyList())
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler(JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "chronicler_at_time" -> ChroniclerAtTime(
                                BlasementClock.loadFrom(config["clock"], System.currentTimeMillis(), Clock.systemUTC()).getOrBreak { return it.cast() },
                                JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers")
                            )
                            "live" -> Live(JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "static" -> Static(config["data"], config.getIntOrNull("index") ?: 0, config.getBooleanOrNull("loop_on_completion") ?: false, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }
}