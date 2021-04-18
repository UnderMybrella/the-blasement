import dev.brella.blasement.common.events.*
import dev.brella.kornea.blaseball.BlaseballRewardShopItem
import dev.brella.kornea.blaseball.EnumBlaseballItem
import dev.brella.kornea.blaseball.GameID
import dev.brella.kornea.blaseball.PlayerID
import dev.brella.kornea.blaseball.TeamID
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class BlasementHostBetter(
    override val name: String,
    override var coins: Long,
    override var idol: PlayerID? = null,
    override var favouriteTeam: TeamID,

    override var hasUnlockedShop: Boolean = false,
    override var hasUnlockedElections: Boolean = false,

    override val inventory: BlasementInventory = BlasementInventory(arrayOfNulls(8)),
    override val currentBets: MutableMap<GameID, BlaseballBet> = HashMap()
): BlasementBetter {
    override suspend fun beg(): Pair<Int?, EnumBegFail?> {
        if (coins > 0) return Pair(null, EnumBegFail.TOO_MANY_COINS)
        if (EnumBlaseballItem.BREAD_CRUMBS !in inventory) return Pair(null, EnumBegFail.NO_BREAD_CRUMBS)

        val begCoins = Random.nextInt(5, 20)
        coins += begCoins

        return Pair(begCoins, null)
    }

    override suspend fun purchaseMembershipCard(): EnumUnlockFail? {
        if (hasUnlockedShop) return EnumUnlockFail.ALREADY_UNLOCKED
        if (coins < 20) return EnumUnlockFail.NOT_ENOUGH_COINS

        coins -= 20
        hasUnlockedShop = true
        return null
    }

    override suspend fun purchase(amount: Int, item: EnumBlaseballItem): EnumPurchaseItemFail? {
        if (!hasUnlockedShop) return EnumPurchaseItemFail.MEMBERSHIP_LOCKED
        val count = inventory[item]

        if (count == null && inventory.isFull) return EnumPurchaseItemFail.INVENTORY_FULL

        if (item.price == null) {
            //Tiered Item
            val tiers = item.tiers!!.drop(count ?: 0).take(amount)
            if (tiers.isEmpty()) return EnumPurchaseItemFail.TIER_TOO_HIGH

            val price = tiers.sumBy(BlaseballRewardShopItem::price)

            if (price > coins) return EnumPurchaseItemFail.NOT_ENOUGH_COINS
            coins -= price
            inventory[item] = count?.plus(tiers.size) ?: tiers.size

            return null
        } else {
            //Single Purchase
            if (count != null && count >= item.inventoryLimit!!) return EnumPurchaseItemFail.ITEM_COUNT_FULL
            var purchasing = 0
            var newCount = count ?: 0
            val purchaseQuantity = item.purchaseQuantity!!
            val limit = item.inventoryLimit!!

            while (newCount + purchaseQuantity <= limit && purchasing < amount) {
                purchasing++
                newCount += purchaseQuantity
            }
            if (purchasing <= 0) return EnumPurchaseItemFail.ITEM_COUNT_FULL

            val price = purchasing * item.price!!
            if (price > coins) return EnumPurchaseItemFail.NOT_ENOUGH_COINS

            coins -= price
            inventory[item] = newCount

            return null
        }
    }
    override suspend fun sell(amount: Int, item: EnumBlaseballItem): EnumSellItemFail? {
        if (!hasUnlockedShop) return EnumSellItemFail.MEMBERSHIP_LOCKED
        val count = inventory[item] ?: return EnumSellItemFail.ITEM_NOT_IN_INVENTORY
        if (amount > count) return EnumSellItemFail.NOT_ENOUGH_ITEMS
        val amountBack = item.sellsFor(count, amount)

        coins += amountBack
        inventory[item] = count - amount

        return null
    }

    override suspend fun purchaseSlot(amount: Int): EnumPurchaseSlotFail? {
        if (!hasUnlockedShop) return EnumPurchaseSlotFail.MEMBERSHIP_LOCKED

        val availableSlotsToBuy = BlaseballRewardShopItem.SLOT_MULTIPLIERS.size - inventory.size
        if (availableSlotsToBuy < amount) return EnumPurchaseSlotFail.TOO_MANY_SLOTS
        if (amount * BlaseballRewardShopItem.SLOT_PRICE < coins) return EnumPurchaseSlotFail.NOT_ENOUGH_COINS

        inventory.boughtSlots(amount)

        coins -= amount * BlaseballRewardShopItem.SLOT_PRICE

        return null
    }
    override suspend fun sellSlot(amount: Int): EnumSellSlotFail? {
        if (!hasUnlockedShop) return EnumSellSlotFail.MEMBERSHIP_LOCKED

        if (inventory.size > amount) return EnumSellSlotFail.NOT_ENOUGH_SLOTS
        val availableSlotsToSell = inventory.count { slot: BlaseballInventorySlot? -> slot == null }
        if (availableSlotsToSell < amount) return EnumSellSlotFail.NO_EMPTY_SLOTS

        inventory.boughtSlots(amount)

        coins += amount * BlaseballRewardShopItem.SLOT_PRICE

        return null
    }

    override suspend fun placeBet(onGame: GameID, onTeam: TeamID, amount: Int): EnumBetFail? {
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