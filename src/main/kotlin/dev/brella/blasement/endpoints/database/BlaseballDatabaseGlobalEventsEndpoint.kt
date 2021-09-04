package dev.brella.blasement.endpoints.database

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.BlasementLeagueBuilder
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.Chronicler
import dev.brella.blasement.endpoints.JsonTransformer
import dev.brella.blasement.endpoints.Live
import dev.brella.blasement.endpoints.Static
import dev.brella.blasement.getChroniclerEntity
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import io.ktor.client.request.*
import kotlinx.serialization.json.*
import java.util.*

interface BlaseballDatabaseGlobalEventsEndpoint : BlaseballEndpoint {
    companion object {
        const val TYPE = "globalevents"
        const val PATH = "/database/globalEvents"
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballDatabaseGlobalEventsEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> Chronicler(TYPE)
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler(TYPE)
                            "live" -> Live(PATH)
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "chronicler" -> Chronicler(TYPE, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
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

inline class GlobalEventsBuilder(val builder: JsonArrayBuilder) {
    inline fun add(id: String, msg: String, expire: Long? = null) =
        builder.addJsonObject {
            put("id", id)
            put("msg", msg)
            put("expire", expire)
        }

    inline operator fun set(id: String, msg: String) =
        add(id, msg, null)
}

inline fun BlasementLeagueBuilder.Database.buildGlobalEvents(init: GlobalEventsBuilder.() -> Unit) =
    Static(buildJsonArray { GlobalEventsBuilder(this).init() }, emptyList())