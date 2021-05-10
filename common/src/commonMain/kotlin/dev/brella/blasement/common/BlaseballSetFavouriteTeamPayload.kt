package dev.brella.blasement.common

import dev.brella.kornea.blaseball.base.common.TeamID
import kotlinx.serialization.Serializable

@Serializable
data class BlaseballSetFavouriteTeamPayload(val teamId: TeamID)

@Serializable
data class BlaseballUpdateFavouriteTeamPayload(val newTeamId: TeamID, val teamName: String)