package dev.brella.blasement.common

import dev.brella.kornea.blaseball.base.common.PlayerID
import kotlinx.serialization.Serializable

@Serializable
data class BlaseballChooseIdolPayload(
    val playerId: PlayerID,
    val playerName: String
)