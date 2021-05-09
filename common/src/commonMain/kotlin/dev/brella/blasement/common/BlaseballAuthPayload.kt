package dev.brella.blasement.common

import kotlinx.serialization.Serializable

@Serializable
data class BlaseballAuthPayload(val password: String, val passwordConfirm: String? = null, val username: String, val isLogin: Boolean = false)
