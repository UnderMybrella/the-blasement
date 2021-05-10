import com.soywiz.klock.DateTimeTz
import dev.brella.blasement.bindAs
import dev.brella.blasement.common.events.*
import dev.brella.kornea.blaseball.base.common.BlaseballRewardShopSnack
import dev.brella.kornea.blaseball.base.common.BlaseballRewardShopSnack.Companion.SLOT_PRICE
import dev.brella.kornea.blaseball.base.common.EnumBlaseballSnack
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.toolkit.coroutines.ReadWriteSemaphore
import dev.brella.kornea.toolkit.coroutines.withWritePermit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.r2dbc.core.bind as bindNullable
import java.util.*
import kotlin.random.Random

import dev.brella.kornea.blaseball.base.common.UUID as KUUID

fun BlasementFanPayload.toHost(blasement: TheBlasement): BlasementHostFan =
    BlasementHostFan(
        blasement = blasement,
        id = id,
        email = email,
        appleId = appleId,
        googleId = googleId,
        discordId = facebookId,
        name = name,
        password = password,
        coins = coins,
        lastActive = lastActive,
        created = created,
        loginStreak = loginStreak,
        idol = idol,
        favouriteTeam = favouriteTeam,
        hasUnlockedShop = hasUnlockedShop,
        hasUnlockedElections = hasUnlockedElections,
        peanutsEaten = peanutsEaten,
        squirrels = squirrels,
        lightMode = lightMode,
        spread = spread,
        coffee = coffee,
        favNumber = favNumber,
        inventory = inventory,
        inventorySpace = inventorySpace,
        currentBets = currentBets,
        trackers = trackers
    )

class BlasementHostFan(
    val blasement: TheBlasement,
    override val id: FanID,

    override val email: String? = null,
    override val appleId: String? = null,
    override val googleId: String? = null,
    override val discordId: String? = null,

    override val name: String? = null,
    override val password: String? = null,

    coins: Long,

    lastActive: DateTimeTz,
    override val created: DateTimeTz,

    loginStreak: Int,

    idol: PlayerID? = null,
    favouriteTeam: TeamID?,

    hasUnlockedShop: Boolean = false,
    hasUnlockedElections: Boolean = false,

    peanutsEaten: Int = 0,
    squirrels: Int = 0,
    lightMode: Boolean = false,
    spread: List<Int> = emptyList(),
    coffee: Int? = null,
    favNumber: Int? = null,

    inventory: BlasementInventory = emptyMap(),
    inventorySpace: Int = 8,
    currentBets: Map<GameID, BlaseballBet> = emptyMap(),

    trackers: BlaseballFanTrackers = BlaseballFanTrackers(),

    readOnly: Boolean = false,
    verified: Boolean = false,

    activeLeagueType: String? = null,
    activeLeagueID: String? = null
) : BlasementFan {
    constructor(payload: BlasementFanDatabasePayload, blasement: TheBlasement, items: List<Pair<EnumBlaseballSnack, Int>?>, bets: List<Triple<KUUID, KUUID, Int>>, trackers: BlaseballFanTrackers) : this(
        blasement,
        payload.fanID,
        payload.email,
        payload.appleId,
        payload.googleId,
        payload.facebookId,

        payload.name,
        payload.password,

        payload.coins,

        payload.lastActive,
        payload.created,

        payload.loginStreak,

        payload.idol,
        payload.favouriteTeam,

        payload.hasUnlockedShop,
        payload.hasUnlockedElections,

        payload.peanutsEaten,
        payload.squirrels,
        payload.lightMode,
        payload.spread,
        payload.coffee,
        payload.favNumber,

        inventorySpace = payload.inventorySpace,
        trackers = trackers
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

    private var _lastActive = lastActive
    override val lastActive by ::_lastActive

    private var _loginStreak = loginStreak
    override val loginStreak by ::_loginStreak

    private var _idol = idol
    override val idol by ::_idol

    private var _favouriteTeam = favouriteTeam
    override val favouriteTeam by ::_favouriteTeam

    private var _shopUnlocked = hasUnlockedShop
    override val hasUnlockedShop by ::_shopUnlocked

    private var _electionsUnlocked = hasUnlockedElections
    override val hasUnlockedElections by ::_electionsUnlocked

    private var _peanutsEaten = peanutsEaten
    override val peanutsEaten by ::_peanutsEaten

    private var _squirrels = squirrels
    override val squirrels by ::_squirrels

    private var _lightMode = lightMode
    override val lightMode by ::_lightMode

    private var _spread: MutableList<Int> = ArrayList(spread)
    override val spread by ::_spread

    private var _coffee = coffee
    override val coffee by ::_coffee

    private var _favNumber = favNumber
    override val favNumber by ::_favNumber

    private val _inventory: BlasementMutableInventory = EnumMap(EnumBlaseballSnack::class.java)
    override val inventory: Map<EnumBlaseballSnack, Int> by ::_inventory

    private var _inventorySpace: Int = inventorySpace
    override val inventorySpace by ::_inventorySpace

    val inventoryFull: Boolean
        get() = _inventory.size == _inventorySpace

    private val _bets: MutableMap<GameID, BlaseballBet> = HashMap(currentBets)
    override val currentBets: Map<GameID, BlaseballBet> by ::_bets

    private var _trackers = trackers
    override val trackers by ::_trackers

    private var _readOnly = readOnly
    val readOnly by ::_readOnly

    private var _verified = verified
    val verified by ::_verified

    private var _activeLeagueType = activeLeagueType
    val activeLeagueType by ::_activeLeagueType

    private var _activeLeagueID = activeLeagueID
    val activeLeagueID by ::_activeLeagueID

    suspend fun changeLeague(leagueType: String, leagueID: String) {
        _activeLeagueType = leagueType
        _activeLeagueID = leagueID

        blasement.client.sql("UPDATE fans SET active_league_type = $1, active_league_id = $2 WHERE fan_id = $3")
            .bind("$1", leagueType)
            .bind("$2", leagueID)
            .bindAs("$3", id)
            .await()
    }

    private suspend inline fun coins(calculate: (coins: Long) -> Long) = setCoins(calculate(_coins))
    suspend fun setCoins(calculate: (coins: Long) -> Long) = semaphore.withWritePermit { setCoins(calculate(_coins)) }
    private suspend fun setCoins(newValue: Long) {
        blasement.client.sql("UPDATE fans SET coins = $2 WHERE fan_id = $1")
            .bindAs("$1", id)
            .bind("$2", newValue)
            .await()

        _coins = newValue
    }

    private suspend inline fun shopUnlocked(calculate: (hasUnlockedShop: Boolean) -> Boolean) = setShopUnlocked(calculate(_shopUnlocked))
    suspend fun setShopUnlocked(calculate: (hasUnlockedShop: Boolean) -> Boolean) = semaphore.withWritePermit { setShopUnlocked(calculate(_shopUnlocked)) }
    private suspend fun setShopUnlocked(newValue: Boolean) {
        blasement.client.sql("UPDATE fans SET has_unlocked_shop = $2 WHERE fan_id = $1")
            .bindAs("$1", id)
            .bind("$2", newValue)
            .await()

        _shopUnlocked = newValue
    }

    private suspend inline fun electionsUnlocked(calculate: (hasUnlockedElections: Boolean) -> Boolean) = setElectionsUnlocked(calculate(_electionsUnlocked))
    suspend fun setElectionsUnlocked(calculate: (hasUnlockedElections: Boolean) -> Boolean) = semaphore.withWritePermit { setElectionsUnlocked(calculate(_electionsUnlocked)) }
    private suspend fun setElectionsUnlocked(newValue: Boolean) {
        blasement.client.sql("UPDATE fans SET has_unlocked_elections = $2 WHERE fan_id = $1")
            .bindAs("$1", id)
            .bind("$2", newValue)
            .await()

        _electionsUnlocked = newValue
    }

    private suspend inline fun favNumber(calculate: (favNumber: Int?) -> Int?) = setFavNumber(calculate(_favNumber))
    suspend fun setFavNumber(calculate: (favNumber: Int?) -> Int?) = semaphore.withWritePermit { setFavNumber(calculate(_favNumber)) }
    private suspend fun setFavNumber(newValue: Int?) {
        //row["coffee"] as? Int,
        //                        row["fav_number"] as? Int

        blasement.client.sql("UPDATE fans SET fav_number = $2 WHERE fan_id = $1")
            .bindAs("$1", id)
            .bindNullable("$2", newValue)
            .await()

        _favNumber = newValue
    }

    private suspend inline fun coffee(calculate: (coffee: Int?) -> Int?) = setCoffee(calculate(_coffee))
    suspend fun setCoffee(calculate: (coffee: Int?) -> Int?) = semaphore.withWritePermit { setCoffee(calculate(_coffee)) }
    private suspend fun setCoffee(newValue: Int?) {
        //row["coffee"] as? Int,
        //                        row["fav_number"] as? Int

        blasement.client.sql("UPDATE fans SET coffee = $2 WHERE fan_id = $1")
            .bindAs("$1", id)
            .bindNullable("$2", newValue)
            .await()

        _coffee = newValue
    }

    private suspend inline fun inventorySpace(calculate: (inventorySpace: Int) -> Int) = setInventorySpace(calculate(_inventorySpace))
    suspend fun setInventorySpace(calculate: (inventorySpace: Int) -> Int) = semaphore.withWritePermit { setInventorySpace(calculate(_inventorySpace)) }
    private suspend fun setInventorySpace(newValue: Int) {
        blasement.client.sql("UPDATE fans SET inventory_space = $2 WHERE fan_id = $1")
            .bindAs("$1", id)
            .bind("$2", newValue)
            .await()

        _inventorySpace = newValue
    }

    private suspend inline fun item(item: EnumBlaseballSnack, calculate: (amount: Int?) -> Int) = setItemQuantity(item, calculate(inventory[item]))
    suspend fun setItemQuantity(item: EnumBlaseballSnack, calculate: (amount: Int?) -> Int) = semaphore.withWritePermit { setItemQuantity(item, calculate(inventory[item])) }

    private suspend inline fun addItem(item: EnumBlaseballSnack, calculate: (amount: Int?) -> Int) = setItemQuantity(item, inventory[item]?.let { it + calculate(it) } ?: calculate(null))
    suspend fun addItemQuantity(item: EnumBlaseballSnack, calculate: (amount: Int?) -> Int) = semaphore.withWritePermit { setItemQuantity(item, inventory[item]?.let { it + calculate(it) } ?: calculate(null)) }

    private suspend inline fun removeItem(item: EnumBlaseballSnack, calculate: (amount: Int?) -> Int) {
        return setItemQuantity(item, (inventory[item] ?: return).let { it - calculate(it) })
    }

    suspend fun removeItemQuantity(item: EnumBlaseballSnack, calculate: (amount: Int?) -> Int) = semaphore.withWritePermit { setItemQuantity(item, (inventory[item] ?: return@withWritePermit).let { it - calculate(it) }) }

    private suspend fun setItemQuantity(item: EnumBlaseballSnack, newValue: Int) {
        if (inventoryFull && item !in _inventory) {
            if (newValue <= 0) return

            throw IllegalArgumentException("No space in $name[${id.id}]'s inventory for ${newValue}x $item!")
        }
        val quantity = blasement.client.sql("SELECT quantity FROM items WHERE fan_id = $1 AND item_name = $2")
            .bindAs("$1", id)
            .bind("$2", item.name)
            .map { row -> row["quantity"] as Number }
            .awaitSingleOrNull()
            ?.toInt()

        when {
            quantity == null -> {
                blasement.client.sql("INSERT INTO items (fan_id, item_name, quantity) VALUES ( $1, $2, $3 )")
                    .bindAs("$1", id)
                    .bind("$2", item.name)
                    .bind("$3", newValue)
                    .await()

                _inventory[item] = newValue
            }
            newValue > 0 -> {
                blasement.client.sql("UPDATE items SET quantity = $3 WHERE fan_id = $1 AND item_name = $2")
                    .bindAs("$1", id)
                    .bind("$2", item.name)
                    .bind("$3", newValue)
                    .await()

                _inventory[item] = newValue
            }
            else -> {
                blasement.client.sql("DELETE FROM items WHERE fan_id = $1 AND item_name = $2")
                    .bindAs("$1", id)
                    .bind("$2", item.name)
                    .await()

                _inventory.remove(item)
            }
        }
    }

    suspend fun gameCompleted(game: GameID): BlaseballBet? {
        val bet = _bets.remove(game) ?: return null
        println("Bet: $bet")

//        blasement.client.sql("DELETE FROM bets WHERE fan_id = $1 AND game_id = $2")
//            .bindAs("$1", id)
//            .bindAs("$2", game)
//            .await()

        return bet
    }

    private suspend fun bet(game: GameID, team: TeamID, amount: Int) {
        val quantity = blasement.client.sql("SELECT amount FROM BETS WHERE fan_id = $1 AND game_id = $2")
            .bindAs("$1", id)
            .bindAs("$2", game)
            .map { row, _ -> row["amount"] as? Number }
            .awaitSingleOrNull()
            ?.toInt()

        if (quantity != null) throw IllegalStateException("Bet already placed for ${game.id}!")

        blasement.client.sql("INSERT INTO bets (fan_id, game_id, team_id, amount) VALUES ( $1, $2, $3, $4 )")
            .bindAs("$1", id)
            .bindAs("$2", game)
            .bindAs("$3", team)
            .bind("$4", amount)
            .await()

        _bets[game] = BlaseballBet(team, amount)
    }

    private suspend inline fun changeTeam(calculate: (favouriteTeam: TeamID?) -> TeamID) = setFavouriteTeam(calculate(_favouriteTeam))
    suspend fun setFavouriteTeam(calculate: (favouriteTeam: TeamID?) -> TeamID) = semaphore.withWritePermit { setFavouriteTeam(calculate(_favouriteTeam)) }
    private suspend fun setFavouriteTeam(newValue: TeamID) {
        blasement.client.sql("UPDATE fans SET favourite_team = $2 WHERE fan_id = $1")
            .bindAs("$1", id)
            .bindAs("$2", newValue)
            .await()

        _favouriteTeam = newValue
    }

    private suspend inline fun changeIdol(calculate: (idol: PlayerID?) -> PlayerID) = setIdol(calculate(_idol))
    suspend fun setIdol(calculate: (idol: PlayerID?) -> PlayerID) = semaphore.withWritePermit { setIdol(calculate(_idol)) }
    private suspend fun setIdol(newValue: PlayerID) {
        blasement.client.sql("UPDATE fans SET idol = $2 WHERE fan_id = $1")
            .bindAs("$1", id)
            .bindAs("$2", newValue)
            .await()

        _idol = newValue
    }

    suspend fun changeTeam(newFavourite: TeamID): Pair<TeamID?, EnumChangeTeamFail?> = semaphore.withWritePermit {
        when (val oldTeam = _favouriteTeam) {
            newFavourite -> return null to EnumChangeTeamFail.ALREADY_FAVOURITE_TEAM
            null -> {
                setFavouriteTeam(newFavourite)
                return null to null
            }
            else -> {
                val flutes = inventory[EnumBlaseballSnack.FLUTES] ?: return null to EnumChangeTeamFail.NO_FLUTE

                if (flutes <= 0) return null to EnumChangeTeamFail.NO_FLUTE

                removeItem(EnumBlaseballSnack.FLUTES) { 1 }
                setFavouriteTeam(newFavourite)

                return oldTeam to null
            }
        }
    }
    suspend fun changeIdol(newIdol: PlayerID): Pair<PlayerID?, EnumChangeIdolFail?> = semaphore.withWritePermit {
        val oldIdol = _idol

        if (oldIdol == newIdol) return null to EnumChangeIdolFail.ALREADY_IDOL
        if (coins < 200) return null to EnumChangeIdolFail.NOT_ENOUGH_COINS

        coins { it - 200 }
        setIdol { newIdol }

        return oldIdol to null
    }

    override suspend fun beg(): Pair<Int?, EnumBegFail?> = semaphore.withWritePermit {
        if (coins > 0) return Pair(null, EnumBegFail.TOO_MANY_COINS)
        if (EnumBlaseballSnack.BREAD_CRUMBS !in inventory) return Pair(null, EnumBegFail.NO_BREAD_CRUMBS)

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

    suspend fun purchaseVotingRights(): Pair<Int?, EnumUnlockFail?> = semaphore.withWritePermit {
        val price = 100

        if (hasUnlockedElections) return null to EnumUnlockFail.ALREADY_UNLOCKED
        if (coins < price) return null to EnumUnlockFail.NOT_ENOUGH_COINS

        coins { it - price }
        electionsUnlocked { true }

        return price to null
    }

    override suspend fun buySnack(amount: Int, item: EnumBlaseballSnack): Pair<Int?, EnumPurchaseItemFail?> = semaphore.withWritePermit {
        if (!hasUnlockedShop) return null to EnumPurchaseItemFail.MEMBERSHIP_LOCKED
        val count = inventory[item]

        if (count == null && inventoryFull) return null to EnumPurchaseItemFail.INVENTORY_FULL

        if (item.price == null) {
            //Tiered Item
            val tiers = item.tiers!!.drop(count ?: 0).take(amount)
            if (tiers.isEmpty()) return null to EnumPurchaseItemFail.TIER_TOO_HIGH

            val price = tiers.sumBy(BlaseballRewardShopSnack::price)

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

    override suspend fun sell(amount: Int, item: EnumBlaseballSnack): Pair<Int?, EnumSellItemFail?> = semaphore.withWritePermit {
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

        val availableSlotsToBuy = BlaseballRewardShopSnack.SLOT_MULTIPLIERS.size - inventory.size
        if (availableSlotsToBuy < amount) return null to EnumPurchaseSlotFail.TOO_MANY_SLOTS
        val price = amount * SLOT_PRICE
        if (coins < price) return null to EnumPurchaseSlotFail.NOT_ENOUGH_COINS

        coins { it - price }
        inventorySpace { it + amount }

        return price to null
    }

    override suspend fun sellSlot(amount: Int): Pair<Int?, EnumSellSlotFail?> = semaphore.withWritePermit {
        if (!hasUnlockedShop) return null to EnumSellSlotFail.MEMBERSHIP_LOCKED

        if (inventory.size < amount) return null to EnumSellSlotFail.NOT_ENOUGH_SLOTS
        val availableSlotsToSell = inventorySpace - inventory.size
        if (availableSlotsToSell < amount) return null to EnumSellSlotFail.NO_EMPTY_SLOTS

        val amountBack = amount * SLOT_PRICE

        //TODO: The Blaseball *server* doesn't do this, but the *client* does - determine which is best
//        coins { it + amountBack }
        inventorySpace { it - amount }

        return amountBack to null
    }

    override suspend fun placeBet(onGame: GameID, onTeam: TeamID, amount: Int): EnumBetFail? = semaphore.withWritePermit {
        if (amount > coins) return EnumBetFail.NOT_ENOUGH_COINS
        if (amount <= 0) return EnumBetFail.CANT_BET_ZERO

        val count = inventory[EnumBlaseballSnack.SNAKE_OIL]?.takeIf { it > 0 } ?: return@withWritePermit EnumBetFail.NO_SNAKE_OIL
        val maxBet = EnumBlaseballSnack.SNAKE_OIL[count - 1]!!.payout

        if (amount > maxBet) return EnumBetFail.BET_TOO_HIGH

        coins { it - amount }
        bet(onGame, onTeam, amount)

        return null
    }

    suspend fun getToasts(by: Long, clear: Boolean = true): List<String> {
        if (clear) {
            val toasts = blasement.client.sql("SELECT id, toast FROM toasts WHERE fan_id = $1 AND timestamp <= $2")
                             .bindAs("$1", id)
                             .bind("$2", by)
                             .map { row -> Pair(row["id"] as Long, row["toast"] as String) }
                             .all()
                             .collectList()
                             .awaitFirstOrNull() ?: return emptyList()

            blasement.client.sql("DELETE FROM toasts WHERE id = ANY($1)")
                .bind("$1", Array(toasts.size) { toasts[it].first })
                .await()

            return toasts.map(Pair<Long, String>::second)
        } else {
            return blasement.client.sql("SELECT toast FROM toasts WHERE fan_id = $1 AND timestamp <= $2")
                             .bindAs("$1", id)
                             .bind("$2", by)
                             .map { row -> row["toast"] as String }
                             .all()
                             .collectList()
                             .awaitFirstOrNull() ?: emptyList()
        }
    }

    suspend fun addToast(toast: String, time: Long) {
        blasement.client.sql("INSERT INTO toasts (fan_id, toast, timestamp) VALUES ($1, $2, $3)")
            .bindAs("$1", id)
            .bind("$2", toast)
            .bind("$3", time)
            .await()

        fanEvents.emit(ServerEvent.Toast(toast, time))
    }

    init {
        _inventory.putAll(inventory)
    }
}