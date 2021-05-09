package dev.brella.blasement.common

import dev.brella.kornea.blaseball.base.common.TeamID
import kotlinx.serialization.Serializable

@Serializable
data class BlaseballSetFavouriteTeamPayload(val teamId: TeamID)