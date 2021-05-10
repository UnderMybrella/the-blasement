package dev.brella.blasement.common.events

import com.soywiz.klock.DateTimeTz
import dev.brella.blasement.common.BlaseballFanFrontendPayload
import dev.brella.blasement.common.BlaseballFanSnacksPayload
import dev.brella.kornea.blaseball.base.common.BlaseballRewardShopSnack
import dev.brella.kornea.blaseball.base.common.BlaseballRewardShopSnack.Companion.SLOT_MULTIPLIERS
import dev.brella.kornea.blaseball.base.common.BlaseballUUID
import dev.brella.kornea.blaseball.base.common.EnumBlaseballSnack
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.UUID
import dev.brella.kornea.blaseball.base.common.UUIDMap
import dev.brella.kornea.blaseball.base.common.json.BlaseballDateTimeSerialiser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.roundToInt

@Serializable
data class BlaseballBet(val team: TeamID, val bet: Int)

@Serializable
enum class EnumChangeIdolFail {
    NOT_ENOUGH_COINS,
    ALREADY_IDOL,
    GAME_IN_PROGRESS
}

@Serializable
enum class EnumChangeTeamFail {
    NO_FLUTE,
    ALREADY_FAVOURITE_TEAM,
    GAME_IN_PROGRESS
}

@Serializable
enum class EnumBegFail {
    TOO_MANY_COINS,
    NO_BREAD_CRUMBS
}

@Serializable
enum class EnumUnlockFail {
    NOT_ENOUGH_COINS,
    ALREADY_UNLOCKED
}

@Serializable
enum class EnumPurchaseItemFail {
    NOT_ENOUGH_COINS,
    MEMBERSHIP_LOCKED,
    TIER_TOO_HIGH,
    INVENTORY_FULL,
    ITEM_COUNT_FULL
}

@Serializable
enum class EnumSellItemFail {
    MEMBERSHIP_LOCKED,
    ITEM_NOT_IN_INVENTORY,
    NOT_ENOUGH_ITEMS,
}

@Serializable
enum class EnumPurchaseSlotFail {
    MEMBERSHIP_LOCKED,
    NOT_ENOUGH_COINS,
    TOO_MANY_SLOTS,
    INVALID_AMOUNT
}

@Serializable
enum class EnumSellSlotFail {
    MEMBERSHIP_LOCKED,
    NO_EMPTY_SLOTS,
    NOT_ENOUGH_SLOTS,
    INVALID_AMOUNT
}

@Serializable
enum class EnumBetFail {
    NOT_ENOUGH_COINS,
    BET_TOO_HIGH,
    CANT_BET_ZERO,
    INVALID_TEAM,
    NO_SNAKE_OIL
}

object ItemSerializer : KSerializer<EnumBlaseballSnack> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): EnumBlaseballSnack =
        EnumBlaseballSnack.valueOf(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: EnumBlaseballSnack) {
        encoder.encodeString(value.name)
    }
}

sealed class BlaseballInventorySlot : Map.Entry<EnumBlaseballSnack, Int> {
    abstract val item: EnumBlaseballSnack
    abstract val count: Int

    override val key: EnumBlaseballSnack get() = item
    override val value: Int get() = count

    @Serializable
    data class Immutable(override val item: @Serializable(ItemSerializer::class) EnumBlaseballSnack, override val count: Int) : BlaseballInventorySlot()

    @Serializable
    data class Mutable(override val item: @Serializable(ItemSerializer::class) EnumBlaseballSnack, override var count: Int) : BlaseballInventorySlot(), MutableMap.MutableEntry<EnumBlaseballSnack, Int> {
        override fun setValue(newValue: Int): Int {
            val prev = count
            count = newValue
            return prev
        }

        inline fun toImmutable() =
            Immutable(item, count)
    }
}

typealias BlasementInventory = Map<@Serializable(ItemSerializer::class) EnumBlaseballSnack, Int>
typealias BlasementMutableInventory = MutableMap<@Serializable(ItemSerializer::class) EnumBlaseballSnack, Int>

@Serializable
inline class FanID(override val uuid: UUID) : BlaseballUUID

interface BlasementFan {
    val id: FanID
    val email: String?
    val appleId: String?
    val googleId: String?
    val discordId: String?

    val name: String?
    val password: String?

    val coins: Long

    val lastActive: DateTimeTz
    val created: DateTimeTz

    val loginStreak: Int

    val idol: PlayerID?
    val favouriteTeam: TeamID?

    val hasUnlockedShop: Boolean
    val hasUnlockedElections: Boolean

    val inventory: BlasementInventory
    val inventorySpace: Int
    val currentBets: Map<GameID, BlaseballBet>

    val peanutsEaten: Int
    val squirrels: Int

    val lightMode: Boolean
    val spread: List<Int>

    val coffee: Int?
    val favNumber: Int?

    val trackers: BlaseballFanTrackers

    val payoutRate: Double
        get() = SLOT_MULTIPLIERS[inventorySpace - 1]

    val nextPayoutRate: Double
        get() = SLOT_MULTIPLIERS[inventorySpace]

    suspend fun beg(): Pair<Int?, EnumBegFail?>
    suspend fun purchaseShopMembershipCard(): Pair<Int?, EnumUnlockFail?>

    suspend fun buySnack(amount: Int, item: EnumBlaseballSnack): Pair<Int?, EnumPurchaseItemFail?>
    suspend fun sell(amount: Int, item: EnumBlaseballSnack): Pair<Int?, EnumSellItemFail?>

    suspend fun purchaseSlot(amount: Int = 1): Pair<Int?, EnumPurchaseSlotFail?>
    suspend fun sellSlot(amount: Int = 1): Pair<Int?, EnumSellSlotFail?>

    suspend fun placeBet(onGame: GameID, onTeam: TeamID, amount: Int): EnumBetFail?
}

inline fun BlaseballInventorySlot.sellsFor(amount: Int): Int = item.sellsFor(count, amount)
inline fun EnumBlaseballSnack.sellsFor(amountInInventory: Int, amountToSell: Int): Int =
    when (this) {
        EnumBlaseballSnack.VOTES -> (100 * amountToSell) / 4
        EnumBlaseballSnack.PEANUTS -> amountToSell / 4
        EnumBlaseballSnack.FLUTES -> ((2e3 * amountToSell) / 4).roundToInt()
        EnumBlaseballSnack.PIZZA, EnumBlaseballSnack.CHEESE_BOARD, EnumBlaseballSnack.APPLE, EnumBlaseballSnack.BREAD_CRUMBS, EnumBlaseballSnack.TAROT_SPREAD -> 0
        else -> tiers?.let { it.slice((amountInInventory - amountToSell) until amountInInventory).sumBy(BlaseballRewardShopSnack::price) / 4 } ?: 0
    }

//inline operator fun Int?.plus(other: Int?): Int? = if (this == null || other == null) null else this + other

inline fun BlasementFan.toPayload() =
    BlasementFanPayload(
        id,
        email,
        appleId,
        googleId,
        discordId,
        name,
        password,
        coins,
        lastActive,
        created,
        loginStreak,
        idol,
        favouriteTeam,
        hasUnlockedShop,
        hasUnlockedElections,
        peanutsEaten,
        squirrels,
        lightMode,
        spread,
        coffee,
        favNumber,
        inventory,
        inventorySpace,
        UUIDMap(currentBets),
        trackers
    )

inline fun BlasementFan.toFrontendPayload() =
    BlaseballFanFrontendPayload(
        id,
        email ?: appleId?.plus("@apple.com") ?: googleId?.plus("@google.com") ?: discordId?.plus("@discord.com"),
        appleId,
        googleId,
        discordId,
        name,
        password,
        coins,
        lastActive,
        created,
        loginStreak,
        favouriteTeam,
        hasUnlockedShop,
        hasUnlockedElections,
        peanutsEaten,
        squirrels,
        idol,
        BlaseballFanSnacksPayload(
            votes = inventory[EnumBlaseballSnack.VOTES],
            flutes = inventory[EnumBlaseballSnack.FLUTES],
            snakeOil = inventory[EnumBlaseballSnack.SNAKE_OIL]?.minus(1),
            popcorn = inventory[EnumBlaseballSnack.POPCORN]?.minus(1),
            stalePopcorn = inventory[EnumBlaseballSnack.STALE_POPCORN]?.minus(1),
            breakfast = inventory[EnumBlaseballSnack.BREAKFAST]?.minus(1),
            taffy = inventory[EnumBlaseballSnack.TAFFY]?.minus(1),
            lemonade = inventory[EnumBlaseballSnack.LEMONADE]?.minus(1),
            chips = inventory[EnumBlaseballSnack.CHIPS]?.minus(1),
            burger = inventory[EnumBlaseballSnack.BURGER]?.minus(1),
            meatball = inventory[EnumBlaseballSnack.MEATBALL]?.minus(1),
            hotDog = inventory[EnumBlaseballSnack.HOT_DOG]?.minus(1),
            sunflowerSeeds = inventory[EnumBlaseballSnack.SUNFLOWER_SEEDS]?.minus(1),
            pickles = inventory[EnumBlaseballSnack.PICKLES]?.minus(1),
            slushie = inventory[EnumBlaseballSnack.SLUSHIE]?.minus(1),
            sundae = inventory[EnumBlaseballSnack.SUNDAE]?.minus(1),
            wetPretzel = inventory[EnumBlaseballSnack.WET_PRETZEL]?.minus(1),
            doughnut = inventory[EnumBlaseballSnack.DOUGHNUT]?.minus(1),
            pizza = inventory[EnumBlaseballSnack.PIZZA],
            cheeseBoard = inventory[EnumBlaseballSnack.CHEESE_BOARD],
            apple = inventory[EnumBlaseballSnack.APPLE],
            peanuts = inventory[EnumBlaseballSnack.PEANUTS],
            tarotSpread = inventory[EnumBlaseballSnack.TAROT_SPREAD],
            breadCrumbs = inventory[EnumBlaseballSnack.BREAD_CRUMBS]
        ),
        lightMode,
        inventorySpace,
        spread,
        coffee,
        favNumber,
        inventory.keys.toCollection(ArrayList<EnumBlaseballSnack?>()).let { order ->
            if (order.size < inventorySpace) {
                order.apply { repeat(inventorySpace - order.size) { add(null) } }
            } else {
                order
            }
        },
        trackers
    )

@Serializable
data class BlasementFanPayload(
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

    val idol: PlayerID? = null,
    val favouriteTeam: TeamID?,

    val hasUnlockedShop: Boolean = false,
    val hasUnlockedElections: Boolean = false,

    val peanutsEaten: Int,
    val squirrels: Int,
    val lightMode: Boolean,
    val spread: List<Int>,
    val coffee: Int?,
    val favNumber: Int?,

    val inventory: BlasementInventory = emptyMap(),
    val inventorySpace: Int = 8,
    val currentBets: UUIDMap<GameID, BlaseballBet> = UUIDMap(HashMap()),

    val trackers: BlaseballFanTrackers
)

data class BlasementFanDatabasePayload(
    val fanID: FanID,
    val email: String?,
    val appleId: String?,
    val googleId: String?,
    val facebookId: String?,
    val name: String?,
    val password: String?,
    val coins: Long,
    val lastActive: @Serializable(with = BlaseballDateTimeSerialiser::class) DateTimeTz,
    val created: @Serializable(with = BlaseballDateTimeSerialiser::class) DateTimeTz,
    val loginStreak: Int,
    val idol: PlayerID?,
    val favouriteTeam: TeamID?,
    val hasUnlockedShop: Boolean,
    val hasUnlockedElections: Boolean,
    val peanutsEaten: Int,
    val squirrels: Int,
    val lightMode: Boolean,
    val inventorySpace: Int,
    val spread: List<Int>,
    val coffee: Int?,
    val favNumber: Int?,
    val readOnly: Boolean = false,
    val verified: Boolean = false,
    val activeLeagueType: String? = null,
    val activeLeagueID: String? = null
)

@Serializable
data class BlaseballFanTrackers(
    @SerialName("BEGS")
    val begs: Int = 0,
    @SerialName("BETS")
    val bets: Int = 0,
    @SerialName("VOTES_CAST")
    val votesCast: Int = 0,
    @SerialName("SNACKS_BOUGHT")
    val snacksBought: Int = 0,
    @SerialName("SNACK_UPGRADES")
    val snackUpgrades: Int = 0
)