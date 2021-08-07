package dev.brella.blasement.endpoints

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import io.ktor.application.*
import io.ktor.websocket.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

interface BlaseballEndpoint {
    suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement?

    fun describe(): JsonElement?
}

interface BlaseballUpdatableEndpoint {
    suspend fun updateDataFor(league: BlasementLeague, call: ApplicationCall)
    suspend fun updateDataForWebSocket(league: BlasementLeague, session: WebSocketServerSession, call: ApplicationCall)
}