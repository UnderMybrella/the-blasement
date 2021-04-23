package dev.brella.blasement

import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballIdols
import dev.brella.kornea.blaseball.base.common.beans.BlaseballTribute
import dev.brella.kornea.blaseball.endpoints.BlaseballGlobalService
import dev.brella.kornea.blaseball.endpoints.BlaseballService
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.doOnFailure
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.filterNotNull
import dev.brella.kornea.errors.common.map
import dev.brella.ktornea.common.KorneaHttpResult
import dev.brella.ktornea.common.getAsResult
import getJsonArray
import getJsonObject
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import redirectInternally
import respond
import kotlin.reflect.jvm.jvmName

interface IBlaseballDataSource {
    suspend fun getIdolBoard(): KorneaResult<BlaseballIdols>
    suspend fun getHallOfFlamePlayers(): KorneaResult<List<BlaseballTribute>>
}

class IBlaseballChroniclerDataSource(val client: HttpClient, val from: String, val to: String) : IBlaseballDataSource {
    companion object {
        fun massProduction(client: HttpClient) =
            IBlaseballChroniclerDataSource(client, "2021-04-12T00:00:00Z", "2021-04-19T00:00:00Z")

        fun liveBait(client: HttpClient) =
            IBlaseballChroniclerDataSource(client, "2021-04-05T00:00:00Z", "2021-04-12T00:00:00Z")
    }

    override suspend fun getIdolBoard(): KorneaResult<BlaseballIdols> =
        client.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/versions") {
            parameter("type", "idols")
            parameter("count", 1)
            parameter("after", from)
            parameter("before", to)
        }.map { json ->
            json.getJsonArray("items")
                .firstOrNull()?.jsonObject
                ?.getJsonObject("data")
                ?.let { idolBoard ->
                    BlaseballIdols(
                        idolBoard.getJsonArray("idols")
                            .map { PlayerID(it.jsonPrimitive.content) },
                        idolBoard.getJsonObject("data")
                    )
                }
        }.filterNotNull()

    override suspend fun getHallOfFlamePlayers(): KorneaResult<List<BlaseballTribute>> =
        client.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v1/tributes/hourly") {
            parameter("order", "desc")
            parameter("format", "json")
            parameter("count", 1)
            parameter("after", from)
            parameter("before", to)
        }.map { json ->
            json.getJsonArray("data")
                .firstOrNull()?.jsonObject
                ?.getJsonObject("players")
                ?.map { (playerID, peanuts) -> BlaseballTribute(PlayerID(playerID), peanuts.jsonPrimitive.long) }
                ?.sortedByDescending(BlaseballTribute::peanuts)
        }.filterNotNull()
}

data class IBlaseballDataSourceWrapper(val api: BlaseballApi) : IBlaseballDataSource {
    override suspend fun getIdolBoard(): KorneaResult<BlaseballIdols> =
        api.getIdolBoard()

    override suspend fun getHallOfFlamePlayers(): KorneaResult<List<BlaseballTribute>> =
        api.getHallOfFlamePlayers()
}

fun Route.blaseball(source: IBlaseballDataSource) {
    route("/api") api@{
        redirectInternally("/idol_board", "/getIdols")
        get("/getIdols") {
            source.getIdolBoard().respond(call)
        }

        redirectInternally("/hall_of_flame", "/getTribute")
        get("/getTribute") {
            source.getHallOfFlamePlayers().respond(call)
        }
    }
}