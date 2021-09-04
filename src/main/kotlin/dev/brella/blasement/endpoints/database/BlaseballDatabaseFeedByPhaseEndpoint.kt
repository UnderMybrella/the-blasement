package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.BlasementLeagueBuilder
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.JsonTransformer
import dev.brella.blasement.endpoints.Live
import dev.brella.blasement.endpoints.Static
import dev.brella.blasement.endpoints.invoke
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import io.ktor.application.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.serialization.json.*
import java.util.*

interface BlaseballDatabaseFeedByPhaseEndpoint : BlaseballEndpoint {
    class Upnuts(vararg val providers: String, val transformers: List<JsonTransformer> = emptyList()) : BlaseballDatabaseFeedByPhaseEndpoint {
        companion object {
            const val TGB = "7fcb63bc-11f2-40b9-b465-f1d458692a63"

            operator fun invoke(transformers: List<JsonTransformer> = emptyList(), builder: Companion.() -> Array<out String>): Upnuts =
                Upnuts(providers = builder(), transformers = transformers)
        }

        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
            transformers {
                league.httpClient.get<JsonArray>("https://api.sibr.dev/upnuts/feed/global") {
                    request.call.request.queryParameters.flattenForEach { k, v -> parameter(k, v) }
                    parameter("time", league.clock.getTime().toEpochMilliseconds())
                    parameter("one_of_providers", providers.joinToString(","))

                    timeout {
                        socketTimeoutMillis = 20_000
                    }
                }
            }

        override fun describe(): JsonElement? =
            buildJsonObject {
                put("type", "upnuts")
                putJsonArray("providers") {
                    providers.forEach { add(it) }
                }
                put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
            }
    }

    companion object {
        const val PATH = "/database/feedbyphase"

        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabaseFeedByPhaseEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Upnuts(Upnuts.TGB)
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB)
                            "live" -> Live(PATH)
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "upnuts" -> Upnuts(Upnuts.TGB, transformers = JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "live" -> Live(PATH, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "static" -> Static(config["data"] ?: JsonNull, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }
}