package dev.brella.blasement.common.events

import dev.brella.kornea.blaseball.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.beans.BlaseballStreamDataGame
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
    @SerialName("BETTER_PAYLOAD")
    data class BetterPayload(val better: BlasementBetterPayload): ServerEvent()

    sealed class BetterActionResponse: ServerEvent() {
        @Serializable
        @SerialName("BEGGED")
        data class Beg(val coinsFound: Int?, val error: EnumBegFail?): BetterActionResponse() {
            constructor(pair: Pair<Int?, EnumBegFail?>): this(pair.first, pair.second)

            inline fun asPair() = Pair(coinsFound, error)
        }

        @Serializable
        @SerialName("PURCHASED_SHOP_MEMBERSHIP_CARD")
        data class PurchaseShopMembershipCard(val cost: Int?, val error: EnumUnlockFail?): BetterActionResponse() {
            constructor(pair: Pair<Int?, EnumUnlockFail?>): this(pair.first, pair.second)
        }

        @Serializable
        @SerialName("PURCHASED_ITEM")
        data class PurchaseItem(val cost: Int?, val error: EnumPurchaseItemFail?): BetterActionResponse() {
            constructor(pair: Pair<Int?, EnumPurchaseItemFail?>): this(pair.first, pair.second)
        }
    }

    @OptIn(InternalSerializationApi::class)
    open fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
        encoder.encodeSerializableElement(descriptor, 1, this::class.serializer() as KSerializer<ServerEvent>, this)
    }
}

//TODO: Once https://github.com/Kotlin/kotlinx.serialization/pull/1408 has been merged into main, replace with sealed class serialisation via @SerialName
object ServerEventSerialiser : KSerializer<ServerEvent> {
    val EVENTS = listOf(
        ServerEvent.CurrentDate::class to ServerEvent.CurrentDate.serializer(),
        ServerEvent.GameList::class to ServerEvent.GameList.serializer(),
        ServerEvent.GameUpdate::class to ServerEvent.GameUpdate.serializer(),
        ServerEvent.GlobalFeedEvent::class to ServerEvent.GlobalFeedEvent.serializer(),
        ServerEvent.BetterPayload::class to ServerEvent.BetterPayload.serializer(),
        ServerEvent.BetterActionResponse.Beg::class to ServerEvent.BetterActionResponse.Beg.serializer(),
        ServerEvent.BetterActionResponse.PurchaseShopMembershipCard::class to ServerEvent.BetterActionResponse.PurchaseShopMembershipCard.serializer(),
        ServerEvent.BetterActionResponse.PurchaseItem::class to ServerEvent.BetterActionResponse.PurchaseItem.serializer()
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