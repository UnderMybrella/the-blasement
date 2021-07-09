package dev.brella.blasement

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

public suspend inline fun HttpClient.getChroniclerEntity(type: String, at: Instant) =
    getChroniclerEntity(type, at.toString())
public suspend inline fun HttpClient.getChroniclerEntity(type: String, at: String) =
    ((get<JsonObject>("https://api.sibr.dev/chronicler/v2/entities") {
        parameter("type", type)
        parameter("at", at)
    }["items"] as? JsonArray)?.firstOrNull() as? JsonObject)?.get("data")