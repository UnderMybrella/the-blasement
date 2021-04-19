package dev.brella.blasement.common.events

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first

internal object NULL {
    override fun toString(): String = "{NULL}"
}

//clientelle.incomingEvents.filterIsInstance<ServerEvent.BetterPayload>()

public suspend fun <T> Flow<T>.doThenWaitFor(operation: suspend () -> Unit, waitFor: suspend (T) -> Boolean): T {
    val subscriber = buffer()
    operation()
    return subscriber.first(waitFor)
}

public suspend inline fun <reified R> Flow<*>.doThenWaitForInstance(operation: suspend () -> Unit): R {
    val subscriber = buffer()
    operation()
    return subscriber.first { it is R } as R
}