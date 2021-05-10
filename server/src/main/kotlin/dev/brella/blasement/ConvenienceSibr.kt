package dev.brella.blasement

import buildKotlin
import com.github.benmanes.caffeine.cache.Caffeine
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import dev.brella.blasement.utils.chroniclerEntity
import dev.brella.blasement.utils.chroniclerEntityList
import dev.brella.blasement.utils.toStableString
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.DivisionID
import dev.brella.kornea.blaseball.base.common.LeagueID
import dev.brella.kornea.blaseball.base.common.ModificationID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.StadiumID
import dev.brella.kornea.blaseball.base.common.SubleagueID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballDivision
import dev.brella.kornea.blaseball.base.common.beans.BlaseballLeague
import dev.brella.kornea.blaseball.base.common.beans.BlaseballSubleague
import dev.brella.kornea.blaseball.base.common.beans.BlaseballTeam
import dev.brella.kornea.blaseball.base.common.beans.Colour
import dev.brella.kornea.blaseball.base.common.beans.ColourAsHexSerialiser
import dev.brella.kornea.blaseball.base.common.beans.unknown
import dev.brella.kornea.errors.common.flatMap
import dev.brella.kornea.errors.common.map
import getAsync
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import respond
import java.util.concurrent.TimeUnit

val LEAGUE_TEAMS = Caffeine.newBuilder()
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .buildKotlin<String, List<ConvenienceLeague>>()

@Serializable
data class ConvenienceLeague(
    val id: LeagueID,
    val subleagues: List<ConvenienceSubLeague>,
    val name: String,
    val tiebreakers: String
)

@Serializable
data class ConvenienceSubLeague(
    val id: SubleagueID,
    val divisions: List<ConvenienceDivision>,
    val name: String
)

@Serializable
data class ConvenienceDivision(
    val id: DivisionID,
    val teams: List<ChroniclerBlaseballTeam>,
    val name: String
)

@Serializable
data class ChroniclerBlaseballTeam(
    val id: TeamID,
    val lineup: List<PlayerID>,
    val rotation: List<PlayerID>,
    val bullpen: List<PlayerID>,
    val bench: List<PlayerID>,
    val fullName: String,
    val location: String,
    val mainColor: @Serializable(ColourAsHexSerialiser::class) Colour,
    val nickname: String,
    val secondaryColor: @Serializable(ColourAsHexSerialiser::class) Colour,
    val shorthand: String,
    val emoji: String,
    val slogan: String,
    val shameRuns: Double,
    val totalShames: Int,
    val totalShamings: Int,
    val seasonShames: Int,
    val seasonShamings: Int,
    val championships: Int,
    val rotationSlot: Int,
    val weekAttr: List<ModificationID>,
    val gameAttr: List<ModificationID>,
    val seasAttr: List<ModificationID>,
    val permAttr: List<ModificationID>,
    val teamSpirit: Double,
    val card: Int? = null,
    /*   */
    val tournamentWins: Int? = null,
    val stadium: StadiumID? = null,
    val imPosition: Double? = null,
    val eDensity: Double? = null,
    val eVelocity: Double? = null,
    val state: unknown? = null,
    val evolution: Double? = null,
    val winStreak: Double? = null,
    val level: Int? = null
)

fun Route.convenience(client: HttpClient) {
    get("/league_teams") {
        LEAGUE_TEAMS.getAsync(call.request.queryParameters["at"] ?: BLASEBALL_TIME_PATTERN.format(DateTime.now().utc)) { time ->
            val leaguesResponse = client.chroniclerEntityList<BlaseballLeague>("league", at = time)
            val subleaguesResponse = client.chroniclerEntityList<BlaseballSubleague>("subleague", at = time)
                .map { list -> list.associateBy(BlaseballSubleague::id) }
            val divisionsResponse = client.chroniclerEntityList<BlaseballDivision>("division", at = time)
                .map { list -> list.associateBy(BlaseballDivision::id) }
            val teamsResponse = client.chroniclerEntityList<ChroniclerBlaseballTeam>("team", at = time)
                .map { list -> list.associateBy(ChroniclerBlaseballTeam::id) }

            leaguesResponse.flatMap { leagues ->
                subleaguesResponse.flatMap { subleagues ->
                    divisionsResponse.flatMap { divisions ->
                        teamsResponse.map { teams ->
                            leagues.map { league ->
                                ConvenienceLeague(
                                    league.id,
                                    league.subleagues.mapNotNull { subleagueID ->
                                        subleagues[subleagueID]?.let { subleague ->
                                            ConvenienceSubLeague(
                                                subleague.id,
                                                subleague.divisions.mapNotNull { divisionID ->
                                                    divisions[divisionID]?.let { division ->
                                                        ConvenienceDivision(
                                                            division.id,
                                                            division.teams.mapNotNull(teams::get),
                                                            division.name
                                                        )
                                                    }
                                                },
                                                subleague.name
                                            )
                                        }
                                    },
                                    league.name,
                                    league.tiebreakers
                                )
                            }
                        }
                    }
                }
            }
        }.respond(call)
    }
}