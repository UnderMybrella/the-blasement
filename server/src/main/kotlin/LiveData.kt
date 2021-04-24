import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.ModificationID
import dev.brella.kornea.blaseball.base.common.TerminologyID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.base.common.beans.BlaseballStreamData
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import dev.brella.kornea.errors.common.doOnFailure
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.doOnThrown
import dev.brella.kornea.errors.common.getOrBreak
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class LiveData(val blaseballApi: BlaseballApi, val chroniclerApi: ChroniclerApi, val scope: CoroutineScope, val context: CoroutineContext = scope.coroutineContext) {
    //    val simulationData: MutableList<BlaseballStreamData> = ArrayList()
    val games: MutableMap<GameID, BlaseballUpdatingGame> = HashMap()
    val chroniclerGames: MutableMap<GameID, Map<Int, BlaseballDatabaseGame>> = HashMap()

    val firstUpdateJob = scope.launch(context) {
        var last: BlaseballStreamData? = null

        while (isActive) {
            blaseballApi.getLiveDataStream()
                .doOnSuccess { events ->
                    events.collect { event ->
                        val update = BlaseballStreamData(
                            event.games ?: last?.games,
                            event.leagues ?: last?.leagues,
                            event.temporal ?: last?.temporal,
                            event.fights ?: last?.fights
                        )

                        launch { parsingJob.send(update) }

                        last = update
                    }
                }.doOnThrown { error -> error.exception.printStackTrace() }
                .doOnFailure { delay(500L + Random.nextLong(100)) }
        }
    }
    val secondUpdateJob = scope.launch(context) {
        var last: BlaseballStreamData? = null

        delay(2_000)

        while (isActive) {
            blaseballApi.getLiveDataStream()
                .doOnSuccess { events ->
                    events.collect { event ->
                        val update = BlaseballStreamData(
                            event.games ?: last?.games,
                            event.leagues ?: last?.leagues,
                            event.temporal ?: last?.temporal,
                            event.fights ?: last?.fights
                        )

                        launch { parsingJob.send(update) }

                        last = update
                    }
                }.doOnThrown { error -> error.exception.printStackTrace() }
                .doOnFailure { delay(500L + Random.nextLong(100)) }
        }
    }

    var date: BlaseballDate? = null

    val parsingJob = scope.actor<BlaseballStreamData>(context, Channel.UNLIMITED) {
        while (isActive) {
            val update = receiveOrNull() ?: break
            val gamesData = update.games ?: continue

            val date = BlaseballDate(gamesData.sim.season, gamesData.sim.day)
            this@LiveData.date = date

            gamesData.schedule.forEach { game ->
                val thisGame = games.computeIfAbsent(GameID(game.id.id)) { BlaseballUpdatingGame() }
                thisGame.issueUpdate(game)
            }
        }
    }

    suspend fun getLocalGame(game: GameID): BlaseballUpdatingGame? =
        games[game]

    suspend fun getGame(game: GameID): Map<Int, BlaseballDatabaseGame>? {
        val thisGame = games[game]

        if (thisGame != null) {
            return thisGame.getUpdates()
                .withIndex()
                .mapNotNull { if (it.value == null) null else it as IndexedValue<BlaseballDatabaseGame> }
                .associate { (k, v) -> Pair(k, v) }
        }

        return if (game in chroniclerGames) chroniclerGames.getValue(game)
        else {
            val updates = chroniclerApi.getGameUpdates(1_000, game = listOf(game))
                .getOrBreak { return null }
                .data
                .associate { wrapper ->
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
                        wrapper.data.awayStrikes ?: 0.0,
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
                        wrapper.data.homeStrikes ?: 0.0,
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

            chroniclerGames[game] = updates
            updates
        }
    }

    suspend fun getGames(game: Iterable<GameID>): Map<GameID, Map<Int, BlaseballDatabaseGame>> {
        val map: MutableMap<GameID, Map<Int, BlaseballDatabaseGame>> = HashMap()
        val missingLocal = game.toMutableList()

        missingLocal.filter { it in games }.forEach { gameID ->
            missingLocal.remove(gameID)

            map[gameID] = games.getValue(gameID)
                .getUpdates()
                .withIndex()
                .mapNotNull { if (it.value == null) null else it as IndexedValue<BlaseballDatabaseGame> }
                .associate { (i, v) -> i to v }
        }

        missingLocal.filter { it in chroniclerGames }.forEach { gameID ->
            missingLocal.remove(gameID)

            map[gameID] = chroniclerGames.getValue(gameID)
        }

        chroniclerApi.getGameUpdates(1_000 * missingLocal.size, game = missingLocal)
            .doOnSuccess { response ->
                response.data.groupBy({ wrapper -> wrapper.gameId }, { wrapper -> wrapper.data })
                    .forEach { (gameID, updates) ->
                        chroniclerGames[gameID] = updates
                            .associate { data ->
                                data.playCount!! to BlaseballDatabaseGame(
                                    data.id,
                                    data.basesOccupied,
                                    data.baseRunners,
                                    data.baseRunnerNames ?: emptyList(),
                                    data.outcomes,
                                    data.terminology,
                                    data.lastUpdate,
                                    data.rules,
                                    data.statsheet,
                                    data.awayPitcher,
                                    data.awayPitcherName,
                                    data.awayBatter,
                                    data.awayBatterName,
                                    data.awayTeam,
                                    data.awayTeamName,
                                    data.awayTeamNickname,
                                    data.awayTeamColor,
                                    data.awayTeamEmoji,
                                    data.awayOdds,
                                    data.awayStrikes ?: 0.0,
                                    data.awayScore,
                                    data.awayTeamBatterCount,
                                    data.homePitcher,
                                    data.homePitcherName,
                                    data.homeBatter,
                                    data.homeBatterName,
                                    data.homeTeam,
                                    data.homeTeamName,
                                    data.homeTeamNickname,
                                    data.homeTeamColor,
                                    data.homeTeamEmoji,
                                    data.homeOdds,
                                    data.homeStrikes ?: 0.0,
                                    data.homeScore,
                                    data.homeTeamBatterCount,
                                    data.season,
                                    data.isPostseason,
                                    data.day,
                                    data.phase,
                                    data.gameComplete,
                                    data.finalized,
                                    data.gameStart,
                                    data.halfInningOuts,
                                    data.halfInningScore,
                                    data.inning,
                                    data.topOfInning,
                                    data.atBatBalls,
                                    data.atBatStrikes,
                                    data.seriesIndex,
                                    data.seriesLength,
                                    data.shame,
                                    data.weather,
                                    data.baserunnerCount,
                                    data.homeBases ?: 0.0,
                                    data.awayBases ?: 0.0,
                                    data.repeatCount ?: 0,
                                    data.awayTeamSecondaryColor,
                                    data.homeTeamSecondaryColor,
                                    data.homeBalls ?: 0,
                                    data.awayBalls ?: 0,
                                    data.homeOuts ?: 0,
                                    data.awayOuts ?: 0,
                                    data.playCount!!,
                                    data.tournament ?: -1,
                                    data.baseRunnerMods ?: emptyList(),
                                    data.homePitcherMod ?: ModificationID(""),
                                    data.homeBatterMod ?: ModificationID(""),
                                    data.awayPitcherMod ?: ModificationID(""),
                                    data.awayBatterMod ?: ModificationID(""),
                                    data.scoreUpdate ?: "",
                                    data.scoreLedger ?: "",
                                    data.stadiumId,
                                    data.secretBaserunner,
                                    data.topInningScore,
                                    data.bottomInningScore,
                                    data.newInningPhase,
                                    data.gameStartPhase,
                                    data.isTitleMatch
                                )
                            }

                        map[gameID] = chroniclerGames.getValue(gameID)
                    }
            }

        return map
    }
}