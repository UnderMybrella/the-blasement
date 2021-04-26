package dev.brella.blasement

import com.soywiz.klock.PatternDateFormat
import com.soywiz.klock.parse
import dev.brella.kornea.blaseball.base.common.FeedID
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballFeedEvent
import dev.brella.kornea.blaseball.base.common.beans.BlaseballFeedMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class EventuallyFeedEvent(
    val id: String,
    val playerTags: List<PlayerID>,
    val playerNames: List<String>,
    val teamTags: List<TeamID>,
    val teamNames: List<String>,
    val gameTags: List<GameID>,
    val metadata: JsonObject?,
    val created: String,
    val season: Int,
    val tournament: Int,
    val type: Int,
    val day: Int,
    val phase: Int,
    val category: Int,
    val description: String,
    val nuts: Int
)

val FORMAT = PatternDateFormat("yyyy-MM-dd'T'HH:mm:ss")

fun EventuallyFeedEvent.toBlaseball() =
    BlaseballFeedEvent.Unknown(
        FeedID(id),
        playerTags,
        teamTags,
        gameTags,
        FORMAT.parse(created),
        season,
        tournament,
        type,
        day,
        phase,
        category,
        description,
        nuts,
        BlaseballFeedMetadata.Unknown(metadata ?: emptyMap())
    )