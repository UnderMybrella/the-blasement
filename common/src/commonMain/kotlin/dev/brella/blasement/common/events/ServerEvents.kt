package dev.brella.blasement.common.events

import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.base.common.beans.BlaseballStreamDataGame
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@Serializable(ServerEventSerialiser::class)
/** This is an event sent **from** the server to the client */
sealed class ServerEvent {
    @Serializable
    @SerialName("CURRENT_DATE")
    data class CurrentDate(val season: Int, val day: Int): ServerEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("GAME_LIST")
    data class GameList(val season: Int, val day: Int, val games: List<BlaseballDatabaseGame>): ServerEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("GAME_UPDATE")
    data class GameUpdate(val schedule: BlaseballStreamDataGame): ServerEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("GLOBAL_FEED_EVENT")
    data class GlobalFeedEvent(val event: BlaseballFeedEventWithContext): ServerEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("NO_BETTER")
    object Unauthenticated: ServerEvent()

    @Serializable
    @SerialName("INVALID_AUTH_TOKEN")
    object InvalidAuthToken: ServerEvent()

    @Serializable
    @SerialName("FAN_CREATION_TOO_EXPENSIVE")
    object FanCreationTooExpensive: ServerEvent()

    @Serializable
    @SerialName("FAN_CREATED")
    data class FanCreated(val userID: FanID, val authToken: String, val payload: BlasementFanPayload): ServerEvent()

    @Serializable
    @SerialName("FAN_PAYLOAD")
    data class FanPayload(val payload: BlasementFanPayload): ServerEvent()

    @Serializable
    @SerialName("NO_GAME_FOUND")
    object NoGameFound: ServerEvent()

    @Serializable
    @SerialName("BETTING_RETURNS")
    data class BettingReturns(val potentialReturns: Int?): ServerEvent()

    sealed class FanActionResponse: ServerEvent() {
        @Serializable
        @SerialName("BEGGED")
        data class Beg(val coinsFound: Int?, val error: EnumBegFail?): FanActionResponse() {
            constructor(pair: Pair<Int?, EnumBegFail?>): this(pair.first, pair.second)

            inline fun asPair() = Pair(coinsFound, error)
        }

        @Serializable
        @SerialName("PURCHASED_SHOP_MEMBERSHIP_CARD")
        data class PurchaseShopMembershipCard(val cost: Int?, val error: EnumUnlockFail?): FanActionResponse() {
            constructor(pair: Pair<Int?, EnumUnlockFail?>): this(pair.first, pair.second)
        }

        @Serializable
        @SerialName("PURCHASED_ITEM")
        data class PurchaseItem(val cost: Int?, val error: EnumPurchaseItemFail?): FanActionResponse() {
            constructor(pair: Pair<Int?, EnumPurchaseItemFail?>): this(pair.first, pair.second)
        }

        @Serializable
        @SerialName("SOLD_ITEM")
        data class SoldItem(val returned: Int?, val error: EnumSellItemFail?): FanActionResponse() {
            constructor(pair: Pair<Int?, EnumSellItemFail?>): this(pair.first, pair.second)
        }

        @Serializable
        @SerialName("PURCHASED_SLOT")
        data class PurchaseSlot(val cost: Int?, val error: EnumPurchaseSlotFail?): FanActionResponse() {
            constructor(pair: Pair<Int?, EnumPurchaseSlotFail?>): this(pair.first, pair.second)
        }

        @Serializable
        @SerialName("SOLD_SLOT")
        data class SoldSlot(val cost: Int?, val error: EnumSellSlotFail?): FanActionResponse() {
            constructor(pair: Pair<Int?, EnumSellSlotFail?>): this(pair.first, pair.second)
        }

        @Serializable
        @SerialName("BET_ON_GAME")
        data class BetOnGame(val potentialReturns: Int?, val error: EnumBetFail?): FanActionResponse() {
            constructor(pair: Pair<Int?, EnumBetFail?>): this(pair.first, pair.second)
        }

        @Serializable
        @SerialName("WON_BET")
        data class WonBet(val onGame: GameID, val onTeam: TeamID, val bet: Int, val returns: Int): FanActionResponse()

        @Serializable
        @SerialName("LOST_BET")
        data class LostBet(val onGame: GameID, val onTeam: TeamID, val bet: Int): FanActionResponse()

        @Serializable
        @SerialName("GAINED_MONEY")
        data class GainedMoney(val amount: Int, val reason: EnumGainedMoneyReason): FanActionResponse()
    }

    @OptIn(InternalSerializationApi::class)
    open fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
        encoder.encodeSerializableElement(descriptor, 1, this::class.serializer() as KSerializer<ServerEvent>, this)
    }
}

enum class EnumGainedMoneyReason {
    POPCORN,
    STALE_POPCORN,
    BREAKFAST,
    TAFFY,
    LEMONADE,
    CHIPS,
    BURGER,
    MEATBALL,
    HOT_DOG,
    SUNFLOWER_SEEDS,
    PICKLES,
    SLUSHIE,
    SUNDAE,
    WET_PRETZEL,
    DOUGHNUT,

    EAT_THE_RICH
}

//TODO: Once https://github.com/Kotlin/kotlinx.serialization/pull/1408 has been merged into main, replace with sealed class serialisation via @SerialName
object ServerEventSerialiser : KSerializer<ServerEvent> {
    val EVENTS = listOf(
        ServerEvent.CurrentDate::class to ServerEvent.CurrentDate.serializer(),
        ServerEvent.GameList::class to ServerEvent.GameList.serializer(),
        ServerEvent.GameUpdate::class to ServerEvent.GameUpdate.serializer(),
        ServerEvent.GlobalFeedEvent::class to ServerEvent.GlobalFeedEvent.serializer(),

        ServerEvent.Unauthenticated::class to ServerEvent.Unauthenticated.serializer(),
        ServerEvent.FanPayload::class to ServerEvent.FanPayload.serializer(),
        ServerEvent.InvalidAuthToken::class to ServerEvent.InvalidAuthToken.serializer(),
        ServerEvent.FanCreationTooExpensive::class to ServerEvent.FanCreationTooExpensive.serializer(),
        ServerEvent.FanCreated::class to ServerEvent.FanCreated.serializer(),

        ServerEvent.FanActionResponse.Beg::class to ServerEvent.FanActionResponse.Beg.serializer(),
        ServerEvent.FanActionResponse.PurchaseShopMembershipCard::class to ServerEvent.FanActionResponse.PurchaseShopMembershipCard.serializer(),
        ServerEvent.FanActionResponse.PurchaseItem::class to ServerEvent.FanActionResponse.PurchaseItem.serializer(),
        ServerEvent.FanActionResponse.SoldItem::class to ServerEvent.FanActionResponse.SoldItem.serializer(),
        ServerEvent.FanActionResponse.PurchaseSlot::class to ServerEvent.FanActionResponse.PurchaseSlot.serializer(),
        ServerEvent.FanActionResponse.SoldSlot::class to ServerEvent.FanActionResponse.SoldSlot.serializer(),
        ServerEvent.FanActionResponse.BetOnGame::class to ServerEvent.FanActionResponse.BetOnGame.serializer(),

        ServerEvent.FanActionResponse.WonBet::class to ServerEvent.FanActionResponse.WonBet.serializer(),
        ServerEvent.FanActionResponse.LostBet::class to ServerEvent.FanActionResponse.LostBet.serializer(),
        ServerEvent.FanActionResponse.GainedMoney::class to ServerEvent.FanActionResponse.GainedMoney.serializer()
    )

    inline fun Pair<KClass<out ServerEvent>, KSerializer<out ServerEvent>>.identifier(): String =
        second.descriptor.serialName

    val SERIALISER_MAP = EVENTS.associate { pair ->
        pair.identifier() to pair.second
    }

    val TYPE_NAMES = EVENTS.associate { pair -> pair.first to pair.identifier() }

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ClientEvent") {
        element<String>("type")
        element("data", buildSerialDescriptor("data", PolymorphicKind.SEALED) {
            EVENTS.forEach { pair -> element(pair.identifier(), pair.second.descriptor) }
        })
    }

    override fun deserialize(decoder: Decoder): ServerEvent =
        decoder.decodeStructure(descriptor) {
            val type = decodeStringElement(descriptor, decodeElementIndex(descriptor))
            val deserialiser = SERIALISER_MAP[type] ?: throw IllegalStateException("Unknown event of type '${type}'!")

            decodeSerializableElement(descriptor, decodeElementIndex(descriptor), deserialiser)
        }

    override fun serialize(encoder: Encoder, value: ServerEvent) {
        val type = TYPE_NAMES[value::class] ?: throw IllegalStateException("Unknown event $value")

        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, type)

            value.serialise(this, descriptor)
        }
    }
}