package dev.brella.blasement.common

import kotlinx.serialization.Serializable

@Serializable
data class BlaseballUpdateProfilePayload(val coffee: Int, val favNumber: Int)
