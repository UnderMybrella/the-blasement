package dev.brella.blasement.common.events

import dev.brella.kornea.blaseball.BlaseballFeedEventType
import dev.brella.kornea.blaseball.PlayerID
import dev.brella.kornea.blaseball.TeamID
import dev.brella.kornea.blaseball.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.beans.BlaseballFeedEvent
import kotlinx.serialization.Serializable

@Serializable
data class BlaseballFeedEventWithContext(val event: BlaseballFeedEvent, val gameStep: BlaseballDatabaseGame?)

data class BlaseballFloodingEvent(val event: BlaseballFeedEvent.Flooding, val gameStep: BlaseballDatabaseGame, val playersFlooded: List<PlayerID>)
data class BlaseballGameEndEvent(val event: BlaseballFeedEvent.GameEndLog, val gameStep: BlaseballDatabaseGame, val winner: TeamID)
data class BlaseballShutoutEvent(val event: BlaseballFeedEvent.GameEndLog, val gameStep: BlaseballDatabaseGame, val pitcher: PlayerID, val pitcherName: String)
data class BlaseballBlackHoleEvent(val event: BlaseballFeedEvent.BlackHoleInGame, val gameStep: BlaseballDatabaseGame)
data class BlaseballIncinerationEvent(val event: BlaseballFeedEvent.Incineration, val gameStep: BlaseballDatabaseGame)
data class BlaseballStrikeoutEvent(val event: BlaseballFeedEvent.Strikeout, val gameStep: BlaseballDatabaseGame)
data class BlaseballTeamShamedEvent(val event: BlaseballFeedEvent.TeamShamed, val team: TeamID)
data class BlaseballTeamShamesEvent(val event: BlaseballFeedEvent.TeamShames, val team: TeamID)
data class BlaseballHomeRunEvent(val event: BlaseballFeedEvent.HomeRun, val gameStep: BlaseballDatabaseGame)
data class BlaseballHitEvent(val event: BlaseballFeedEvent.Hit, val gameStep: BlaseballDatabaseGame, val pitcher: PlayerID, val pitcherName: String, val batter: PlayerID, val batterName: String)
data class BlaseballStolenBaseEvent(val event: BlaseballFeedEvent.StolenBase, val gameStep: BlaseballDatabaseGame)
data class BlaseballSun2Event(val event: BlaseballFeedEvent.Sun2InGame, val gameStep: BlaseballDatabaseGame)

fun BlaseballFeedEventWithContext.asFlooding(): BlaseballFloodingEvent? =
    if (gameStep != null && event is BlaseballFeedEvent.Flooding)
        BlaseballFloodingEvent(event, gameStep, gameStep.baseRunners.filterNot(event.playerTags::contains))
    else null