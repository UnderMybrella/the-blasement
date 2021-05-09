package dev.brella.blasement.data

import com.soywiz.klock.DateTimeTz
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.base.common.json.BlaseballDateTimeSerialiser
import dev.brella.kornea.blaseball.chronicler.ChroniclerBlaseballGame
import kotlinx.serialization.Serializable

@Serializable
class ChroniclerResponseWrapper<T>(val data: List<T>?, val nextPage: String? = null)

@Serializable
data class ChroniclerGameUpdateWrapper(val gameId: String, val timestamp: @Serializable(BlaseballDateTimeSerialiser::class) DateTimeTz, val data: BlaseballDatabaseGame)
