package dev.brella.blasement

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import dev.brella.blasement.common.events.FanID
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.FeedID
import dev.brella.kornea.blaseball.base.common.ItemID
import dev.brella.kornea.blaseball.base.common.ModificationID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballDatabasePlayer
import dev.brella.kornea.blaseball.base.common.beans.BlaseballFeedEvent
import dev.brella.kornea.blaseball.base.common.beans.BlaseballGlobalEvent
import dev.brella.kornea.blaseball.base.common.beans.BlaseballIdols
import dev.brella.kornea.blaseball.base.common.beans.BlaseballItem
import dev.brella.kornea.blaseball.base.common.beans.BlaseballMod
import dev.brella.kornea.blaseball.base.common.beans.BlaseballSimulationData
import dev.brella.kornea.blaseball.base.common.beans.BlaseballTribute
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.cast
import dev.brella.kornea.errors.common.getOrBreak
import dev.brella.kornea.errors.common.map
import dev.brella.ktornea.common.getAsResult
import dev.brella.ktornea.common.streamAsResult
import getJsonArray
import getString
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.JsonObject

interface BlasementDataSource {
    suspend fun getFeedByPhase(phase: Int, season: Int): KorneaResult<List<BlaseballFeedEvent>>
    suspend fun getGlobalFeed(category: Int? = null, limit: Int = 100, type: Int? = null, sort: Int? = null, start: String? = null, upNuts: Map<FeedID, Set<FanID>> = emptyMap(), fanID: FanID? = null): KorneaResult<List<BlaseballFeedEvent>>
    suspend fun getPlayerFeed(
        id: PlayerID,
        category: Int? = null,
        limit: Int = 100,
        type: Int? = null,
        sort: Int? = null,
        start: String? = null,
        upNuts: Map<FeedID, Set<FanID>> = emptyMap(),
        fanID: FanID? = null
    ): KorneaResult<List<BlaseballFeedEvent>>

    suspend fun getTeamFeed(
        id: TeamID,
        category: Int? = null,
        limit: Int = 100,
        type: Int? = null,
        sort: Int? = null,
        start: String? = null,
        upNuts: Map<FeedID, Set<FanID>> = emptyMap(),
        fanID: FanID? = null
    ): KorneaResult<List<BlaseballFeedEvent>>

    suspend fun getIdolBoard(): KorneaResult<BlaseballIdols>
    suspend fun getHallOfFlamePlayers(): KorneaResult<List<BlaseballTribute>>

    suspend fun getBloodTypes(bloodIDs: Iterable<String>): KorneaResult<List<String>>
    suspend fun getCoffeePreferences(coffeeIDs: Iterable<String>): KorneaResult<List<String>>

    suspend fun getItems(itemIDs: Iterable<ItemID>): KorneaResult<List<BlaseballItem>>
    suspend fun getModifications(modIDs: Iterable<ModificationID>): KorneaResult<List<BlaseballMod>>

    suspend fun getPlayers(playerIDs: Iterable<PlayerID>): KorneaResult<List<BlaseballDatabasePlayer>>

    suspend fun getGlobalEvents(): KorneaResult<List<BlaseballGlobalEvent>>
    suspend fun getSimulationData(): KorneaResult<BlaseballSimulationData>

    suspend fun getLiveDataStream(): KorneaResult<Flow<String>>

    fun now(): DateTimeTz
    suspend fun wait(until: DateTimeTz)
}

data class BlasementDataSourceWrapper(val blaseball: BlaseballApi) : BlasementDataSource {
    override suspend fun getFeedByPhase(phase: Int, season: Int): KorneaResult<List<BlaseballFeedEvent>> =
        blaseball.getFeedByPhase(phase, season)

    override suspend fun getGlobalFeed(category: Int?, limit: Int, type: Int?, sort: Int?, start: String?, upNuts: Map<FeedID, Set<FanID>>, fanID: FanID?): KorneaResult<List<BlaseballFeedEvent>> =
        blaseball.getGlobalFeed(category, limit, type, sort, start)
            .let { result ->
                if (fanID == null) {
                    result.map { feedList ->
                        feedList.map { event ->
                            upNuts[event.id]?.let { event.nuts += it.size }
                            event
                        }
                    }
                } else {
                    result.map { feedList ->
                        feedList.map { event ->
                            upNuts[event.id]?.let {
                                if (fanID in it) event.metadata?.upnut = true

                                event.nuts += it.size
                            }
                            event
                        }
                    }
                }
            }

    override suspend fun getPlayerFeed(id: PlayerID, category: Int?, limit: Int, type: Int?, sort: Int?, start: String?, upNuts: Map<FeedID, Set<FanID>>, fanID: FanID?): KorneaResult<List<BlaseballFeedEvent>> =
        blaseball.getPlayerFeed(id, category, limit, type, sort, start)
            .let { result ->
                if (fanID == null) {
                    result.map { feedList ->
                        feedList.map { event ->
                            upNuts[event.id]?.let { event.nuts += it.size }
                            event
                        }
                    }
                } else {
                    result.map { feedList ->
                        feedList.map { event ->
                            upNuts[event.id]?.let {
                                upNuts[event.id]?.let {
                                    if (fanID in it) event.metadata?.upnut = true

                                    event.nuts += it.size
                                }
                            }
                            event
                        }
                    }
                }
            }

    override suspend fun getTeamFeed(id: TeamID, category: Int?, limit: Int, type: Int?, sort: Int?, start: String?, upNuts: Map<FeedID, Set<FanID>>, fanID: FanID?): KorneaResult<List<BlaseballFeedEvent>> =
        blaseball.getTeamFeed(id, category, limit, type, sort, start)
            .let { result ->
                if (fanID == null) {
                    result.map { feedList ->
                        feedList.map { event ->
                            upNuts[event.id]?.let { event.nuts += it.size }
                            event
                        }
                    }
                } else {
                    result.map { feedList ->
                        feedList.map { event ->
                            upNuts[event.id]?.let {
                                upNuts[event.id]?.let {
                                    if (fanID in it) event.metadata?.upnut = true

                                    event.nuts += it.size
                                }
                            }
                            event
                        }
                    }
                }
            }

    override suspend fun getIdolBoard(): KorneaResult<BlaseballIdols> =
        blaseball.getIdolBoard()

    override suspend fun getHallOfFlamePlayers(): KorneaResult<List<BlaseballTribute>> =
        blaseball.getHallOfFlamePlayers()

    override suspend fun getBloodTypes(bloodIDs: Iterable<String>): KorneaResult<List<String>> = blaseball.getBloodTypes(bloodIDs)
    override suspend fun getCoffeePreferences(coffeeIDs: Iterable<String>): KorneaResult<List<String>> = blaseball.getCoffeePreferences(coffeeIDs)

    override suspend fun getGlobalEvents(): KorneaResult<List<BlaseballGlobalEvent>> =
        blaseball.getGlobalEvents()

    override suspend fun getSimulationData(): KorneaResult<BlaseballSimulationData> =
        blaseball.getSimulationData()

    override suspend fun getItems(itemIDs: Iterable<ItemID>): KorneaResult<List<BlaseballItem>> =
        blaseball.getItems(itemIDs)

    override suspend fun getModifications(modIDs: Iterable<ModificationID>): KorneaResult<List<BlaseballMod>> =
        blaseball.getModifications(modIDs)

    override suspend fun getPlayers(playerIDs: Iterable<PlayerID>): KorneaResult<List<BlaseballDatabasePlayer>> =
        blaseball.getPlayers(playerIDs)

    override suspend fun getLiveDataStream(): KorneaResult<Flow<String>> =
        blaseball.client.streamAsResult {
            method = HttpMethod.Get
            url("${blaseball.blaseballBaseUrl}/events/streamData")
        }.map { flow ->
            flow.mapNotNull { str ->
                if (str.startsWith("data:")) str.substring(5) else null
            }
        }

    override fun now(): DateTimeTz =
        DateTime.now().utc

    override suspend fun wait(until: DateTimeTz) =
        delay((until - now()).millisecondsLong)
}


suspend fun HttpClient.getSibrLatestFile(pathRegex: Regex, before: String? = null): KorneaResult<String> {
    val results = getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v1/site/updates") {
        parameter("order", "desc")
        parameter("count", 50)
        if (before != null) parameter("before", before)
    }.getOrBreak { return it.cast() }.getJsonArray("data").filterIsInstance<JsonObject>()

    val matching = results.firstOrNull { obj -> obj.getString("path").matches(pathRegex) } ?: return KorneaResult.failedPredicate()

    return getAsResult("https://api.sibr.dev/chronicler/v1${matching.getString("downloadUrl")}")
}

val MAIN_JS_REPLACEMENTS = mapOf(
    "/events/" to "events/",
    "/api/getUser" to "api/getUser",
    "/api/getActiveBets" to "api/getActiveBets",

    "/player/" to "player/",
    "/bet/" to "bet/",
    "/shop/" to "shop/",
    "/login" to "login",
    "/team/" to "team/",
    "/game/" to "game/",
    "/sell/" to "sell/",
    "/vote/" to "vote/",
    "/tribute/" to "tribute/",
    "/contribute/" to "contribute/",

    "/api/upNut" to "api/upNut",
    "/auth/validate-reset-token?token=" to "auth/validate-reset-token?token=",
    "/database/globalEvents" to "database/globalEvents",
    "/database/offseasonSetup" to "database/offseasonSetup",
    "/database/offseasonRecap?season=" to "database/offseasonRecap?season=",
    "/database/eventResults?ids=" to "database/eventResults?ids=",
    "/database/bonusResults?ids=" to "database/bonusResults?ids=",
    "/database/decreeResults?ids=" to "database/decreeResults?ids=",
    "/database/players?ids=" to "database/players?ids=",
    "/database/feed/global?" to "database/feed/global?",
    "/api/userfeed/?" to "api/userfeed/?",
    "/database/feed/player?id=" to "database/feed/player?id=",
    "/database/feed/team?id=" to "database/feed/team?id=",
    "/database/feed/game?id=" to "database/feed/game?id=",
    "/database/feedbyphase?phase=" to "database/feedbyphase?phase=",
    "/database/renovations?ids=" to "database/renovations?ids=",
    "/database/renovationProgress?id=" to "database/renovationProgress?id=",
    "/database/teamElectionStats?id=" to "database/teamElectionStats?id=",
    "/api/eatADangPeanut" to "api/eatADangPeanut",
    "/auth/logout" to "auth/logout",
    "/auth/local" to "auth/local",
    "/auth/reset-password/" to "auth/reset-password",
    "/auth/forgot-password" to "auth/forgot-password",
    "/api/getIdols" to "api/getIdols",
    "/api/getTribute" to "api/getTribute",
    "/api/updateFavoriteTeam" to "api/updateFavoriteTeam",
    "/api/bet" to "api/bet",
    "/api/reorderSnacks" to "api/reorderSnacks",
    "/api/reorderCards" to "api/reorderCards",
    "/api/buyUnlockShop" to "api/buyUnlockShop",
    "/api/buyUnlockElection" to "api/buyUnlockElection",
    "/api/logBeg" to "api/logBeg",
    "/buy/vote" to "buy/vote",
    "/api/buySnackNoUpgrade" to "api/buySnackNoUpgrade",
    "/api/buySnack" to "api/buySnack",
    "/api/dealCards" to "api/dealCards",
    "/api/buyVote" to "api/buyVote",
    "/api/sellSnack" to "api/sellSnack",
    "/api/sellSlot" to "api/sellSlot",
    "/api/buySlot" to "api/buySlot",
    "/api/updateProfile" to "api/updateProfile",
    "/api/updateSettings" to "api/updateSettings",
    "/api/setFavoriteTeam" to "api/setFavoriteTeam",
    "/api/payTribute" to "api/payTribute",
    "/api/vote" to "api/vote",
    "/api/getUserRewards" to "api/getUserRewards",
    "/api/chooseIdol" to "api/chooseIdol",
    "/api/renovate" to "api/renovate"
).mapKeys { (k) -> "\"$k\"" }.mapValues { (_, v) -> "\"$v\"" } + mapOf(
    "url=\"/upcoming\"" to "url=\"upcoming\"",
    "url=\"/shop\"" to "url=\"shop\"",

    "path=\"/account\"" to "path=\"account\"",
    "path=\"/signup\"" to "path=\"signup\"",
    "path=\"/login\"" to "path=\"login\"",
    "path=\"/league\"" to "path=\"league\"",
    "path=\"/upcoming\"" to "path=\"upcoming\"",
    "path=\"/bracket\"" to "path=\"bracket\"",
    "path=\"/tournament\"" to "path=\"tournament\"",
    "path=\"/standings\"" to "path=\"standings\"",
    "path=\"/leaderboard\"" to "path=\"leaderboard\"",
    "path=\"/renovation\"" to "path=\"renovation\"",
    "path=\"/pack/sell\"" to "path=\"pack/sell\"",
    "path=\"/reorder/snacks\"" to "path=\"reorder/snacks\"",
    "path=\"/pack/buy\"" to "path=\"pack/buy\"",
    "path=\"/reorder/cards\"" to "path=\"reorder/cards\"",
    "path=\"/forgot\"" to "path=\"forgot\"",

    "to=\"/shop/25\"" to "to=\"shop/25\"",
    "to=\"/upcoming\"" to "to=\"upcoming\"",
    "to=\"/leaderboard\"" to "to=\"leaderboard\"",
    "to=\"/shop/\"" to "to=\"shop/\"",
    "to=\"/offseason\"" to "to=\"offseason\"",
    "to=\"/upcoming\"" to "to=\"upcoming\"",
    "to=\"/league\"" to "to=\"league\"",

    "href=\"/offseason\"" to "href=\"offseason\"",
    "href=\"/upcoming\"" to "href=\"upcoming\"",
    "href=\"/login\"" to "href=\"login\"",
    "href=\"/welcome\"" to "href=\"welcome\"",
)

val indexCache: MutableMap<BlasementDataSource, String> = HashMap()
val mainCssCache: MutableMap<BlasementDataSource, String> = HashMap()
val mainJsCache: MutableMap<BlasementDataSource, String> = HashMap()
val twoJsCache: MutableMap<BlasementDataSource, String> = HashMap()