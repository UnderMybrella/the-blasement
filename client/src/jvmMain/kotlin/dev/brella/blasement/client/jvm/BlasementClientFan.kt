package dev.brella.blasement.client.jvm

import com.soywiz.klock.DateTimeTz
import dev.brella.blasement.common.events.*
import dev.brella.kornea.blaseball.base.common.EnumBlaseballSnack
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import kotlinx.coroutines.withTimeout

inline fun BlasementFanPayload.toClient(clientele: BlasementClientele) =
    BlasementClientFan(
        clientele,
        id,
        name,
        coins,
        idol,
        favouriteTeam,
        hasUnlockedShop,
        hasUnlockedElections,
        inventory.toMutableMap(),
        inventorySpace,
        currentBets.toMutableMap(),
        email,
        appleId,
        googleId,
        facebookId,
        password,
        lastActive,
        created,
        loginStreak,
        peanutsEaten,
        squirrels,
        lightMode,
        spread,
        coffee,
        favNumber,
        trackers
    )

suspend fun BlasementClientele.getSelf(): BlasementClientFan =
    incomingEvents.doThenWaitForInstance<ServerEvent.FanPayload> { send(ClientEvent.GetSelf) }
        .payload.toClient(this)

suspend fun BlasementClientele.getAuthenticated(authToken: String): BlasementClientFan =
    incomingEvents.doThenWaitForInstance<ServerEvent.FanPayload> { send(ClientEvent.AuthenticateFan(authToken)) }
        .payload.toClient(this)

class BlasementClientFan(
    val clientele: BlasementClientele,
    override val id: FanID,
    override val name: String?,
    override var coins: Long,
    override val idol: PlayerID?,
    override val favouriteTeam: TeamID?,
    override val hasUnlockedShop: Boolean,
    override val hasUnlockedElections: Boolean,
    override val inventory: BlasementMutableInventory,
    override var inventorySpace: Int,
    override val currentBets: MutableMap<GameID, BlaseballBet>,

    override val email: String?,
    override val appleId: String?,
    override val googleId: String?,
    override val facebookId: String?,
    override val password: String?,
    override val lastActive: DateTimeTz,
    override val created: DateTimeTz,
    override val loginStreak: Int,
    override val peanutsEaten: Int,
    override val squirrels: Int,
    override val lightMode: Boolean,
    override val spread: List<Int>,
    override val coffee: Int?,
    override val favNumber: Int?,
    override val trackers: BlaseballFanTrackers
) : BlasementFan {
    override suspend fun beg(): Pair<Int?, EnumBegFail?> = withTimeout(10_000L) {
        val (coinsFound, error) = clientele.incomingEvents.doThenWaitForInstance<ServerEvent.FanActionResponse.Beg> { clientele.send(ClientEvent.PerformFanAction.Beg) }

        if (coinsFound != null) coins += coinsFound

        return@withTimeout Pair(coinsFound, error)
    }


    override suspend fun purchaseShopMembershipCard(): Pair<Int?, EnumUnlockFail?> = withTimeout(10_000L) {
        val (cost, error) = clientele.incomingEvents.doThenWaitForInstance<ServerEvent.FanActionResponse.PurchaseShopMembershipCard> { clientele.send(ClientEvent.PerformFanAction.PurchaseMembershipCard) }

        if (cost != null) coins -= cost

        return@withTimeout Pair(cost, error)
    }

    override suspend fun buySnack(amount: Int, item: EnumBlaseballSnack): Pair<Int?, EnumPurchaseItemFail?> = withTimeout(10_000L) {
        val (cost, error) = clientele.incomingEvents.doThenWaitForInstance<ServerEvent.FanActionResponse.PurchaseItem> { clientele.send(ClientEvent.PerformFanAction.PurchaseItem(item, amount)) }

        if (cost != null) {
            coins -= cost
            inventory[item] = inventory[item]?.plus(amount) ?: amount
        }

        return@withTimeout Pair(cost, error)
    }

    override suspend fun sell(amount: Int, item: EnumBlaseballSnack): Pair<Int?, EnumSellItemFail?> = withTimeout(10_000L) {
        val (amountBack, error) = clientele.incomingEvents.doThenWaitForInstance<ServerEvent.FanActionResponse.SoldItem> { clientele.send(ClientEvent.PerformFanAction.SellItem(item, amount)) }

        if (amountBack != null) {
            coins -= amountBack
            inventory[item] = inventory[item]?.plus(amount) ?: amount
        }

        return@withTimeout Pair(amountBack, error)
    }

    override suspend fun purchaseSlot(amount: Int): Pair<Int?, EnumPurchaseSlotFail?> = withTimeout(10_000L) {
        val (cost, error) = clientele.incomingEvents.doThenWaitForInstance<ServerEvent.FanActionResponse.PurchaseSlot> { clientele.send(ClientEvent.PerformFanAction.PurchaseSlot(amount)) }

        if (cost != null) {
            coins -= cost
            inventorySpace += amount
        }

        return@withTimeout Pair(cost, error)
    }

    override suspend fun sellSlot(amount: Int): Pair<Int?, EnumSellSlotFail?> = withTimeout(10_000L) {
        val (amountBack, error) = clientele.incomingEvents.doThenWaitForInstance<ServerEvent.FanActionResponse.SoldSlot> { clientele.send(ClientEvent.PerformFanAction.SellSlot(amount)) }

        if (amountBack != null) {
            coins += amountBack
            inventorySpace -= amount
        }

        return@withTimeout Pair(amountBack, error)
    }

    override suspend fun placeBet(onGame: GameID, onTeam: TeamID, amount: Int): EnumBetFail? = withTimeout(10_000L) {
        val (_, error) = clientele.incomingEvents.doThenWaitForInstance<ServerEvent.FanActionResponse.BetOnGame> { clientele.send(ClientEvent.PerformFanAction.PlaceBet(onGame, onTeam, amount)) }

        if (error == null) {
            coins -= amount
            currentBets[onGame] = BlaseballBet(onTeam, amount)
        }

        return@withTimeout error
    }
}