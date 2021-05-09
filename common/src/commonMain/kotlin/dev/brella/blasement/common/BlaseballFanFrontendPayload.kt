package dev.brella.blasement.common

import com.soywiz.klock.DateTimeTz
import dev.brella.blasement.common.events.BlaseballFanTrackers
import dev.brella.blasement.common.events.FanID
import dev.brella.kornea.blaseball.base.common.EnumBlaseballSnack
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.json.BlaseballDateTimeSerialiser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

fun createFanPayload(id: FanID, email: String, favouriteTeam: TeamID) =
    BlaseballFanFrontendPayload(
        id,
        email,
        null,
        null,
        null,
        null,
        null,
        1000,
        DateTimeTz.nowLocal(),
        DateTimeTz.nowLocal(),
        0,
        favouriteTeam,
        true,
        true,
        0,
        0,
        null,
        BlaseballFanSnacksPayload(
            snakeOil = 1,
            peanuts = 1000
        ),
        false,
        3,
        emptyList(),
        0,
        0,
        listOf(EnumBlaseballSnack.SNAKE_OIL, EnumBlaseballSnack.PEANUTS),
        BlaseballFanTrackers(
            begs = 0,
            bets = 0,
            votesCast = 0,
            snacksBought = 2,
            snackUpgrades = 0
        )
    )

@Serializable
data class BlaseballFanFrontendPayload(
    val id: FanID,
    val email: String?,
    val appleId: String?,
    val googleId: String?,
    val facebookId: String?,
    val name: String?,
    val password: String?,
    val coins: Long,
    val lastActive: @Serializable(BlaseballDateTimeSerialiser::class) DateTimeTz,
    val created: @Serializable(BlaseballDateTimeSerialiser::class) DateTimeTz,
    val loginStreak: Int,
    val favoriteTeam: TeamID?,
    val unlockedShop: Boolean,
    val unlockedElection: Boolean,
    val peanutsEaten: Int,
    val squirrels: Int,
    val idol: PlayerID?,
    val snacks: BlaseballFanSnacksPayload,
    val lightMode: Boolean,
    val packSize: Int,
    val spread: List<Int>,
    val coffee: Int?,
    val favNumber: Int?,
    val snackOrder: @Serializable(SnackListSerialiser::class) List<EnumBlaseballSnack?>,
    val trackers: BlaseballFanTrackers
)

object SnackListSerialiser : KSerializer<List<EnumBlaseballSnack?>> {
    override val descriptor: SerialDescriptor = listSerialDescriptor<EnumBlaseballSnack>()
    private val internalSerialiser = ListSerializer(String.serializer())

    override fun deserialize(decoder: Decoder): List<EnumBlaseballSnack?> =
        internalSerialiser.deserialize(decoder).map(EnumBlaseballSnack::fromID)

    override fun serialize(encoder: Encoder, value: List<EnumBlaseballSnack?>) =
        internalSerialiser.serialize(encoder, value.map { it?.snackId ?: "E" })

}

@Serializable
data class BlaseballFanSnacksPayload(
    @SerialName(EnumBlaseballSnack.IDs.SUNFLOWER_SEEDS)
    val sunflowerSeeds: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.HOT_DOG)
    val hotDog: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.CHIPS)
    val chips: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.BURGER)
    val burger: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.VOTES)
    val votes: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.SNAKE_OIL)
    val snakeOil: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.POPCORN)
    val popcorn: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.STALE_POPCORN)
    val stalePopcorn: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.TAROT_SPREAD)
    val tarotSpread: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.PIZZA)
    val pizza: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.CHEESEBOARD)
    val cheeseBoard: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.APPLE)
    val apple: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.FLUTES)
    val flutes: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.PEANUTS)
    val peanuts: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.BREAD_CRUMBS)
    val breadCrumbs: Int? = null,
    @SerialName("Tarot_Reroll")
    val Tarot_Reroll: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.SLUSHIE)
    val slushie: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.WET_PRETZEL)
    val wetPretzel: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.SUNDAE)
    val sundae: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.PICKLES)
    val pickles: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.HOT_FRIES)
    val hotFries: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.MEATBALL)
    val meatball: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.TAFFY)
    val taffy: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.LEMONADE)
    val lemonade: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.DOUGHNUT)
    val doughnut: Int? = null,
    @SerialName(EnumBlaseballSnack.IDs.BREAKFAST)
    val breakfast: Int? = null,
//    @SerialName(BlaseballSnacks.SLOT)
//    val slot: Int? = null
)
