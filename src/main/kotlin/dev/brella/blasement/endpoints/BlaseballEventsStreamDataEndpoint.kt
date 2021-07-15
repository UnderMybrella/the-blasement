package dev.brella.blasement.endpoints

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getChroniclerVersionsBefore
import dev.brella.blasement.getJsonObjectOrNull
import dev.brella.blasement.loopEvery
import io.ktor.client.request.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlin.coroutines.coroutineContext
import kotlin.time.ExperimentalTime

//This is a bit of a misnomer honestly, since we're not setting up an 'endpoint'
fun interface BlaseballEventsStreamDataEndpoint {
    object Chronicler : BlaseballEventsStreamDataEndpoint {
        @OptIn(ExperimentalTime::class)
        override fun setupFlow(league: BlasementLeague): SharedFlow<String> =
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

                    emit(stream.toString())
                }
            }.shareIn(league, SharingStarted.Eagerly)
    }

    fun setupFlow(league: BlasementLeague): SharedFlow<String>
}