package dev.brella.blasement.common.events

import dev.brella.kornea.blaseball.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.beans.BlaseballFeedEvent
import kotlinx.serialization.Serializable

@Serializable
data class BlaseballFeedEventWithContext(val event: BlaseballFeedEvent, val gameStep: BlaseballDatabaseGame)