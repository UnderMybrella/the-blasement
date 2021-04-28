package dev.brella.blasement.common

import kotlinx.serialization.Serializable

@Serializable
data class BlaseballSellSnackPayload(val amount: Int, val snackId: String)