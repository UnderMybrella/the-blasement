package dev.brella.blasement.common.events

import dev.brella.kornea.blaseball.base.common.BlaseballRewardShopItem
import dev.brella.kornea.blaseball.base.common.BlaseballRewardShopItem.Companion.SLOT_MULTIPLIERS
import dev.brella.kornea.blaseball.base.common.BlaseballUUID
import dev.brella.kornea.blaseball.base.common.EnumBlaseballItem
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.UUIDMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.roundToInt
import kotlin.reflect.KProperty

@Serializable
data class BlaseballBet(val team: TeamID, val bet: Int)

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
    INVALID_TEAM
}

object ItemSerializer : KSerializer<EnumBlaseballItem> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): EnumBlaseballItem =
        EnumBlaseballItem.valueOf(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: EnumBlaseballItem) {
        encoder.encodeString(value.name)
    }
}

sealed class BlaseballInventorySlot : Map.Entry<EnumBlaseballItem, Int> {
    abstract val item: EnumBlaseballItem
    abstract val count: Int

    override val key: EnumBlaseballItem get() = item
    override val value: Int get() = count

    @Serializable
    data class Immutable(override val item: @Serializable(ItemSerializer::class) EnumBlaseballItem, override val count: Int) : BlaseballInventorySlot()

    @Serializable
    data class Mutable(override val item: @Serializable(ItemSerializer::class) EnumBlaseballItem, override var count: Int) : BlaseballInventorySlot(), MutableMap.MutableEntry<EnumBlaseballItem, Int> {
        override fun setValue(newValue: Int): Int {
            val prev = count
            count = newValue
            return prev
        }

        inline fun toImmutable() =
            Immutable(item, count)
    }
}

//@Serializable
//sealed class BlasementOldInventory<T: BlaseballInventorySlot> : List<T?>, Map<EnumBlaseballItem, Int> {
//    abstract var backing: Array<T?>
//    override val size: Int get() = backing.size
//
//    val isFull: Boolean
//        get() = null !in backing
//
//    inline fun <reified T> Array<out T?>.filterNotNullArray(): Array<T> {
//        val array = arrayOfNulls<T>(count { it != null })
//        var index = 0
//
//        forEach { if (it != null) array[index++] = it }
//
//        return array as Array<T>
//    }
//
//    @Serializable
//    class Immutable(override var backing: Array<BlaseballInventorySlot.Immutable?>) : BlasementOldInventory<BlaseballInventorySlot.Immutable>()
//
//    class Mutable(override var backing: Array<BlaseballInventorySlot.Mutable?>) : BlasementOldInventory<BlaseballInventorySlot.Mutable>(), MutableMap<EnumBlaseballItem, Int> {
//        override val entries: MutableSet<MutableMap.MutableEntry<EnumBlaseballItem, Int>>
//            get() = backing.filterNotNull().toMutableSet()
//        override val keys: MutableSet<EnumBlaseballItem>
//            get() = backing.filterNotNull().mapTo(HashSet(), BlaseballInventorySlot::item)
//        override val values: MutableCollection<Int>
//            get() = backing.filterNotNull().mapTo(HashSet(), BlaseballInventorySlot::count)
//
//        operator fun getValue(thisRef: Any?, property: KProperty<*>): BlasementOldInventory.Immutable =
//            Immutable(Array(backing.size) { backing[it]?.toImmutable() })
//
//        fun boughtSlots(count: Int = 1): EnumPurchaseSlotFail? {
//            if (count <= 0) return EnumPurchaseSlotFail.INVALID_AMOUNT
//
//            val current = backing
//            backing = arrayOfNulls(current.size + count)
//            current.copyInto(backing)
//
//            return null
//        }
//
//        fun soldSlots(count: Int = 1): EnumSellSlotFail? {
//            if (count <= 0) return EnumSellSlotFail.INVALID_AMOUNT
//
//            val current = backing.filterNotNullArray()
//            if (current.size >= backing.size - count) return EnumSellSlotFail.NO_EMPTY_SLOTS
//            backing = arrayOfNulls(current.size - count)
//            current.copyInto(backing)
//
//            return null
//        }
//
//        public operator fun set(index: Int, element: BlaseballInventorySlot.Mutable?): BlaseballInventorySlot.Mutable? {
//            val prev = backing[index]
//            backing[index] = element?.takeUnless { it.count <= 0 }
//            return prev
//        }
//
//        override fun clear() = throw UnsupportedOperationException("Blaseball inventory cannot be cleared")
//
//        override fun put(key: EnumBlaseballItem, value: Int): Int? {
//            var index = backing.indexOfFirst { slot -> slot?.item == key }
//            if (index == -1) {
//                //Can we add to this array at the moment?
//                index = backing.indexOf(null)
//
//                //Whoops, can't add to the inventory!
//                if (index == -1) throw UnsupportedOperationException("Blaseball inventory is full!")
//            }
//
//            val element = backing[index]
//            if (element == null) {
//                if (value > 0) backing[index] = BlaseballInventorySlot.Mutable(key, value)
//                return null
//            }
//
//            val prev = element.count
//            if (value > 0)
//                element.count = value
//            else
//                backing[index] = null
//            return prev
//        }
//
//        override fun putAll(from: Map<out EnumBlaseballItem, Int>) =
//            from.forEach { (item, count) -> put(item, count) }
//
//        override fun remove(key: EnumBlaseballItem): Int? {
//            val index = backing.indexOfFirst { slot -> slot?.item == key }
//            if (index == -1) return null
//            val element = backing[index]
//            backing[index] = null
//            return element?.count
//        }
//    }
//
//    override fun contains(element: T?): Boolean =
//        backing.any { slot -> slot?.item == element?.item }
//
//    operator fun contains(item: EnumBlaseballItem?): Boolean =
//        backing.any { slot -> slot?.item == item }
//
//    override fun containsAll(elements: Collection<T?>): Boolean =
//        elements.toMutableList().also { list ->
//            backing.forEach { slot -> list.removeAll { it?.item == slot?.item } }
//        }.isNotEmpty()
//
//    override fun get(index: Int): T? =
//        backing[index]
//
//    override fun indexOf(element: T?): Int =
//        backing.indexOfFirst { slot -> slot?.item == element?.item }
//
//    override fun isEmpty(): Boolean = backing.isEmpty() || backing.all { it == null }
//
//    override fun iterator(): Iterator<T?> = backing.iterator()
//    override fun listIterator(): ListIterator<T?> = backing.asList().listIterator()
//    override fun listIterator(index: Int): ListIterator<T?> = backing.asList().listIterator(index)
//    override fun subList(fromIndex: Int, toIndex: Int): List<T?> = backing.asList().subList(fromIndex, toIndex)
//
//    override fun lastIndexOf(element: T?): Int = backing.indexOfLast { slot -> slot?.item == element?.item }
//
//    override val entries: Set<Map.Entry<EnumBlaseballItem, Int>>
//        get() = backing.filterNotNull().toSet()
//
//    override fun containsKey(key: EnumBlaseballItem): Boolean = backing.any { slot -> slot?.item == key }
//
//    override fun containsValue(value: Int): Boolean = backing.any { slot -> slot?.count == value }
//
//    override fun get(key: EnumBlaseballItem): Int? = backing.firstOrNull { slot -> slot?.item == key }?.count
//
//    override val keys: Set<EnumBlaseballItem>
//        get() = backing.filterNotNull().mapTo(HashSet(), BlaseballInventorySlot::item)
//    override val values: Collection<Int>
//        get() = backing.filterNotNull().mapTo(HashSet(), BlaseballInventorySlot::count)
//}

typealias BlasementInventory = Map<@Serializable(ItemSerializer::class) EnumBlaseballItem, Int>
typealias BlasementMutableInventory = MutableMap<@Serializable(ItemSerializer::class) EnumBlaseballItem, Int>

@Serializable
inline class FanID(override val id: String) : BlaseballUUID

interface BlasementFan {
    val id: FanID

    val name: String
    val coins: Long

    val idol: PlayerID?
    val favouriteTeam: TeamID

    val hasUnlockedShop: Boolean
    val hasUnlockedElections: Boolean

    val inventory: BlasementInventory
    val inventorySpace: Int
    val currentBets: Map<GameID, BlaseballBet>

    val payoutRate: Double
        get() = SLOT_MULTIPLIERS[inventorySpace - 1]

    val nextPayoutRate: Double
        get() = SLOT_MULTIPLIERS[inventorySpace]

    suspend fun beg(): Pair<Int?, EnumBegFail?>
    suspend fun purchaseShopMembershipCard(): Pair<Int?, EnumUnlockFail?>

    suspend fun purchase(amount: Int, item: EnumBlaseballItem): Pair<Int?, EnumPurchaseItemFail?>
    suspend fun sell(amount: Int, item: EnumBlaseballItem): Pair<Int?, EnumSellItemFail?>

    suspend fun purchaseSlot(amount: Int = 1): Pair<Int?, EnumPurchaseSlotFail?>
    suspend fun sellSlot(amount: Int = 1): Pair<Int?, EnumSellSlotFail?>

    suspend fun placeBet(onGame: GameID, onTeam: TeamID, amount: Int): EnumBetFail?
}

inline fun BlaseballInventorySlot.sellsFor(amount: Int): Int = item.sellsFor(count, amount)
inline fun EnumBlaseballItem.sellsFor(amountInInventory: Int, amountToSell: Int): Int =
    when (this) {
        EnumBlaseballItem.VOTES -> (100 * amountToSell) / 4
        EnumBlaseballItem.PEANUTS -> amountToSell / 4
        EnumBlaseballItem.FLUTES -> ((2e3 * amountToSell) / 4).roundToInt()
        EnumBlaseballItem.PIZZA, EnumBlaseballItem.CHEESE_BOARD, EnumBlaseballItem.APPLE, EnumBlaseballItem.BREAD_CRUMBS, EnumBlaseballItem.TAROT_SPREAD -> 0
        else -> tiers?.let { it.slice((amountInInventory - amountToSell) until amountInInventory).sumBy(BlaseballRewardShopItem::price) / 4 } ?: 0
    }

//inline operator fun Int?.plus(other: Int?): Int? = if (this == null || other == null) null else this + other

inline fun BlasementFan.toPayload() =
    BlasementFanPayload(id, name, coins, idol, favouriteTeam, hasUnlockedShop, hasUnlockedElections, inventory, inventorySpace, UUIDMap(currentBets))

@Serializable
data class BlasementFanPayload(
    val id: FanID,

    val name: String,
    val coins: Long,
    val idol: PlayerID? = null,
    val favouriteTeam: TeamID,

    val hasUnlockedShop: Boolean = false,
    val hasUnlockedElections: Boolean = false,

    val inventory: BlasementInventory = emptyMap(),
    val inventorySpace: Int = 8,
    val currentBets: UUIDMap<GameID, BlaseballBet> = UUIDMap(HashMap())
)

data class BlasementFanDatabasePayload(
    val fanID: FanID,
    val fanName: String,
    val coins: Long,
    val idol: PlayerID?,
    val favouriteTeam: TeamID,
    val hasUnlockedShop: Boolean,
    val hasUnlockedElections: Boolean,
    val inventorySpace: Int
)