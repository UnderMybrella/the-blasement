package dev.brella.blasement

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.utils.io.*

class OutgoingReadChannel(override val contentType: ContentType? = null, val producer: () -> ByteReadChannel): OutgoingContent.ReadChannelContent() {
    override fun readFrom(): ByteReadChannel =
        producer()
}

public suspend fun ApplicationCall.respondReadChannel(
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
    producer: () -> ByteReadChannel
) {
    respond(OutgoingReadChannel(contentType ?: ContentType.Application.OctetStream, producer))
}