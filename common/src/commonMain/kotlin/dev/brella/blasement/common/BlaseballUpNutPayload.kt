package dev.brella.blasement.common

import dev.brella.kornea.blaseball.base.common.FeedID
import kotlinx.serialization.Serializable

@Serializable
data class BlaseballUpNutPayload(val eventId: FeedID)
