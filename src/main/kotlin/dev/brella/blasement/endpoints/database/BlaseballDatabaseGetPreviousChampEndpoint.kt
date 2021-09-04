package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.JsonTransformer
import dev.brella.blasement.endpoints.Static
import dev.brella.blasement.endpoints.invoke
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import io.ktor.application.*
import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.util.*

interface BlaseballDatabaseGetPreviousChampEndpoint : BlaseballEndpoint {
    companion object {
        val OVERBRACKET_CHAMPIONS = mapOf(
            1 to "23e4cbc1-e9cd-47fa-a35b-bfa06f726cb7", //Pies
            2 to "23e4cbc1-e9cd-47fa-a35b-bfa06f726cb7",
            3 to "747b8e4a-7e50-4638-a973-ea7950a3e739", //Tigers
            4 to "747b8e4a-7e50-4638-a973-ea7950a3e739",
            5 to "ca3f1c8c-c025-4d8e-8eef-5be6accbeb16", //Firefighters
            6 to "8d87c468-699a-47a8-b40d-cfb73a5660ad", //Crabs
            7 to "57ec08cc-0411-4643-b304-0e80dbc15ac7", //Wild Wings
            8 to "8d87c468-699a-47a8-b40d-cfb73a5660ad", //Crabs
            9 to "bfd38797-8404-4b38-8b82-341da28b1f83", //Shoe Thieves
            10 to "8d87c468-699a-47a8-b40d-cfb73a5660ad", //Crabs
            11 to "f02aeae2-5e6a-4098-9842-02d2273f25c7", //Sunbeams
            //Coffee Cup - d8f82163-2e74-496b-8e4b-2ab35b2d3ff1 - Xpresso
            12 to "747b8e4a-7e50-4638-a973-ea7950a3e739", //Tigers
            13 to "8d87c468-699a-47a8-b40d-cfb73a5660ad", //Crabs
            14 to "eb67ae5e-c4bf-46ca-bbbc-425cd34182ff", //Moist Talkers
            15 to "eb67ae5e-c4bf-46ca-bbbc-425cd34182ff", //Moist Talkers
            16 to "b024e975-1c4a-4575-8936-a3754a08806a", //Steaks
            17 to "878c1bf6-0d21-4659-bfee-916c8314d69c", //Tacos
            18 to "46358869-dce9-4a01-bfba-ac24fc56f57e", //Mechanics
            19 to "c73b705c-40ad-4633-a6ed-d357ee2e2bcf", //Lift
            20 to "46358869-dce9-4a01-bfba-ac24fc56f57e", //Mechanics
            21 to "8d87c468-699a-47a8-b40d-cfb73a5660ad", //Crabs
            22 to "adc5b394-8f76-416d-9ce9-813706877b84", //Breath Mints
            23 to "eb67ae5e-c4bf-46ca-bbbc-425cd34182ff", //Moist Talkers
        )
        val UNDERBRACKET_CHAMPIONS = mapOf(
            20 to "b72f3061-f573-40d7-832a-5ad475bd7909", //Lovers
            21 to "f02aeae2-5e6a-4098-9842-02d2273f25c7", //Sunbeams
            22 to "b63be8c2-576a-4d6e-8daf-814f8bcea96f", //Dale
            23 to "7966eb04-efcc-499b-8f03-d139163305310", //Magic
        )

        val OVER_UNDER_CHAMPIONSHIP_CHANGE = Instant.parse("2021-07-19T14:50:00.000Z")

        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabaseGetPreviousChampEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> QueryLookup()
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "querylookup", "query_lookup", "query lookup" -> QueryLookup()
                            "live" -> Live()
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "querylookup", "query_lookup", "query lookup" -> QueryLookup(JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "live" -> Live(JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            "static" -> Static(config["data"] ?: JsonNull, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }

    data class QueryLookup(val transformers: List<JsonTransformer> = emptyList()) : BlaseballDatabaseGetPreviousChampEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? {
            if (league.siteDataClock.getTime() < OVER_UNDER_CHAMPIONSHIP_CHANGE) {
                val time = league.clock.getTime()
                return request.call.request.queryParameters["season"]
                    ?.toIntOrNull()
                    ?.let(OVERBRACKET_CHAMPIONS::get)
                    ?.let { id ->
                        transformers {
                            league.httpClient.getChroniclerEntity("team", time) {
                                parameter("id", id)
                            } ?: JsonNull
                        }
                    }
            } else {
                val time = league.clock.getTime()
                return transformers {
                    buildJsonObject {
                        val season = request.call.request.queryParameters["season"]
                            ?.toIntOrNull()

                        season
                            ?.let(OVERBRACKET_CHAMPIONS::get)
                            ?.let { id ->
                                put("over", league.httpClient.getChroniclerEntity("team", time) {
                                    parameter("id", id)
                                } ?: JsonNull)
                            }

                        season
                            ?.let(UNDERBRACKET_CHAMPIONS::get)
                            ?.let { id ->
                                put("under", league.httpClient.getChroniclerEntity("team", time) {
                                    parameter("id", id)
                                } ?: JsonNull)
                            }
                    }
                }
            }
        }

        override fun describe(): JsonElement? =
            JsonPrimitive("query_lookup")
    }

    data class TimeLookup(val transformers: List<JsonTransformer> = emptyList()) : BlaseballDatabaseGetPreviousChampEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? {
            if (league.siteDataClock.getTime() < OVER_UNDER_CHAMPIONSHIP_CHANGE) {
                val time = league.clock.getTime()
                return request.call.request.queryParameters["season"]
                    ?.toIntOrNull()
                    ?.let(OVERBRACKET_CHAMPIONS::get)
                    ?.let { id ->
                        transformers {
                            league.httpClient.getChroniclerEntity("team", time) {
                                parameter("id", id)
                            } ?: JsonNull
                        }
                    }
            } else {
                val time = league.clock.getTime()
                return transformers {
                    buildJsonObject {
                        val season = request.call.request.queryParameters["season"]
                            ?.toIntOrNull()

                        season
                            ?.let(OVERBRACKET_CHAMPIONS::get)
                            ?.let { id ->
                                put("over", league.httpClient.getChroniclerEntity("team", time) {
                                    parameter("id", id)
                                } ?: JsonNull)
                            }

                        season
                            ?.let(UNDERBRACKET_CHAMPIONS::get)
                            ?.let { id ->
                                put("under", league.httpClient.getChroniclerEntity("team", time) {
                                    parameter("id", id)
                                } ?: JsonNull)
                            }
                    }
                }
            }
        }

        override fun describe(): JsonElement? =
            JsonPrimitive("query_lookup")
    }

    data class Live(val transformers: List<JsonTransformer> = emptyList()) : BlaseballDatabaseGetPreviousChampEndpoint {
        override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement? =
            transformers {
                league.httpClient.get<JsonObject>("https://www.blaseball.com/database/getPreviousChamp") {
                    parameter("season", request.call.request.queryParameters["season"])
                }.let { json ->
                    if (league.siteDataClock.getTime() < OVER_UNDER_CHAMPIONSHIP_CHANGE)
                        json["over"] ?: JsonNull
                    else
                        json
                }
            }

        override fun describe(): JsonElement =
            buildJsonObject {
                put("type", "live")
                put("transformers", JsonArray(transformers.map(JsonTransformer::describe)))
            }
    }

    override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?
}