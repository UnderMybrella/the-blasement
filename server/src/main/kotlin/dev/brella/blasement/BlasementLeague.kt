package dev.brella.blasement

import BlasementEventFeed
import BlasementHostFan
import TheBlasement
import dev.brella.blasement.blaseback.BlasementDataSource
import dev.brella.blasement.common.*
import dev.brella.blasement.common.events.*
import dev.brella.kornea.blaseball.base.common.BettingPayouts
import dev.brella.kornea.blaseball.base.common.EnumBlaseballSnack
import dev.brella.kornea.blaseball.base.common.UUID
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.MissingClaimException
import io.jsonwebtoken.security.SignatureException
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*
import kotlin.math.roundToInt

class BlasementLeague(val blasement: TheBlasement, val source: BlasementDataSource, val leagueType: String, val leagueID: String, val baseUrl: String) {
    inline val client: HttpClient get() = blasement.httpClient

    val parser = blasement.parser()
        .requireAudience("$leagueType-$leagueID")
        .build()

    inline val ApplicationCall.authToken: String?
        get() = request.header("Authorization") ?: request.cookies[TheBlasement.COOKIE_NAME]

    inline val ApplicationCall.authJwt: Jws<Claims>?
        get() = try {
            authToken?.let { parser.parseClaimsJws(it) }
        } catch (th: Throwable) {
            when (th) {
                is JwtException, is SignatureException, is IllegalArgumentException, is MissingClaimException -> null
                else -> throw th
            }
        }

    inline val ApplicationCall.fan: BlasementHostFan?
        get() = authJwt?.let { fans[UUID.fromString(it.body.id)] }

    inline infix fun fanFor(call: ApplicationCall) = call.fan

    val siteData = BlasementSiteData(blasement, source, baseUrl)
    val siteDataLaunch = siteData.launch(blasement)

    /** Rewards Calculation */
    val globalEventFeed = BlasementEventFeed(source.globalFeed, blasement)
    val fans get() = blasement.fans

    val trace = source.globalFeed.onEach { event ->
//        println(event.event)
    }.launchIn(blasement)

    val bettingRewards = globalEventFeed.onGameEnd.onEach { event ->
//        println("Game Ending: ${event.gameStep.id} (${event.gameStep.homeTeamName} vs ${event.gameStep.awayTeamName}; ${event.winner} won)")
        val gameID = event.gameStep.id
        val winner = event.winner

        val time = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            val bet = fan.gameCompleted(gameID) ?: return@forEach

            if (bet.team == winner) {
                val returns = when (bet.team) {
                    event.gameStep.homeTeam -> BettingPayouts.currentSeason(bet.bet, event.gameStep.homeOdds)
                    event.gameStep.awayTeam -> BettingPayouts.currentSeason(bet.bet, event.gameStep.awayOdds)
                    else -> bet.bet
                }

                fan.setCoins { it + returns }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.WonBet(gameID, bet.team, bet.bet, returns))

                fan.addToast("You bet ${bet.bet} on the {tnn}${bet.team.id} and won ${returns} coins.", time)
            } else {
                fan.fanEvents.emit(ServerEvent.FanActionResponse.LostBet(gameID, bet.team, bet.bet))

                fan.addToast("You bet ${bet.bet} on the {tnn}${bet.team.id} and lost.", time)
            }
        }
    }.launchIn(blasement)

    val popcornRewards = globalEventFeed.onGameEnd.onEach { event ->
        val winner = event.winner
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            if (fan.favouriteTeam == winner) {
                val popcornCount = fan.inventory[EnumBlaseballSnack.POPCORN]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.POPCORN[popcornCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.POPCORN))
                fan.addToast("You earned $coinPayout coins from 1 {tnn}${winner} win.", eventTime)
            }
        }
    }.launchIn(blasement)

    val stalePopcornRewards = globalEventFeed.onGameEnd.onEach { event ->
        val loser = event.gameStep.homeTeam.takeUnless { it == event.winner } ?: event.gameStep.awayTeam
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            if (fan.favouriteTeam == loser) {
                val snackCount = fan.inventory[EnumBlaseballSnack.STALE_POPCORN]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.STALE_POPCORN[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.STALE_POPCORN))
                fan.addToast("You earned $coinPayout coins from 1 {tnn}${loser} loss.", eventTime)
            }
        }
    }.launchIn(blasement)

//    val breakfastRewards =
    /** Earn coins every time your Team shames another Team. */
    val taffyRewards = globalEventFeed.onTeamShames.onEach { event ->
        val team = event.team
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            if (fan.favouriteTeam == team) {
                val snackCount = fan.inventory[EnumBlaseballSnack.TAFFY]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.TAFFY[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.TAFFY))
                fan.addToast("You earned $coinPayout coins from {tnn}${team} shaming 1 time.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Earn coins every time your Team gets shamed. */
    val lemonadeRewards = globalEventFeed.onTeamShamed.onEach { event ->
        val team = event.team
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            if (fan.favouriteTeam == team) {
                val snackCount = fan.inventory[EnumBlaseballSnack.LEMONADE]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.LEMONADE[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.LEMONADE))
                fan.addToast("You earned $coinPayout coins from {tnn}${team} being shamed 1 time.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Crisp. Earn 3 coins when your Idol strikes a batter out. */
    val chipRewards = globalEventFeed.onStrikeout.onEach { event ->
        val pitcher = event.gameStep.homePitcher ?: event.gameStep.awayPitcher!!
        val pitcherName = if (pitcher == event.gameStep.homePitcher) event.gameStep.homePitcherName else event.gameStep.awayPitcherName

        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            if (fan.idol == pitcher) {
                val snackCount = fan.inventory[EnumBlaseballSnack.CHIPS]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.CHIPS[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.CHIPS))
//                fan.addToast("You earned $coinPayout coins from ", eventTime)
                fan.addToast("You earned $coinPayout coins from $pitcherName striking a batter out.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Medium Rare. Earn coins when your Idol pitches a shutout. */
    val burgerRewards = globalEventFeed.onShutout.onEach { event ->
        val pitcher = event.gameStep.homePitcher ?: event.gameStep.awayPitcher!!
        val pitcherName = if (pitcher == event.gameStep.homePitcher) event.gameStep.homePitcherName else event.gameStep.awayPitcherName
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            if (fan.idol == pitcher) {
                val snackCount = fan.inventory[EnumBlaseballSnack.BURGER]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.BURGER[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.BURGER))
                fan.addToast("You earned $coinPayout coins from $pitcherName pitching a shutout.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Uh oh. Earn coins when a batter hits a home run off of your Idol's pitch. */
    val meatballRewards = globalEventFeed.onHomeRun.onEach { event ->
        val pitcher = event.gameStep.homePitcher ?: event.gameStep.awayPitcher!!
        val pitcherName = if (pitcher == event.gameStep.homePitcher) event.gameStep.homePitcherName else event.gameStep.awayPitcherName
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            if (fan.idol == pitcher) {
                val snackCount = fan.inventory[EnumBlaseballSnack.MEATBALL]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.MEATBALL[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.MEATBALL))
                fan.addToast("You earned $coinPayout coins from a batter hitting a home run off of $pitcherName's pitch'.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Hot Dog! Earn coins when your Idol hits a home run. */
    val hotDogRewards = globalEventFeed.onHomeRun.onEach { event ->
        val batter = event.gameStep.homeBatter ?: event.gameStep.awayBatter!!
        val batterName = if (batter == event.gameStep.homeBatter) event.gameStep.homeBatterName else event.gameStep.awayBatterName
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            if (fan.idol == batter) {
                val snackCount = fan.inventory[EnumBlaseballSnack.HOT_DOG]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.HOT_DOG[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.HOT_DOG))
                fan.addToast("You earned $coinPayout coins from $batterName hitting a home run.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Ptooie. Earn 5 coins when your Idol gets a hit. */
    val sunflowerSeedRewards = globalEventFeed.onHit.onEach { event ->
        val batter = event.gameStep.homeBatter ?: event.gameStep.awayBatter!!
        val batterName = if (batter == event.gameStep.homeBatter) event.gameStep.homeBatterName else event.gameStep.awayBatterName
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            if (fan.idol == batter) {
                val snackCount = fan.inventory[EnumBlaseballSnack.SUNFLOWER_SEEDS]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.SUNFLOWER_SEEDS[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.SUNFLOWER_SEEDS))
                fan.addToast("You earned $coinPayout coins from $batterName getting a hit.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Ptooie. Earn coins every time your Idol steals a base. */
    val pickleRewards = globalEventFeed.onStolenBase.onEach { event ->
        val batter = event.event.playerTags.firstOrNull() ?: return@onEach
        val batterName = if (batter == event.gameStep.homeBatter) event.gameStep.homeBatterName else event.gameStep.awayBatterName
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            if (fan.idol == batter) {
                val snackCount = fan.inventory[EnumBlaseballSnack.PICKLES]?.takeIf { it > 0 } ?: return@forEach
                val payoutRate = EnumBlaseballSnack.PICKLES[snackCount - 1]?.payout ?: return@forEach
                val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

                fan.setCoins { it + coinPayout }
                fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.PICKLES))
                fan.addToast("You earned $coinPayout coins from $batterName stealing a base.", eventTime)
            }
        }
    }.launchIn(blasement)

    /** Ptooie. Earn coins for every baserunner swept away by Flooding weather across the league. */
    val slushieRewards = globalEventFeed.onFlood.onEach { event ->
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            val snackCount = fan.inventory[EnumBlaseballSnack.SLUSHIE]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballSnack.SLUSHIE[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = ((payoutRate * event.playersFlooded.size) * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.SLUSHIE))
            fan.addToast("You earned $coinPayout coins from ${event.playersFlooded.size} Baserunners being cleared in Flood weather.", eventTime)
        }
    }.launchIn(blasement)

    /** Refreshing. Earn coins every time a Player is incinerated. */
    val sundaeRewards = globalEventFeed.onIncineration.onEach { event ->
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            val snackCount = fan.inventory[EnumBlaseballSnack.SUNDAE]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballSnack.SUNDAE[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.SUNDAE))
            fan.addToast("You earned $coinPayout coins from 1 Incinerations; RIV.", eventTime)
        }
    }.launchIn(blasement)

    /** Earn coins for every time the Black Hole swallows a Win from any Team. */
    val wetPretzelRewards = globalEventFeed.onBlackHole.onEach { event ->
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            val snackCount = fan.inventory[EnumBlaseballSnack.WET_PRETZEL]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballSnack.WET_PRETZEL[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.WET_PRETZEL))
            fan.addToast("You earned $coinPayout coins from Black Holes being activated 1 time.", eventTime)
        }
    }.launchIn(blasement)

    /** Earn coins for every time Sun 2 sets a Win on a Team. */
    val doughnutRewards = globalEventFeed.onSun2.onEach { event ->
        val eventTime = event.event.created.utc.unixMillisLong

        fans.forEach { (_, fan) ->
            if (fan.activeLeagueType != leagueType || fan.activeLeagueID != leagueID) return@forEach

            val snackCount = fan.inventory[EnumBlaseballSnack.DOUGHNUT]?.takeIf { it > 0 } ?: return@forEach
            val payoutRate = EnumBlaseballSnack.DOUGHNUT[snackCount - 1]?.payout ?: return@forEach
            val coinPayout = (payoutRate * fan.payoutRate).roundToInt()

            fan.setCoins { it + coinPayout }
            fan.fanEvents.emit(ServerEvent.FanActionResponse.GainedMoney(coinPayout, EnumGainedMoneyReason.DOUGHNUT))
            fan.addToast("You earned $coinPayout coins from Sun 2 being activated 1 time.", eventTime)
        }
    }.launchIn(blasement)
}