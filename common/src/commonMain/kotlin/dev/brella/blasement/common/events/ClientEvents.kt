package dev.brella.blasement.common.events

import dev.brella.kornea.blaseball.base.common.EnumBlaseballItem
import dev.brella.kornea.blaseball.base.common.GameID
import dev.brella.kornea.blaseball.base.common.TeamID
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

@Serializable(ClientEventSerialiser::class)
/** This is an event sent **from** the client to the server */
sealed class ClientEvent {
    @Serializable
    @SerialName("SUBSCRIBE_TO_GAME")
    data class SubscribeToGame(val game: GameID) : ClientEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("SUBSCRIBE_TO_GLOBAL_FEED")
    object SubscribeToGlobalFeed : ClientEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("SUBSCRIBE_TO_GLOBAL_FEED_EVENTS")
    data class SubscribeToGlobalFeedEvents(val types: List<Int>): ClientEvent() {
        constructor(vararg types: Int): this(types.toList())
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("SUBSCRIBE_TO_LIVE_FEED")
    object SubscribeToLiveFeed : ClientEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("GET_DATE")
    object GetDate : ClientEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("GET_TODAYS_GAMES")
    object GetTodaysGames : ClientEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("GET_TOMORROWS_GAMES")
    object GetTomorrowsGames : ClientEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("AUTHENTICATE_FAN")
    data class AuthenticateFan(val authToken: String): ClientEvent()

    @Serializable
    @SerialName("CREATE_NEW_FAN")
    data class CreateNewFan(val hashcashToken: String, val name: String, val favouriteTeam: TeamID): ClientEvent()

    @Serializable
    @SerialName("GET_SELF")
    object GetSelf: ClientEvent()

    @Serializable
    @SerialName("BETTING_RETURNS")
    data class BettingReturns(val onGame: GameID, val onTeam: TeamID, val amount: Int): ClientEvent()

    sealed class PerformFanAction: ClientEvent() {
        @Serializable
        @SerialName("BEG")
        object Beg: PerformFanAction()

        @Serializable
        @SerialName("PURCHASE_MEMBERSHIP_CARD")
        object PurchaseMembershipCard: PerformFanAction()

        @Serializable
        @SerialName("PURCHASE_ITEM")
        data class PurchaseItem(val item: @Serializable(ItemSerializer::class) EnumBlaseballItem, val amount: Int): PerformFanAction()

        @Serializable
        @SerialName("SELL_ITEM")
        data class SellItem(val item: @Serializable(ItemSerializer::class) EnumBlaseballItem, val amount: Int): PerformFanAction()

        @Serializable
        @SerialName("PURCHASE_SLOT")
        data class PurchaseSlot(val amount: Int): PerformFanAction()

        @Serializable
        @SerialName("SELL_SLOT")
        data class SellSlot(val amount: Int): PerformFanAction()

        @Serializable
        @SerialName("PlACE_BET")
        data class PlaceBet(val onGame: GameID, val onTeam: TeamID, val amount: Int): PerformFanAction()
    }

    @OptIn(InternalSerializationApi::class)
    open fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
        encoder.encodeSerializableElement(descriptor, 1, this::class.serializer() as KSerializer<ClientEvent>, this)
    }
}

//TODO: Once https://github.com/Kotlin/kotlinx.serialization/pull/1408 has been merged into main, replace with sealed class serialisation via @SerialName
object ClientEventSerialiser : KSerializer<ClientEvent> {
    val EVENTS = listOf(
        ClientEvent.SubscribeToGame::class to ClientEvent.SubscribeToGame.serializer(),

        ClientEvent.SubscribeToGlobalFeed::class to ClientEvent.SubscribeToGlobalFeed.serializer(),
        ClientEvent.SubscribeToGlobalFeedEvents::class to ClientEvent.SubscribeToGlobalFeedEvents.serializer(),

        ClientEvent.SubscribeToLiveFeed::class to ClientEvent.SubscribeToLiveFeed.serializer(),

        ClientEvent.GetDate::class to ClientEvent.GetDate.serializer(),
        ClientEvent.GetTodaysGames::class to ClientEvent.GetTodaysGames.serializer(),
        ClientEvent.GetTomorrowsGames::class to ClientEvent.GetTomorrowsGames.serializer(),

        ClientEvent.AuthenticateFan::class to ClientEvent.AuthenticateFan.serializer(),
        ClientEvent.GetSelf::class to ClientEvent.GetSelf.serializer(),
        ClientEvent.CreateNewFan::class to ClientEvent.CreateNewFan.serializer(),

        ClientEvent.PerformFanAction.Beg::class to ClientEvent.PerformFanAction.Beg.serializer(),
        ClientEvent.PerformFanAction.PurchaseMembershipCard::class to ClientEvent.PerformFanAction.PurchaseMembershipCard.serializer(),
        ClientEvent.PerformFanAction.PurchaseItem::class to ClientEvent.PerformFanAction.PurchaseItem.serializer(),
        ClientEvent.PerformFanAction.SellItem::class to ClientEvent.PerformFanAction.SellItem.serializer(),
        ClientEvent.PerformFanAction.PurchaseSlot::class to ClientEvent.PerformFanAction.PurchaseSlot.serializer(),
        ClientEvent.PerformFanAction.SellSlot::class to ClientEvent.PerformFanAction.SellSlot.serializer(),
        ClientEvent.PerformFanAction.PlaceBet::class to ClientEvent.PerformFanAction.PlaceBet.serializer()
    )

    inline fun Pair<KClass<out ClientEvent>, KSerializer<out ClientEvent>>.identifier(): String =
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

    override fun deserialize(decoder: Decoder): ClientEvent =
        decoder.decodeStructure(descriptor) {
            val type = decodeStringElement(descriptor, decodeElementIndex(descriptor))
            val deserialiser = SERIALISER_MAP[type] ?: throw IllegalStateException("Unknown event of type '${type}'!")

            decodeSerializableElement(descriptor, decodeElementIndex(descriptor), deserialiser)
        }

    override fun serialize(encoder: Encoder, value: ClientEvent) {
        val type = TYPE_NAMES[value::class] ?: throw IllegalStateException("Unknown event $value")

        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, type)

            value.serialise(this, descriptor)
        }
    }
}