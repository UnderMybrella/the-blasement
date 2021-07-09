package dev.brella.blasement.endpoints

import io.ktor.application.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.*

fun interface BlasementGetUserEndpoint {
    sealed class GuestSibr: BlasementGetUserEndpoint {
        object Season20: GuestSibr() {
            /**
             * id: "",
            email: "",
            isSignedIn: !1,
            coins: 0,
            isFetching: !0,
            favoriteTeam: null,
            lastActive: Date.now(),
            unlockedShop: !1,
            unlockedElection: !1,
            squirrels: 0,
            idol: null,
            packSize: 8,
            snacks: { Votes: 1 },
            lightMode: !1,
            spread: [],
            snackOrder: ["Votes", "E", "E", "E", "E", "E", "E"],
            favNumber: -1,
            coffee: -1,
            trackers: { BETS: 0, VOTES_CAST: 0, SNACKS_BOUGHT: 0, SNACK_UPGRADES: 0, BEGS: 0 },
            verified: !1,
            facebookId: null,
            googleId: null,
            appleId: null,
             */
            override suspend fun getUserFor(call: ApplicationCall): JsonElement =
                buildJsonObject {
                    put("id", UUID.randomUUID().toString())
                    put("email", "guest@sibr.dev")
                    put("isSignedIn", true)
                    put("coins", 0)
                    put("isFetching", false)
                    put("favoriteTeam", "d2634113-b650-47b9-ad95-673f8e28e687")
                    put("lastActive", "1970-01-01T00:00:00Z")
                    put("unlockedShop", true)
                    put("unlockedElection", true)
                    put("squirrels", 0)
                    put("idol", JsonNull)
                    put("packSize", 8)
                    putJsonObject("snacks") {
                        put("Votes", 1)
                    }
                    put("lightMode", false)
                    putJsonArray("spread") {}
                    putJsonArray("snackOrder") {
                        add("Votes")
                        add("E")
                        add("E")
                        add("E")
                        add("E")
                        add("E")
                        add("E")
                        add("E")
                    }
                    put("favNumber", -1)
                    put("coffee", -1)
                    putJsonObject("trackers") {
                        put("BETS", 0)
                        put("VOTES_CAST", 0)
                        put("SNACKS_BOUGHT", 0)
                        put("SNACK_UPGRADES", 0)
                        put("BEGS", 0)
                    }
                    put("verified", true)
                    put("facebookId", JsonNull)
                    put("googleId", JsonNull)
                    put("appleId", JsonNull)
                }
        }
    }
    suspend fun getUserFor(call: ApplicationCall): JsonElement
}