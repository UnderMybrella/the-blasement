import dev.brella.blasement.common.events.*
import dev.brella.kornea.blaseball.BlaseballRewardShopItem
import dev.brella.kornea.blaseball.BlaseballRewardShopItem.Companion.SLOT_PRICE
import dev.brella.kornea.blaseball.EnumBlaseballItem
import dev.brella.kornea.blaseball.GameID
import dev.brella.kornea.blaseball.PlayerID
import dev.brella.kornea.blaseball.TeamID
import dev.brella.kornea.toolkit.coroutines.ReadWriteSemaphore
import dev.brella.kornea.toolkit.coroutines.withReadPermit
import dev.brella.kornea.toolkit.coroutines.withWritePermit
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.collections.HashMap
import kotlin.random.Random

fun newBlaseballGal(id: BetterID = BetterID(UUID.randomUUID().toString()), name: String, favouriteTeam: TeamID): BlasementBetter =
    BlasementHostBetter(
        id,
        name,
        coins = 250,
        favouriteTeam = favouriteTeam
    ).apply {
        inventory[EnumBlaseballItem.VOTES] = 1
        inventory[EnumBlaseballItem.SNAKE_OIL] = 1
        inventory[EnumBlaseballItem.PEANUTS] = 10
    }

@Serializable
data class BlasementHostBetter(
    override val id: BetterID,

    override val name: String,
    override var coins: Long,
    override var idol: PlayerID? = null,
    override var favouriteTeam: TeamID,

    override var hasUnlockedShop: Boolean = false,
    override var hasUnlockedElections: Boolean = false,

    override val inventory: BlasementInventory = BlasementInventory(arrayOfNulls(8)),
    override val currentBets: MutableMap<GameID, BlaseballBet> = HashMap()
) : BlasementBetter {
    val semaphore = ReadWriteSemaphore(16)

    override suspend fun beg(): Pair<Int?, EnumBegFail?> = semaphore.withWritePermit {
        if (coins > 0) return Pair(null, EnumBegFail.TOO_MANY_COINS)
        if (EnumBlaseballItem.BREAD_CRUMBS !in inventory) return Pair(null, EnumBegFail.NO_BREAD_CRUMBS)

        val begCoins = Random.nextInt(5, 20)
        coins += begCoins

        return Pair(begCoins, null)
    }

    override suspend fun purchaseShopMembershipCard(): Pair<Int?, EnumUnlockFail?> = semaphore.withWritePermit {
        val price = 20

        if (hasUnlockedShop) return null to EnumUnlockFail.ALREADY_UNLOCKED
        if (coins < price) return null to EnumUnlockFail.NOT_ENOUGH_COINS

        coins -= price
        hasUnlockedShop = true

        return price to null
    }

    override suspend fun purchase(amount: Int, item: EnumBlaseballItem): Pair<Int?, EnumPurchaseItemFail?> = semaphore.withWritePermit {
        if (!hasUnlockedShop) return null to EnumPurchaseItemFail.MEMBERSHIP_LOCKED
        val count = inventory[item]

        if (count == null && inventory.isFull) return null to EnumPurchaseItemFail.INVENTORY_FULL

        if (item.price == null) {
            //Tiered Item
            val tiers = item.tiers!!.drop(count ?: 0).take(amount)
            if (tiers.isEmpty()) return null to EnumPurchaseItemFail.TIER_TOO_HIGH

            val price = tiers.sumBy(BlaseballRewardShopItem::price)

            if (coins < price) return null to EnumPurchaseItemFail.NOT_ENOUGH_COINS

            coins -= price
            inventory[item] = count?.plus(tiers.size) ?: tiers.size

            return price to null
        } else {
            //Single Purchase
            if (count != null && count >= item.inventoryLimit!!) return null to EnumPurchaseItemFail.ITEM_COUNT_FULL
            var purchasing = 0
            var newCount = count ?: 0
            val purchaseQuantity = item.purchaseQuantity!!
            val limit = item.inventoryLimit!!

            while (newCount + purchaseQuantity <= limit && purchasing < amount) {
                purchasing++
                newCount += purchaseQuantity
            }
            if (purchasing <= 0) return null to EnumPurchaseItemFail.ITEM_COUNT_FULL

            val price = purchasing * item.price!!
            if (coins < price) return null to EnumPurchaseItemFail.NOT_ENOUGH_COINS

            coins -= price
            inventory[item] = newCount

            return price to null
        }
    }

    override suspend fun sell(amount: Int, item: EnumBlaseballItem): Pair<Int?, EnumSellItemFail?> = semaphore.withWritePermit {
        if (!hasUnlockedShop) return null to EnumSellItemFail.MEMBERSHIP_LOCKED
        val count = inventory[item] ?: return null to EnumSellItemFail.ITEM_NOT_IN_INVENTORY
        if (amount > count) return null to EnumSellItemFail.NOT_ENOUGH_ITEMS
        val amountBack = item.sellsFor(count, amount)

        coins += amountBack
        inventory[item] = count - amount

        return amountBack to null
    }

    override suspend fun purchaseSlot(amount: Int): Pair<Int?, EnumPurchaseSlotFail?> = semaphore.withWritePermit {
        if (!hasUnlockedShop) return null to EnumPurchaseSlotFail.MEMBERSHIP_LOCKED

        val availableSlotsToBuy = BlaseballRewardShopItem.SLOT_MULTIPLIERS.size - inventory.size
        if (availableSlotsToBuy < amount) return null to EnumPurchaseSlotFail.TOO_MANY_SLOTS
        val price = amount * SLOT_PRICE
        if (coins < price) return null to EnumPurchaseSlotFail.NOT_ENOUGH_COINS

        inventory.boughtSlots(amount)
        coins -= price

        return price to null
    }

    override suspend fun sellSlot(amount: Int): Pair<Int?, EnumSellSlotFail?> = semaphore.withWritePermit {
        if (!hasUnlockedShop) return null to EnumSellSlotFail.MEMBERSHIP_LOCKED

        if (inventory.size > amount) return null to EnumSellSlotFail.NOT_ENOUGH_SLOTS
        val availableSlotsToSell = inventory.count { slot: BlaseballInventorySlot? -> slot == null }
        if (availableSlotsToSell < amount) return null to EnumSellSlotFail.NO_EMPTY_SLOTS

        val amountBack = amount * SLOT_PRICE

        inventory.soldSlots(amount)

        coins += amountBack

        return amountBack to null
    }

    override suspend fun placeBet(onGame: GameID, onTeam: TeamID, amount: Int): EnumBetFail? = semaphore.withWritePermit {
        if (amount > coins) return EnumBetFail.NOT_ENOUGH_COINS
        if (amount <= 0) return EnumBetFail.CANT_BET_ZERO

        val count = inventory[EnumBlaseballItem.SNAKE_OIL]?.takeIf { it > 0 } ?: 1
        val maxBet = EnumBlaseballItem.SNAKE_OIL[count - 1]!!.payout

        if (amount > maxBet) return EnumBetFail.BET_TOO_HIGH
        coins -= amount
        currentBets[onGame] = BlaseballBet(onTeam, amount)

        return null
    }
}