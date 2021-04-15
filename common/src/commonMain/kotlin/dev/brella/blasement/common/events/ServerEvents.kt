package dev.brella.blasement.common.events

import dev.brella.kornea.blaseball.GameID
import dev.brella.kornea.blaseball.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.beans.BlaseballFeedEvent
import dev.brella.kornea.blaseball.beans.BlaseballStreamDataSchedule
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
import kotlin.reflect.KClass

@Serializable(ServerEventSerialiser::class)
/** This is an event sent **from** the client to the server */
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
    data class GameUpdate(val schedule: BlaseballStreamDataSchedule): ServerEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    @Serializable
    @SerialName("GLOBAL_FEED_EVENT")
    data class GlobalFeedEvent(val event: BlaseballFeedEvent): ServerEvent() {
        override fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor) {
            encoder.encodeSerializableElement(descriptor, 1, serializer(), this)
        }
    }

    abstract fun serialise(encoder: CompositeEncoder, descriptor: SerialDescriptor)
}

//TODO: Once https://github.com/Kotlin/kotlinx.serialization/pull/1408 has been merged into main, replace with sealed class serialisation via @SerialName
object ServerEventSerialiser : KSerializer<ServerEvent> {
    val EVENTS = listOf(
        ServerEvent.CurrentDate::class to ServerEvent.CurrentDate.serializer(),
        ServerEvent.GameList::class to ServerEvent.GameList.serializer(),
        ServerEvent.GameUpdate::class to ServerEvent.GameUpdate.serializer(),
        ServerEvent.GlobalFeedEvent::class to ServerEvent.GlobalFeedEvent.serializer()
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