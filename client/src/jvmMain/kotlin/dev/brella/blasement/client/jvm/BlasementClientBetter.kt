package dev.brella.blasement.client.jvm

import dev.brella.blasement.common.events.*
import dev.brella.kornea.blaseball.EnumBlaseballItem
import dev.brella.kornea.blaseball.GameID
import dev.brella.kornea.blaseball.PlayerID
import dev.brella.kornea.blaseball.TeamID
import kotlinx.coroutines.runBlocking

class BlasementClientBetter(val clientelle: BlasementClientelle): BlasementBetter {
    override val id: BetterID
    override val name: String
    override var coins: Long = -1L
        private set
    override val idol: PlayerID?
    override val favouriteTeam: TeamID
    override val hasUnlockedShop: Boolean
    override val hasUnlockedElections: Boolean
    override val inventory: BlasementInventory
    override val currentBets: Map<GameID, BlaseballBet>

    override suspend fun beg(): Pair<Int?, EnumBegFail?> {
        val (coinsFound, error) = clientelle.incomingEvents.doThenWaitForInstance<ServerEvent.BetterActionResponse.Beg> { clientelle.send(ClientEvent.PerformBetterAction.Beg) }

        if (coinsFound != null) coins += coinsFound

        return Pair(coinsFound, error)
    }


    override suspend fun purchaseShopMembershipCard(): Pair<Int?, EnumUnlockFail?> {
        val (cost, error) = clientelle.incomingEvents.doThenWaitForInstance<ServerEvent.BetterActionResponse.PurchaseShopMembershipCard> { clientelle.send(ClientEvent.PerformBetterAction.PurchaseMembershipCard) }

        if (cost != null) coins -= cost

        return Pair(cost, error)
    }

    override suspend fun purchase(amount: Int, item: EnumBlaseballItem): Pair<Int?, EnumPurchaseItemFail?> {
        val (cost, error) = clientelle.incomingEvents.doThenWaitForInstance<ServerEvent.BetterActionResponse.PurchaseItem> { clientelle.send(ClientEvent.PerformBetterAction.PurchaseItem(item, amount)) }

        if (cost != null) {
            coins -= cost
            inventory[item] = inventory[item]?.plus(amount) ?: amount
        }

        return Pair(cost, error)
    }

    override suspend fun sell(amount: Int, item: EnumBlaseballItem): Pair<Int?, EnumSellItemFail?> {
        TODO("Not yet implemented")
    }

    override suspend fun purchaseSlot(amount: Int): Pair<Int?, EnumPurchaseSlotFail?> {
        TODO("Not yet implemented")
    }

    override suspend fun sellSlot(amount: Int): Pair<Int?, EnumSellSlotFail?> {
        TODO("Not yet implemented")
    }

    override suspend fun placeBet(onGame: GameID, onTeam: TeamID, amount: Int): EnumBetFail? {
        TODO("Not yet implemented")
    }

    init {
        val base = runBlocking {
            clientelle.incomingEvents.doThenWaitForInstance<ServerEvent.BetterPayload> { clientelle.send(ClientEvent.GetBetter) }
        }.better

        this.id = base.id
        this.name = base.name
        this.coins = base.coins
        this.idol = base.idol
        this.favouriteTeam = base.favouriteTeam
        this.hasUnlockedShop = base.hasUnlockedShop
        this.hasUnlockedElections = base.hasUnlockedElections
        this.inventory = base.inventory
        this.currentBets = base.currentBets
    }
}