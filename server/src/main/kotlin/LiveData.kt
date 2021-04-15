import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.GameID
import dev.brella.kornea.blaseball.ModificationID
import dev.brella.kornea.blaseball.TerminologyID
import dev.brella.kornea.blaseball.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.beans.BlaseballStreamData
import dev.brella.kornea.blaseball.beans.BlaseballStreamDataSchedule
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class LiveData(val blaseballApi: BlaseballApi, val chroniclerApi: ChroniclerApi, val scope: CoroutineScope, val context: CoroutineContext = scope.coroutineContext) {
    //    val simulationData: MutableList<BlaseballStreamData> = ArrayList()
    val games: MutableMap<Int, MutableMap<GameID, BlaseballUpdatingGame>> = HashMap()

    val firstUpdateJob = scope.launch(context) {
        var last: BlaseballStreamData? = null

        while (isActive) {
            val events = blaseballApi.getLiveDataStream()

            events?.collect { event ->
                val update = BlaseballStreamData(
                    event.games ?: last?.games,
                    event.leagues ?: last?.leagues,
                    event.temporal ?: last?.temporal,
                    event.fights ?: last?.fights
                )

                launch { parsingJob.send(update) }

                last = update
            }
        }
    }
    val secondUpdateJob = scope.launch(context) {
        var last: BlaseballStreamData? = null

        delay(2_000)

        while (isActive) {
            val events = blaseballApi.getLiveDataStream()

            events?.collect { event ->
                val update = BlaseballStreamData(
                    event.games ?: last?.games,
                    event.leagues ?: last?.leagues,
                    event.temporal ?: last?.temporal,
                    event.fights ?: last?.fights
                )

                launch { parsingJob.send(update) }

                last = update
            }
        }
    }

    var date: BlaseballDate? = null

    val parsingJob = scope.actor<BlaseballStreamData>(context, Channel.UNLIMITED) {
        while (isActive) {
            val update = receiveOrNull() ?: break
            val gamesData = update.games ?: continue

            val date = BlaseballDate(gamesData.sim.season, gamesData.sim.day)
            this@LiveData.date = date
            val gamesForToday = games.computeIfAbsent(date.data) { HashMap() }

            gamesData.schedule.forEach { game ->
                val thisGame = gamesForToday.computeIfAbsent(GameID(game.id.id)) { BlaseballUpdatingGame() }
                thisGame.issueUpdate(game)
            }
        }
    }

    suspend fun getLocalGame(season: Int, day: Int, game: GameID): BlaseballUpdatingGame? =
        games[(season shl 8) or (day)]?.get(game)

    suspend fun getGame(season: Int, day: Int, game: GameID): Map<Int, BlaseballDatabaseGame>? {
        val dayKey = (season shl 8) or (day)
        val thisGame = games[dayKey]?.get(game)

        if (thisGame != null) {
            return thisGame.getUpdates().withIndex().mapNotNull { if (it.value == null) null else it as IndexedValue<BlaseballStreamDataSchedule> }.associate { (i, v) ->
                i to BlaseballDatabaseGame(
                    GameID(v.id.id),
                    v.basesOccupied,
                    v.baseRunners,
                    v.baseRunnerNames,
                    v.outcomes,
                    TerminologyID(v.terminology),
                    v.lastUpdate,
                    v.rules,
                    v.statsheet,
                    v.awayPitcher,
                    v.awayPitcherName,
                    v.awayBatter,
                    v.awayBatterName,
                    v.awayTeam,
                    v.awayTeamName,
                    v.awayTeamNickname,
                    v.awayTeamColor,
                    v.awayTeamEmoji,
                    v.awayOdds,
                    v.awayStrikes,
                    v.awayScore,
                    v.awayTeamBatterCount,
                    v.homePitcher,
                    v.homePitcherName,
                    v.homeBatter,
                    v.homeBatterName,
                    v.homeTeam,
                    v.homeTeamName,
                    v.homeTeamNickname,
                    v.homeTeamColor,
                    v.homeTeamEmoji,
                    v.homeOdds,
                    v.homeStrikes,
                    v.homeScore,
                    v.homeTeamBatterCount,
                    v.season,
                    v.isPostseason,
                    v.day,
                    v.phase,
                    v.gameComplete,
                    v.finalized,
                    v.gameStart,
                    v.halfInningOuts,
                    v.halfInningScore,
                    v.inning,
                    v.topOfInning,
                    v.atBatBalls,
                    v.atBatStrikes,
                    v.seriesIndex,
                    v.seriesLength,
                    v.shame,
                    v.weather,
                    v.baserunnerCount,
                    v.homeBases,
                    v.awayBases,
                    v.repeatCount,
                    v.awayTeamSecondaryColor,
                    v.homeTeamSecondaryColor,
                    v.homeBalls,
                    v.awayBalls,
                    v.homeOuts,
                    v.awayOuts,
                    v.playCount,
                    v.tournament,
                    v.baseRunnerMods,
                    v.homePitcherMod,
                    v.homeBatterMod,
                    v.awayPitcherMod,
                    v.awayBatterMod,
                    v.scoreUpdate,
                    v.scoreLedger,
                    v.stadiumId,
                    v.secretBaserunner,
                    v.topInningScore,
                    v.bottomInningScore,
                    v.newInningPhase,
                    v.gameStartPhase,
                    v.isTitleMatch
                )
            }
        }

        val updates = chroniclerApi.getGameUpdates(1_000, game = listOf(game))

        return updates?.data?.associate { wrapper ->
            wrapper.data.playCount!! to BlaseballDatabaseGame(
                wrapper.data.id,
                wrapper.data.basesOccupied,
                wrapper.data.baseRunners,
                wrapper.data.baseRunnerNames ?: emptyList(),
                wrapper.data.outcomes,
                wrapper.data.terminology,
                wrapper.data.lastUpdate,
                wrapper.data.rules,
                wrapper.data.statsheet,
                wrapper.data.awayPitcher,
                wrapper.data.awayPitcherName,
                wrapper.data.awayBatter,
                wrapper.data.awayBatterName,
                wrapper.data.awayTeam,
                wrapper.data.awayTeamName,
                wrapper.data.awayTeamNickname,
                wrapper.data.awayTeamColor,
                wrapper.data.awayTeamEmoji,
                wrapper.data.awayOdds,
                wrapper.data.awayStrikes,
                wrapper.data.awayScore,
                wrapper.data.awayTeamBatterCount,
                wrapper.data.homePitcher,
                wrapper.data.homePitcherName,
                wrapper.data.homeBatter,
                wrapper.data.homeBatterName,
                wrapper.data.homeTeam,
                wrapper.data.homeTeamName,
                wrapper.data.homeTeamNickname,
                wrapper.data.homeTeamColor,
                wrapper.data.homeTeamEmoji,
                wrapper.data.homeOdds,
                wrapper.data.homeStrikes,
                wrapper.data.homeScore,
                wrapper.data.homeTeamBatterCount,
                wrapper.data.season,
                wrapper.data.isPostseason,
                wrapper.data.day,
                wrapper.data.phase,
                wrapper.data.gameComplete,
                wrapper.data.finalized,
                wrapper.data.gameStart,
                wrapper.data.halfInningOuts,
                wrapper.data.halfInningScore,
                wrapper.data.inning,
                wrapper.data.topOfInning,
                wrapper.data.atBatBalls,
                wrapper.data.atBatStrikes,
                wrapper.data.seriesIndex,
                wrapper.data.seriesLength,
                wrapper.data.shame,
                wrapper.data.weather,
                wrapper.data.baserunnerCount,
                wrapper.data.homeBases ?: 0.0,
                wrapper.data.awayBases ?: 0.0,
                wrapper.data.repeatCount ?: 0,
                wrapper.data.awayTeamSecondaryColor,
                wrapper.data.homeTeamSecondaryColor,
                wrapper.data.homeBalls ?: 0,
                wrapper.data.awayBalls ?: 0,
                wrapper.data.homeOuts ?: 0,
                wrapper.data.awayOuts ?: 0,
                wrapper.data.playCount!!,
                wrapper.data.tournament ?: -1,
                wrapper.data.baseRunnerMods ?: emptyList(),
                wrapper.data.homePitcherMod ?: ModificationID(""),
                wrapper.data.homeBatterMod ?: ModificationID(""),
                wrapper.data.awayPitcherMod ?: ModificationID(""),
                wrapper.data.awayBatterMod ?: ModificationID(""),
                wrapper.data.scoreUpdate ?: "",
                wrapper.data.scoreLedger ?: "",
                wrapper.data.stadiumId,
                wrapper.data.secretBaserunner,
                wrapper.data.topInningScore,
                wrapper.data.bottomInningScore,
                wrapper.data.newInningPhase,
                wrapper.data.gameStartPhase,
                wrapper.data.isTitleMatch
            )
        }
    }
}