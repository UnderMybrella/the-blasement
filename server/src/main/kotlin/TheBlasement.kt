import com.soywiz.klock.DateFormat
import com.soywiz.klock.PatternDateFormat
import com.soywiz.klock.parse
import dev.brella.blasement.common.events.BlasementFanDatabasePayload
import dev.brella.blasement.common.events.EnumGainedMoneyReason
import dev.brella.blasement.common.events.FanID
import dev.brella.blasement.common.events.ServerEvent
import dev.brella.blasement.common.events.TimeRange
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.BettingPayouts
import dev.brella.kornea.blaseball.base.common.EnumBlaseballItem
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import dev.brella.kornea.blaseball.chronicler.EnumOrder
import dev.brella.ktornea.common.getAsResult
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.ktor.client.*
import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import io.r2dbc.h2.H2ConnectionOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt


class TheBlasement(val json: Json, val httpClient: HttpClient, val blaseballApi: BlaseballApi, val chroniclerApi: ChroniclerApi) : CoroutineScope {
    companion object {
        val format = PatternDateFormat("yyyy-MM-dd'T'HH:mm:ss[.SS[S[SSS]]]Z", options = PatternDateFormat.Options.WITH_OPTIONAL)
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default

    val liveData = LiveData(blaseballApi, chroniclerApi, this)
    val globalFeed = BlaseballFeed.Global(this, runBlocking { dateTimeRangeForSeason(0) }) //DateTime.now().utc .. null)

    val globalEventFeed = BlasementEventFeed(globalFeed, liveData, this)

    val fans: MutableList<BlasementHostFan> = ArrayList()
    //2021-03-01T16:00:13.446802Z

    val trace = globalFeed.flow.onEach { event ->
        println("[${event.event.created}] TRACE: $event")
    }.launchIn(this)

    suspend fun dateTimeRangeForSeason(season: Int): TimeRange {
        if (liveData.date.let { it == null || it.season == season }) {
            val simData = blaseballApi.getSimulationData().get()

            if (simData.season == season) return TimeRange(simData.godsDayDate, simData.electionDate)
        }

        if (season < 11) {
            val startingTime = chroniclerApi.getGames(
                order = EnumOrder.ASC,
                season = season,
                count = 1
            ).get().first().startTime

            val endTime = chroniclerApi.getGames(
                order = EnumOrder.DESC,
                season = season,
                count = 1
            ).get().first().endTime

            return TimeRange(format.parse(startingTime), format.parse(endTime))
        } else {
            val startingTime = chroniclerApi.getGames(
                order = EnumOrder.ASC,
                season = season,
                count = 1
            ).get().first().startTime

            println("Starting: $startingTime")

            val simulationData = httpClient.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities?type=sim&at=${startingTime}&count=1")
                .get()
                .also { println(it) }
                .getValue("items")
                .jsonArray
                .first()
                .jsonObject
                .getValue("data")
                .jsonObject

            return TimeRange(
                format.parse(simulationData.getValue("godsDayDate").jsonPrimitive.content),
                format.parse(simulationData.getValue("electionDate").jsonPrimitive.content)
            )
        }
    }

//    var connectionFactory: ConnectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///test?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
//
//    var client: DatabaseClient = DatabaseClient.create(connectionFactory)

    val connectionFactory = H2ConnectionFactory(
        H2ConnectionConfiguration.builder()
            .file("./blasement")
            .property(H2ConnectionOption.DB_CLOSE_DELAY, "-1")
            .property(H2ConnectionOption.AUTO_SERVER, "true")
            .build()
    )

    val blasementInitialisationJob = launch {
        val connection = connectionFactory.create().awaitSingle()

        try {
            println("Opened connection: $connection")

            connection.createStatement("CREATE TABLE IF NOT EXISTS fans (fan_id VARCHAR(64) NOT NULL, fan_name VARCHAR(128) NOT NULL, coins BIGINT NOT NULL DEFAULT 250, idol VARCHAR(64), favourite_team VARCHAR(64) NOT NULL, has_unlocked_shop BOOLEAN NOT NULL DEFAULT FALSE, has_unlocked_elections BOOLEAN NOT NULL DEFAULT FALSE, inventory_space INT NOT NULL DEFAULT 8);")
                .execute()
                .awaitSingle()
                .rowsUpdated
                .awaitSingle()

            connection.createStatement("CREATE TABLE IF NOT EXISTS items (id IDENTITY PRIMARY KEY, fan_id VARCHAR(64) NOT NULL, item_name VARCHAR(64) NOT NULL, quantity INT NOT NULL DEFAULT 0);")
                .execute()
                .awaitSingleOrNull()

            connection.createStatement("CREATE TABLE IF NOT EXISTS bets (id IDENTITY PRIMARY KEY, fan_id VARCHAR(64) NOT NULL, game_id VARCHAR(64) NOT NULL, team_id VARCHAR(64) NOT NULL, amount INT NOT NULL);")
                .execute()
                .awaitSingleOrNull()

            connection.createStatement("SELECT fan_id, fan_name, coins, idol, favourite_team, has_unlocked_shop, has_unlocked_elections, inventory_space FROM fans;")
                .execute()
                .awaitSingle()
                .map { row, _ ->
                    BlasementFanDatabasePayload(
                        FanID(row["fan_id"] as String),
                        row["fan_name"] as String,
                        row["coins"] as Long,
                        (row["idol"] as? String)?.let(::PlayerID),
                        TeamID(row["favourite_team"] as String),
                        row["has_unlocked_shop"] as Boolean,
                        row["has_unlocked_elections"] as Boolean,
                        row["inventory_space"] as Int
                    )
                }.asFlow()
                .collect { databasePayload ->
                    val items =
                        connection.createStatement("SELECT item_name, quantity FROM items WHERE fan_id = $1")
                            .bind("$1", databasePayload.fanID.id)
                            .execute()
                            .awaitSingle()
                            .map { row, _ ->
                                val name = row["item_name"] as String
                                val item = EnumBlaseballItem.values().firstOrNull { it.name == name }
                                           ?: return@map null

                                Pair(item, row["quantity"] as Int)
                            }.collectList().awaitSingle()


                    val bets =
                        connection.createStatement("SELECT game_id, team_id, amount FROM bets WHERE fan_id = $1")
                            .bind("$1", databasePayload.fanID.id)
                            .execute()
                            .awaitSingle()
                            .map { row, _ ->
                                Triple(
                                    row["game_id"] as String,
                                    row["team_id"] as String,
                                    row["amount"] as Int
                                )
                            }.collectList().awaitSingle()

                    fans.add(BlasementHostFan(databasePayload, this@TheBlasement, items, bets))

                }
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        } finally {
            connection.close().awaitSingleOrNull()
        }
    }

    /** Sub Event Feeds */

    private val gamesToday = ValueCache { date: BlaseballDate -> blaseballApi.getGamesByDate(date.season, date.day).get() }
    private val gamesTomorrow = ValueCache { date: BlaseballDate -> blaseballApi.getGamesByDate(date.season, date.day + 1).get() }

    val key = File(".secretkey").let { file ->
        if (file.exists()) Keys.hmacShaKeyFor(file.readBytes())
        else {
            val key = Keys.secretKeyFor(SignatureAlgorithm.HS512)
            file.writeBytes(key.encoded)
            key
        }
    }
    val parser = Jwts.parserBuilder().setSigningKey(key).build()

    suspend fun today(): BlaseballDate =
        liveData.date ?: blaseballApi.getSimulationData().get().run { BlaseballDate(season, day) }

    suspend fun gamesToday(): List<BlaseballDatabaseGame> =
        gamesToday.get(today())

    suspend fun gamesTomorrow(): List<BlaseballDatabaseGame> =
        gamesTomorrow.get(today())

    suspend fun newFan(name: String, favouriteTeam: TeamID): Pair<String, BlasementHostFan> {
        val fanID = UUID.randomUUID().toString()
        val authToken = Jwts.builder()
            .setSubject(name)
            .setId(fanID)
            .signWith(key)
            .compact()

        val fan = BlasementHostFan(
            this,
            FanID(fanID),
            name,
            coins = 250,
            favouriteTeam = favouriteTeam,
        )

        fan.apply {
            setItemQuantity(EnumBlaseballItem.VOTES) { 1 }
            setItemQuantity(EnumBlaseballItem.SNAKE_OIL) { 1 }
            setItemQuantity(EnumBlaseballItem.PEANUTS) { 10 }
        }

        val connection = connectionFactory
            .create()
            .awaitSingle()

        try {
            connection.createStatement("INSERT INTO fans (FAN_ID, FAN_NAME, COINS, IDOL, FAVOURITE_TEAM, HAS_UNLOCKED_SHOP, HAS_UNLOCKED_ELECTIONS) VALUES ( $1, $2, $3, $4, $5, $6, $7 )")
                .bind("$1", fan.id.id)
                .bind("$2", fan.name)
                .bind("$3", fan.coins)
                .bindNullable("$4", fan.idol?.id)
                .bind("$5", fan.favouriteTeam.id)
                .bind("$6", fan.hasUnlockedShop)
                .bind("$7", fan.hasUnlockedElections)
                .execute()
                .awaitSingle()
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        } finally {
            connection.close()
        }

        fans.add(fan)
        return Pair(authToken, fan)
    }

    val bettingRewards = globalEventFeed.onGameEnd.onEach { event ->
        val gameID = event.gameStep.id
        val winner = event.winner

        fans.forEach { fan ->
            val bet = fan.gameCompleted(gameID) ?: return@forEach

            if (bet.team == winner) {
                val returns = when (bet.team) {
                    event.gameStep.homeTeam -> BettingPayouts.currentSeason(bet.bet, event.gameStep.homeOdds)
                    event.gameStep.awayTeam -> BettingPayouts.currentSeason(bet.bet, event.gameStep.awayOdds)
                    else -> bet.bet
                }

                fan.setCoins { it + returns }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.WonBet(gameID, bet.team, bet.bet, returns))
            } else {
                fan.fanEvents.emit(ServerEvent.FanActionResponse.LostBet(gameID, bet.team, bet.bet))
            }
        }
    }.launchIn(this)

    val popcornRewards = globalEventFeed.onGameEnd.onEach { event ->
        val winner = event.winner

        fans.forEach { fan ->
            if (fan.favouriteTeam == winner) {
                val popcornCount = fan.inventory[EnumBlaseballItem.POPCORN]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballItem.POPCORN[popcornCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.POPCORN))
            }
        }
    }.launchIn(this)

    val stalePopcornRewards = globalEventFeed.onGameEnd.onEach { event ->
        val loser = event.gameStep.homeTeam.takeUnless { it == event.winner } ?: event.gameStep.awayTeam

        fans.forEach { fan ->
            if (fan.favouriteTeam == loser) {
                val snackCount = fan.inventory[EnumBlaseballItem.STALE_POPCORN]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballItem.STALE_POPCORN[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.STALE_POPCORN))
            }
        }
    }.launchIn(this)

//    val breakfastRewards =
    /** Earn coins every time your Team shames another Team. */
    val taffyRewards = globalEventFeed.onTeamShames.onEach { event ->
        val team = event.team

        fans.forEach { fan ->
            if (fan.favouriteTeam == team) {
                val snackCount = fan.inventory[EnumBlaseballItem.TAFFY]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballItem.TAFFY[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.TAFFY))
            }
        }
    }.launchIn(this)

    /** Earn coins every time your Team gets shamed. */
    val lemonadeRewards = globalEventFeed.onTeamShamed.onEach { event ->
        val team = event.team

        fans.forEach { fan ->
            if (fan.favouriteTeam == team) {
                val snackCount = fan.inventory[EnumBlaseballItem.LEMONADE]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballItem.LEMONADE[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.LEMONADE))
            }
        }
    }.launchIn(this)

    /** Crisp. Earn 3 coins when your Idol strikes a batter out. */
    val chipRewards = globalEventFeed.onStrikeout.onEach { event ->
        val pitcher = event.gameStep.homePitcher ?: event.gameStep.awayPitcher!!

        fans.forEach { fan ->
            if (fan.idol == pitcher) {
                val snackCount = fan.inventory[EnumBlaseballItem.CHIPS]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballItem.CHIPS[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.CHIPS))
            }
        }
    }.launchIn(this)

    /** Medium Rare. Earn coins when your Idol pitches a shutout. */
    val burgerRewards = globalEventFeed.onShutout.onEach { event ->
        val pitcher = event.gameStep.homePitcher ?: event.gameStep.awayPitcher!!

        fans.forEach { fan ->
            if (fan.idol == pitcher) {
                val snackCount = fan.inventory[EnumBlaseballItem.BURGER]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballItem.BURGER[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.BURGER))
            }
        }
    }.launchIn(this)

    /** Uh oh. Earn coins when a batter hits a home run off of your Idol's pitch. */
    val meatballRewards = globalEventFeed.onHomeRun.onEach { event ->
        val pitcher = event.gameStep.homePitcher ?: event.gameStep.awayPitcher!!

        fans.forEach { fan ->
            if (fan.idol == pitcher) {
                val snackCount = fan.inventory[EnumBlaseballItem.MEATBALL]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballItem.MEATBALL[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.MEATBALL))
            }
        }
    }.launchIn(this)

    /** Hot Dog! Earn coins when your Idol hits a home run. */
    val hotDogRewards = globalEventFeed.onHomeRun.onEach { event ->
        val batter = event.gameStep.homeBatter ?: event.gameStep.awayBatter!!

        fans.forEach { fan ->
            if (fan.idol == batter) {
                val snackCount = fan.inventory[EnumBlaseballItem.HOT_DOG]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballItem.HOT_DOG[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.HOT_DOG))
            }
        }
    }.launchIn(this)

    /** Ptooie. Earn 5 coins when your Idol gets a hit. */
    val sunflowerSeedRewards = globalEventFeed.onHit.onEach { event ->
        val batter = event.gameStep.homeBatter ?: event.gameStep.awayBatter!!

        fans.forEach { fan ->
            if (fan.idol == batter) {
                val snackCount = fan.inventory[EnumBlaseballItem.SUNFLOWER_SEEDS]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballItem.SUNFLOWER_SEEDS[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.SUNFLOWER_SEEDS))
            }
        }
    }.launchIn(this)

    /** Ptooie. Earn coins every time your Idol steals a base. */
    val pickleRewards = globalEventFeed.onStolenBase.onEach { event ->
        val batter = event.event.playerTags.firstOrNull() ?: return@onEach

        fans.forEach { fan ->
            if (fan.idol == batter) {
                val snackCount = fan.inventory[EnumBlaseballItem.PICKLES]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballItem.PICKLES[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.PICKLES))
            }
        }
    }.launchIn(this)

    /** Ptooie. Earn coins for every baserunner swept away by Flooding weather across the league. */
    val slushieRewards = globalEventFeed.onFlood.onEach { event ->
        fans.forEach { fan ->
            val snackCount = fan.inventory[EnumBlaseballItem.SLUSHIE]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballItem.SLUSHIE[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = ((payoutRate * event.playersFlooded.size) * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.SLUSHIE))
        }
    }.launchIn(this)

    /** Refreshing. Earn coins every time a Player is incinerated. */
    val sundaeRewards = globalEventFeed.onIncineration.onEach { event ->
        fans.forEach { fan ->
            val snackCount = fan.inventory[EnumBlaseballItem.SUNDAE]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballItem.SUNDAE[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.SUNDAE))
        }
    }.launchIn(this)

    /** Earn coins for every time the Black Hole swallows a Win from any Team. */
    val wetPretzelRewards = globalEventFeed.onBlackHole.onEach { event ->
        fans.forEach { fan ->
            val snackCount = fan.inventory[EnumBlaseballItem.WET_PRETZEL]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballItem.WET_PRETZEL[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.WET_PRETZEL))
        }
    }.launchIn(this)

    /** Earn coins for every time Sun 2 sets a Win on a Team. */
    val doughnutRewards = globalEventFeed.onSun2.onEach { event ->
        fans.forEach { fan ->
            val snackCount = fan.inventory[EnumBlaseballItem.DOUGHNUT]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballItem.DOUGHNUT[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.DOUGHNUT))
        }
    }.launchIn(this)
}