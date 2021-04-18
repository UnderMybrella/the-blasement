package dev.brella.blasement.common.events

import dev.brella.kornea.blaseball.BlaseballRewardShopItem
import dev.brella.kornea.blaseball.BlaseballRewardShopItem.Companion.SLOT_MULTIPLIERS
import dev.brella.kornea.blaseball.BlaseballRewardShopItem.Companion.SLOT_PRICE
import dev.brella.kornea.blaseball.EnumBlaseballItem
import dev.brella.kornea.blaseball.GameID
import dev.brella.kornea.blaseball.PlayerID
import dev.brella.kornea.blaseball.TeamID
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt
import kotlin.random.Random

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
    CANT_BET_ZERO
}

@Serializable
data class BlaseballInventorySlot(val item: EnumBlaseballItem, var count: Int) : MutableMap.MutableEntry<EnumBlaseballItem, Int> {
    override val key: EnumBlaseballItem by this::item
    override val value: Int by this::count

    override fun setValue(newValue: Int): Int {
        val prev = count
        count = newValue
        return prev
    }
}

@Serializable
class BlasementInventory(var backing: Array<BlaseballInventorySlot?>) : List<BlaseballInventorySlot?>, MutableMap<EnumBlaseballItem, Int> {
    override val size: Int by backing::size

    val isFull: Boolean
        get() = null !in backing

    inline fun <reified T> Array<out T?>.filterNotNullArray(): Array<T> {
        val array = arrayOfNulls<T>(count { it != null })
        var index = 0

        forEach { if (it != null) array[index++] = it }

        return array as Array<T>
    }

    fun boughtSlots(count: Int = 1): EnumPurchaseSlotFail? {
        if (count <= 0) return EnumPurchaseSlotFail.INVALID_AMOUNT

        val current = backing
        backing = arrayOfNulls(current.size + count)
        current.copyInto(backing)

        return null
    }

    fun soldSlots(count: Int = 1): EnumSellSlotFail? {
        if (count <= 0) return EnumSellSlotFail.INVALID_AMOUNT

        val current = backing.filterNotNullArray()
        if (current.size >= backing.size - count) return EnumSellSlotFail.NO_EMPTY_SLOTS
        backing = arrayOfNulls(current.size - count)
        current.copyInto(backing)

        return null
    }

    override fun contains(element: BlaseballInventorySlot?): Boolean =
        backing.any { slot -> slot?.item == element?.item }

    operator fun contains(item: EnumBlaseballItem?): Boolean =
        backing.any { slot -> slot?.item == item }

    override fun containsAll(elements: Collection<BlaseballInventorySlot?>): Boolean =
        elements.toMutableList().also { list ->
            backing.forEach { slot -> list.removeAll { it?.item == slot?.item } }
        }.isNotEmpty()

    override fun get(index: Int): BlaseballInventorySlot? =
        backing[index]

    public operator fun set(index: Int, element: BlaseballInventorySlot?): BlaseballInventorySlot? {
        val prev = backing[index]
        backing[index] = element?.takeUnless { it.count <= 0 }
        return prev
    }

    override fun indexOf(element: BlaseballInventorySlot?): Int =
        backing.indexOfFirst { slot -> slot?.item == element?.item }

    override fun isEmpty(): Boolean = backing.isEmpty() || backing.all { it == null }

    override fun iterator(): Iterator<BlaseballInventorySlot?> = backing.iterator()
    override fun listIterator(): ListIterator<BlaseballInventorySlot?> = backing.asList().listIterator()
    override fun listIterator(index: Int): ListIterator<BlaseballInventorySlot?> = backing.asList().listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<BlaseballInventorySlot?> = backing.asList().subList(fromIndex, toIndex)

    override fun lastIndexOf(element: BlaseballInventorySlot?): Int = backing.indexOfLast { slot -> slot?.item == element?.item }

    override val entries: MutableSet<MutableMap.MutableEntry<EnumBlaseballItem, Int>>
        get() = backing.filterNotNull().toMutableSet()

    override fun containsKey(key: EnumBlaseballItem): Boolean = backing.any { slot -> slot?.item == key }

    override fun containsValue(value: Int): Boolean = backing.any { slot -> slot?.count == value }

    override fun get(key: EnumBlaseballItem): Int? = backing.firstOrNull { slot -> slot?.item == key }?.count

    override val keys: MutableSet<EnumBlaseballItem>
        get() = backing.filterNotNull().mapTo(HashSet(), BlaseballInventorySlot::item)
    override val values: MutableCollection<Int>
        get() = backing.filterNotNull().mapTo(HashSet(), BlaseballInventorySlot::count)

    override fun clear() = throw UnsupportedOperationException("Blaseball inventory cannot be cleared")

    override fun put(key: EnumBlaseballItem, value: Int): Int? {
        var index = backing.indexOfFirst { slot -> slot?.item == key }
        if (index == -1) {
            //Can we add to this array at the moment?
            index = backing.indexOf(null)

            //Whoops, can't add to the inventory!
            if (index == -1) throw UnsupportedOperationException("Blaseball inventory is full!")
        }

        val element = backing[index]
        if (element == null) {
            if (value > 0) backing[index] = BlaseballInventorySlot(key, value)
            return null
        }

        val prev = element.count
        if (value > 0)
            element.count = value
        else
            backing[index] = null
        return prev
    }

    override fun putAll(from: Map<out EnumBlaseballItem, Int>) =
        from.forEach { (item, count) -> put(item, count) }

    override fun remove(key: EnumBlaseballItem): Int? {
        val index = backing.indexOfFirst { slot -> slot?.item == key }
        if (index == -1) return null
        val element = backing[index]
        backing[index] = null
        return element?.count
    }
}

interface BlasementBetter {
    val name: String
    val coins: Long

    val idol: PlayerID?
    val favouriteTeam: TeamID

    val hasUnlockedShop: Boolean
    val hasUnlockedElections: Boolean

    val inventory: BlasementInventory
    val currentBets: Map<GameID, BlaseballBet>

    val payoutRate: Double
        get() = SLOT_MULTIPLIERS[inventory.size - 1]

    val nextPayoutRate: Double
        get() = SLOT_MULTIPLIERS[inventory.size]

    suspend fun beg(): Pair<Int?, EnumBegFail?>
    suspend fun purchaseMembershipCard(): EnumUnlockFail?

    suspend fun purchase(amount: Int, item: EnumBlaseballItem): EnumPurchaseItemFail?
    suspend fun sell(amount: Int, item: EnumBlaseballItem): EnumSellItemFail?

    suspend fun purchaseSlot(amount: Int = 1): EnumPurchaseSlotFail?
    suspend fun sellSlot(amount: Int = 1): EnumSellSlotFail?

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

inline operator fun Int?.plus(other: Int?): Int? = if (this == null || other == null) null else this + other