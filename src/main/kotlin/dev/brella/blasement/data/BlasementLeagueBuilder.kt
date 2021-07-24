package dev.brella.blasement.data

import dev.brella.blasement.endpoints.BlaseballEventsStreamDataEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetActiveBetsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetIdolsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetRisingStarsEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetTributesEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserEndpoint
import dev.brella.blasement.endpoints.api.BlaseballApiGetUserRewardsEndpoint
import dev.brella.blasement.endpoints.database.*
import io.ktor.client.*
import kotlinx.serialization.json.Json

class BlasementLeagueBuilder {
    class Api {
        var getActiveBets: BlaseballApiGetActiveBetsEndpoint? = null
        var getIdols: BlaseballApiGetIdolsEndpoint? = null

        var getRisingStars: BlaseballApiGetRisingStarsEndpoint? = null
        var getTribute: BlaseballApiGetTributesEndpoint? = null
        var getUser: BlaseballApiGetUserEndpoint? = null

        //    var apiGetUserNotifications: BlaseballApiGetUserNotificationsEndpoint? = null
        var getUserRewards: BlaseballApiGetUserRewardsEndpoint? = null
//    var apiUserFeed: BlaseballApiUserFeedEndpoint? = null?

        inline operator fun invoke(builder: Api.() -> Unit) = apply(builder)
    }

    class Auth {
        //    var authResetPassword: BlaseballAuthResetPasswordEndpoint? = null
//    var authValidateResetToken: BlaseballAuthValidateResetTokenEndpoint? = null

        inline operator fun invoke(builder: Auth.() -> Unit) = apply(builder)
    }

    class Database {
        inner class Feed {
            var global: BlaseballDatabaseFeedEndpoint.Global? = null
            var game: BlaseballDatabaseFeedEndpoint.Game? = null
            var player: BlaseballDatabaseFeedEndpoint.Player? = null
            var team: BlaseballDatabaseFeedEndpoint.Team? = null
            var story: BlaseballDatabaseFeedEndpoint.Story? = null

            inline operator fun invoke(builder: Feed.() -> Unit) = apply(builder)
        }

        var allDivisions: BlaseballDatabaseAllDivisionsEndpoint? = null
        var allTeams: BlaseballDatabaseAllTeamsEndpoint? = null
        var communityChestProgress: BlaseballDatabaseCommunityChestProgressEndpoint? = null

        var bonusResults: BlaseballDatabaseBonusResultsEndpoint? = null
        var decreeResults: BlaseballDatabaseDecreeResultsEndpoint? = null
        var eventResults: BlaseballDatabaseEventResultsEndpoint? = null

        var feedByPhase: BlaseballDatabaseFeedByPhaseEndpoint? = null

        var gameById: BlaseballDatabaseGameByIdEndpoint? = null

        var getPreviousChamp: BlaseballDatabaseGetPreviousChampEndpoint? = null
        var giftProgress: BlaseballDatabaseGiftProgressEndpoint? = null
        var globalEvents: BlaseballDatabaseGlobalEventsEndpoint? = null

        var items: BlaseballDatabaseItemsEndpoint? = null

        var offseasonRecap: BlaseballDatabaseOffseasonRecapEndpoint? = null
        var offseasonSetup: BlaseballDatabaseOffseasonSetupEndpoint? = null
        var playerNamesIds: BlaseballDatabasePlayerNamesEndpoint? = null
        var players: BlaseballDatabasePlayersEndpoint? = null

        var playersByItemId: BlaseballDatabasePlayersByItemEndpoint? = null
        var playoffs: BlaseballDatabasePlayoffsEndpoint? = null
        var renovationProgress: BlaseballDatabaseRenovationProgressEndpoint? = null

        var renovations: BlaseballDatabaseRenovationsEndpoint? = null
        var shopSetup: BlaseballDatabaseShopSetupEndpoint? = null

        var subleague: BlaseballDatabaseSubleagueEndpoint? = null
        var sunSun: BlaseballDatabaseSunSunEndpoint? = null

        var team: BlaseballDatabaseTeamEndpoint? = null
        var teamElectionStats: BlaseballDatabaseTeamElectionStatsEndpoint? = null

        var vault: BlaseballDatabaseVaultEndpoint? = null

        val feed = Feed()

        inline operator fun invoke(builder: Database.() -> Unit) = apply(builder)
    }

    val api = Api()
    val auth = Auth()
    val database = Database()

    var eventsStreamData: BlaseballEventsStreamDataEndpoint? = null

    lateinit var leagueID: String
    lateinit var json: Json
    lateinit var http: HttpClient
    lateinit var clock: BlasementClock
    var siteDataClock: BlasementClock? = null

    lateinit var protectionStatus: EnumProtectionStatus
    lateinit var authentication: String

    fun build(): BlasementLeague =
        BlasementLeague(
            leagueID = leagueID,
            json = json,
            httpClient = http,

            protection = protectionStatus,
            authentication = authentication,

            clock = clock,
            siteDataClock = siteDataClock ?: clock,

            apiGetUser = api.getUser,
            apiGetUserRewards = api.getUserRewards,
            apiGetActiveBets = api.getActiveBets,
            apiGetIdols = api.getIdols,
            apiGetTributes = api.getTribute,

            databaseGlobalEvents = database.globalEvents,
            databaseShopSetup = database.shopSetup,

            databaseGlobalFeed = database.feed.global,
            databaseGameFeed = database.feed.game,
            databasePlayerFeed = database.feed.player,
            databaseTeamFeed = database.feed.team,
            databaseStoryFeed = database.feed.story,
            databaseFeedByPhase = database.feedByPhase,

            databasePlayerNames = database.playerNamesIds,
            databasePlayers = database.players,
            databaseOffseasonSetup = database.offseasonSetup,
            databaseVault = database.vault,
            databaseSunSun = database.sunSun,

            databaseAllDivisions = database.allDivisions,
            databaseAllTeams = database.allTeams,
            databaseCommunityChestProgress = database.communityChestProgress,

            databaseBonusResults = database.bonusResults,
            databaseDecreeResults = database.decreeResults,
            databaseEventResults = database.eventResults,

            databaseGameById = database.gameById,
            databaseGetPreviousChamp = database.getPreviousChamp,
            databaseGiftProgress = database.giftProgress,
            databaseItems = database.items,

            databaseOffseasonRecap = database.offseasonRecap,
            databasePlayersByItemId = database.playersByItemId,
            databasePlayoffs = database.playoffs,
            databaseRenovationProgress = database.renovationProgress,
            databaseRenovations = database.renovations,
            databaseSubleague = database.subleague,
            databaseTeam = database.team,
            databaseTeamElectionStats = database.teamElectionStats,

            eventsStreamData = eventsStreamData,
        )
}

inline fun buildBlasementLeague(block: BlasementLeagueBuilder.() -> Unit): BlasementLeague {
    val builder = BlasementLeagueBuilder()
    builder.block()
    return builder.build()
}

inline fun buildBlasementLeague(
    leagueID: String? = null,
    json: Json? = null,
    http: HttpClient? = null,
    clock: BlasementClock? = null,
    siteDataClock: BlasementClock? = clock,
    protection: EnumProtectionStatus? = null,
    authentication: String? = null,
    block: BlasementLeagueBuilder.() -> Unit
): BlasementLeague {
    val builder = BlasementLeagueBuilder()

    if (leagueID != null) builder.leagueID = leagueID
    if (json != null) builder.json = json
    if (http != null) builder.http = http
    if (clock != null) builder.clock = clock
    if (siteDataClock != null) builder.siteDataClock = siteDataClock
    if (protection != null) builder.protectionStatus = protection
    if (authentication != null) builder.authentication = authentication

    builder.block()
    return builder.build()
}