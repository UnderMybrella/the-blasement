import dev.brella.blasement.common.events.*
import dev.brella.kornea.blaseball.base.common.BlaseballRewardShopItem
import dev.brella.kornea.blaseball.base.common.BlaseballRewardShopItem.Companion.SLOT_PRICE
import dev.brella.kornea.blaseball.base.common.EnumBlaseballItem
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.toolkit.coroutines.ReadWriteSemaphore
import dev.brella.kornea.toolkit.coroutines.withWritePermit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.awaitSingleOrNull
import java.util.*
import kotlin.collections.HashMap
import kotlin.random.Random

fun BlasementFanPayload.toHost(blasement: TheBlasement): BlasementHostFan =
    BlasementHostFan(blasement, id, name, coins, idol, favouriteTeam, hasUnlockedShop, hasUnlockedElections, inventory, inventorySpace, currentBets)

class BlasementHostFan(
    val blasement: TheBlasement,
    override val id: FanID,

    override val name: String,
    coins: Long,
    idol: PlayerID? = null,
    favouriteTeam: TeamID,

    hasUnlockedShop: Boolean = false,
    hasUnlockedElections: Boolean = false,

    inventory: BlasementInventory = emptyMap(),
    inventorySpace: Int = 8,
    currentBets: Map<GameID, BlaseballBet> = emptyMap()
) : BlasementFan {
    constructor(payload: BlasementFanDatabasePayload, blasement: TheBlasement, items: List<Pair<EnumBlaseballItem, Int>?>, bets: List<Triple<String, String, Int>>) : this(
        blasement,
        payload.fanID,
        payload.fanName,
        payload.coins,
        payload.idol,
        payload.favouriteTeam,
        payload.hasUnlockedShop,
        payload.hasUnlockedElections,

        inventorySpace = payload.inventorySpace
    ) {
        items.forEach { pair ->
            if (pair != null) {
                _inventory[pair.first] = pair.second
            }
        }

        bets.forEach { (gameID, teamID, bet) ->
            _bets[GameID(gameID)] = BlaseballBet(TeamID(teamID), bet)
        }
    }

    val semaphore = ReadWriteSemaphore(16)
    val fanEvents: MutableSharedFlow<ServerEvent> = MutableSharedFlow()

    private var _coins = coins
    override val coins by ::_coins

    private var _idol = idol
    override val idol by ::_idol

    private var _favouriteTeam = favouriteTeam
    override val favouriteTeam by ::_favouriteTeam

    private var _shopUnlocked = hasUnlockedShop
    override val hasUnlockedShop by ::_shopUnlocked

    private var _electionsUnlocked = hasUnlockedElections
    override val hasUnlockedElections by ::_electionsUnlocked

    private val _inventory: BlasementMutableInventory = EnumMap(EnumBlaseballItem::class.java)
    override val inventory: Map<EnumBlaseballItem, Int> by ::_inventory

    private var _inventorySpace: Int = inventorySpace
    override val inventorySpace by ::_inventorySpace

    val inventoryFull: Boolean
        get() = _inventory.size == _inventorySpace

    private val _bets: MutableMap<GameID, BlaseballBet> = HashMap()
    override val currentBets: Map<GameID, BlaseballBet> by ::_bets

    private suspend inline fun coins(calculate: (coins: Long) -> Long) = setCoins(calculate(_coins))
    suspend fun setCoins(calculate: (coins: Long) -> Long) = semaphore.withWritePermit { setCoins(calculate(_coins)) }
    private suspend fun setCoins(newValue: Long) {
        blasement.connectionFactory
            .create()
            .use { connection ->
                connection.createStatement("UPDATE FANS SET COINS = $2 WHERE FAN_ID = $1;")
                    .bind("$1", id.id)
                    .bind("$2", newValue)
                    .execute()
                    .awaitFirst()

                _coins = newValue
            }
    }

    private suspend inline fun shopUnlocked(calculate: (hasUnlockedShop: Boolean) -> Boolean) = setShopUnlocked(calculate(_shopUnlocked))
    suspend fun setShopUnlocked(calculate: (hasUnlockedShop: Boolean) -> Boolean) = semaphore.withWritePermit { setShopUnlocked(calculate(_shopUnlocked)) }
    private suspend fun setShopUnlocked(newValue: Boolean) {
        blasement.connectionFactory
            .create()
            .use { connection ->
                connection.createStatement("UPDATE FANS SET HAS_UNLOCKED_SHOP = $2 WHERE FAN_ID = $1;")
                    .bind("$1", id.id)
                    .bind("$2", newValue)
                    .execute()
                    .awaitFirst()

                _shopUnlocked = newValue
            }
    }

    private suspend inline fun electionsUnlocked(calculate: (hasUnlockedElections: Boolean) -> Boolean) = setElectionsUnlocked(calculate(_electionsUnlocked))
    suspend fun setElectionsUnlocked(calculate: (hasUnlockedElections: Boolean) -> Boolean) = semaphore.withWritePermit { setElectionsUnlocked(calculate(_electionsUnlocked)) }
    private suspend fun setElectionsUnlocked(newValue: Boolean) {
        blasement.connectionFactory
            .create()
            .use { connection ->
                connection.createStatement("UPDATE FANS SET HAS_UNLOCKED_ELECTIONS = $2 WHERE FAN_ID = $1;")
                    .bind("$1", id.id)
                    .bind("$2", newValue)
                    .execute()
                    .awaitFirst()

                _shopUnlocked = hasUnlockedShop
            }
    }

    private suspend inline fun inventorySpace(calculate: (inventorySpace: Int) -> Int) = setInventorySpace(calculate(_inventorySpace))
    suspend fun setInventorySpace(calculate: (inventorySpace: Int) -> Int) = semaphore.withWritePermit { setInventorySpace(calculate(_inventorySpace)) }
    private suspend fun setInventorySpace(newValue: Int) {
        blasement.connectionFactory
            .create()
            .use { connection ->
                connection.createStatement("UPDATE FANS SET INVENTORY_SPACE = $2 WHERE FAN_ID = $1;")
                    .bind("$1", id.id)
                    .bind("$2", newValue)
                    .execute()
                    .awaitFirst()

                _shopUnlocked = hasUnlockedShop
            }
    }

    private suspend inline fun item(item: EnumBlaseballItem, calculate: (amount: Int?) -> Int) = setItemQuantity(item, calculate(inventory[item]))
    suspend fun setItemQuantity(item: EnumBlaseballItem, calculate: (amount: Int?) -> Int) = semaphore.withWritePermit { setItemQuantity(item, calculate(inventory[item])) }

    private suspend inline fun addItem(item: EnumBlaseballItem, calculate: (amount: Int?) -> Int) = setItemQuantity(item, inventory[item]?.let { it + calculate(it) } ?: calculate(null))
    suspend fun addItemQuantity(item: EnumBlaseballItem, calculate: (amount: Int?) -> Int) = semaphore.withWritePermit { setItemQuantity(item, inventory[item]?.let { it + calculate(it) } ?: calculate(null)) }

    private suspend inline fun removeItem(item: EnumBlaseballItem, calculate: (amount: Int?) -> Int) {
        return setItemQuantity(item, (inventory[item] ?: return).let { it + calculate(it) })
    }

    suspend fun removeItemQuantity(item: EnumBlaseballItem, calculate: (amount: Int?) -> Int) = semaphore.withWritePermit { setItemQuantity(item, (inventory[item] ?: return@withWritePermit).let { it + calculate(it) }) }

    private suspend fun setItemQuantity(item: EnumBlaseballItem, newValue: Int) {
        if (inventoryFull && item !in _inventory) {
            if (newValue <= 0) return

            throw IllegalArgumentException("No space in $name[${id.id}]'s inventory for ${newValue}x $item!")
        }
        blasement.connectionFactory
            .create()
            .use { connection ->
                val quantity = connection.createStatement("SELECT QUANTITY FROM ITEMS WHERE FAN_ID = $1 AND ITEM_NAME = $2")
                    .bind("$1", id.id)
                    .bind("$2", item.name)
                    .execute()
                    .awaitFirstOrNull()
                    ?.map { row, _ -> row["QUANTITY"] as Int }
                    ?.awaitSingleOrNull()

                when {
                    quantity == null -> {
                        connection.createStatement("INSERT INTO ITEMS (FAN_ID, ITEM_NAME, QUANTITY) VALUES ( $1, $2, $3 )")
                            .bind("$1", id.id)
                            .bind("$2", item.name)
                            .bind("$3", newValue)
                            .execute()
                            .awaitSingle()

                        _inventory[item] = newValue
                    }
                    newValue > 0 -> {
                        connection.createStatement("UPDATE ITEMS SET QUANTITY = $3 WHERE FAN_ID = $1 AND ITEM_NAME = $2;")
                            .bind("$1", id.id)
                            .bind("$2", item.name)
                            .bind("$3", newValue)
                            .execute()
                            .awaitFirst()

                        _inventory[item] = newValue
                    }
                    else -> {
                        connection.createStatement("DELETE FROM ITEMS WHERE FAN_ID = $1 AND ITEM_NAME = $2;")
                            .bind("$1", id.id)
                            .bind("$2", item.name)
                            .execute()
                            .awaitFirst()

                        _inventory.remove(item)
                    }
                }
            }
    }

    suspend fun gameCompleted(game: GameID): BlaseballBet? {
        val bet = _bets.remove(game) ?: return null

        blasement.connectionFactory
            .create()
            .use { connection ->
                connection.createStatement("DELETE FROM BETS WHERE FAN_ID = $1 AND GAME_ID = $2;")
                    .bind("$1", id.id)
                    .bind("$2", game.id)
                    .execute()
                    .awaitFirstOrNull()
            }

        return bet
    }

    private suspend fun bet(game: GameID, team: TeamID, amount: Int) {
        blasement.connectionFactory
            .create()
            .use { connection ->
                val quantity = connection.createStatement("SELECT AMOUNT FROM BETS WHERE FAN_ID = $1 AND GAME_ID = $2")
                    .bind("$1", id.id)
                    .bind("$2", game.id)
                    .execute()
                    .awaitFirstOrNull()
                    ?.map { row, _ -> row["AMOUNT"] as? Int }
                    ?.awaitFirstOrNull()

                if (quantity != null) throw IllegalStateException("Bet already placed for ${game.id}!")

                connection.createStatement("INSERT INTO BETS (FAN_ID, GAME_ID, TEAM_ID, AMOUNT) VALUES ( $1, $2, $3, $4 )")
                    .bind("$1", id.id)
                    .bind("$2", game.id)
                    .bind("$3", team.id)
                    .bind("$4", amount)
                    .execute()
                    .awaitSingle()

                _bets[game] = BlaseballBet(team, amount)
            }
    }

    override suspend fun beg(): Pair<Int?, EnumBegFail?> = semaphore.withWritePermit {
        if (coins > 0) return Pair(null, EnumBegFail.TOO_MANY_COINS)
        if (EnumBlaseballItem.BREAD_CRUMBS !in inventory) return Pair(null, EnumBegFail.NO_BREAD_CRUMBS)

        val begCoins = Random.nextInt(5, 20)
        coins { it + begCoins }

        return Pair(begCoins, null)
    }

    override suspend fun purchaseShopMembershipCard(): Pair<Int?, EnumUnlockFail?> = semaphore.withWritePermit {
        val price = 20

        if (hasUnlockedShop) return null to EnumUnlockFail.ALREADY_UNLOCKED
        if (coins < price) return null to EnumUnlockFail.NOT_ENOUGH_COINS

        coins { it - price }
        shopUnlocked { true }

        return price to null
    }

    override suspend fun purchase(amount: Int, item: EnumBlaseballItem): Pair<Int?, EnumPurchaseItemFail?> = semaphore.withWritePermit {
        if (!hasUnlockedShop) return null to EnumPurchaseItemFail.MEMBERSHIP_LOCKED
        val count = inventory[item]

        if (count == null && inventoryFull) return null to EnumPurchaseItemFail.INVENTORY_FULL

        if (item.price == null) {
            //Tiered Item
            val tiers = item.tiers!!.drop(count ?: 0).take(amount)
            if (tiers.isEmpty()) return null to EnumPurchaseItemFail.TIER_TOO_HIGH

            val price = tiers.sumBy(BlaseballRewardShopItem::price)

            if (coins < price) return null to EnumPurchaseItemFail.NOT_ENOUGH_COINS

            coins { it - price }
            addItem(item) { tiers.size }

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

            coins { it - price }
            item(item) { newCount }

            return price to null
        }
    }

    override suspend fun sell(amount: Int, item: EnumBlaseballItem): Pair<Int?, EnumSellItemFail?> = semaphore.withWritePermit {
        if (!hasUnlockedShop) return null to EnumSellItemFail.MEMBERSHIP_LOCKED
        val count = inventory[item] ?: return null to EnumSellItemFail.ITEM_NOT_IN_INVENTORY
        if (amount > count) return null to EnumSellItemFail.NOT_ENOUGH_ITEMS
        val amountBack = item.sellsFor(count, amount)

        coins { it + amountBack }
        removeItem(item) { amount }

        return amountBack to null
    }

    override suspend fun purchaseSlot(amount: Int): Pair<Int?, EnumPurchaseSlotFail?> = semaphore.withWritePermit {
        if (!hasUnlockedShop) return null to EnumPurchaseSlotFail.MEMBERSHIP_LOCKED

        val availableSlotsToBuy = BlaseballRewardShopItem.SLOT_MULTIPLIERS.size - inventory.size
        if (availableSlotsToBuy < amount) return null to EnumPurchaseSlotFail.TOO_MANY_SLOTS
        val price = amount * SLOT_PRICE
        if (coins < price) return null to EnumPurchaseSlotFail.NOT_ENOUGH_COINS

        coins { it - price }
        inventorySpace { it + amount }

        return price to null
    }

    override suspend fun sellSlot(amount: Int): Pair<Int?, EnumSellSlotFail?> = semaphore.withWritePermit {
        if (!hasUnlockedShop) return null to EnumSellSlotFail.MEMBERSHIP_LOCKED

        if (inventory.size > amount) return null to EnumSellSlotFail.NOT_ENOUGH_SLOTS
        val availableSlotsToSell = inventorySpace - inventory.size
        if (availableSlotsToSell < amount) return null to EnumSellSlotFail.NO_EMPTY_SLOTS

        val amountBack = amount * SLOT_PRICE

        coins { it + amountBack }
        inventorySpace { it - amount }

        return amountBack to null
    }

    override suspend fun placeBet(onGame: GameID, onTeam: TeamID, amount: Int): EnumBetFail? = semaphore.withWritePermit {
        if (amount > coins) return EnumBetFail.NOT_ENOUGH_COINS
        if (amount <= 0) return EnumBetFail.CANT_BET_ZERO

        val count = inventory[EnumBlaseballItem.SNAKE_OIL]?.takeIf { it > 0 } ?: 1
        val maxBet = EnumBlaseballItem.SNAKE_OIL[count - 1]!!.payout

        if (amount > maxBet) return EnumBetFail.BET_TOO_HIGH

        coins { it - amount }
        bet(onGame, onTeam, amount)

        return null
    }
}