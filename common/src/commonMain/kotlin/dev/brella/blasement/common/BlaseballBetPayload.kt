package dev.brella.blasement.common

import dev.brella.blasement.common.events.FanID
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.TeamID
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BlaseballBetPayload(
    val amount: Int,
    val targets: List<String>,
    val type: Int,
    val userId: FanID
) {
    val bettingTeam: TeamID
        get() = TeamID(targets[0])

    val bettingGame: GameID
        get() = GameID(targets[1])
}