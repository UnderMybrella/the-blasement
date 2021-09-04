package dev.brella.blasement.endpoints.api

import dev.brella.blasement.data.BlasementLeague
import dev.brella.blasement.data.Request
import dev.brella.blasement.endpoints.BlaseballEndpoint
import dev.brella.blasement.endpoints.JsonTransformer
import dev.brella.blasement.endpoints.Static
import dev.brella.blasement.getJsonArrayOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import kotlinx.serialization.json.*
import java.util.*

interface BlaseballApiGetUserEndpoint : BlaseballEndpoint {
    sealed class GuestSibr : BlaseballApiGetUserEndpoint {
        object Season20 : GuestSibr() {
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
            override suspend fun getDataFor(league: BlasementLeague, request: Request): JsonElement =
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
                        put("Forbidden_Knowledge_Access", 1)
                        put("Stadium_Access", 1)
                        put("Wills_Access", 1)
                    }
                    put("lightMode", false)
                    putJsonArray("spread") {}
                    putJsonArray("snackOrder") {
                        add("Forbidden_Knowledge_Access")
                        add("Stadium_Access")
                        add("Wills_Access")
                        add("E")
                        add("E")
                        add("E")
                        add("E")
                        add("E")
                    }
                    put("favNumber", -1)
                    put("coffee", -1)
                    putJsonObject("trackers") {
                        put("BEGS", 3)
                        put("BETS", 10)
                        put("VOTES_CAST", 1)
                        put("SNACKS_BOUGHT", 3)
                        put("SNACK_UPGRADES", 3)
                    }
                    put("verified", true)
                    put("facebookId", JsonNull)
                    put("googleId", JsonNull)
                    put("appleId", JsonNull)
                }

            override fun describe() = JsonPrimitive("guest_s20")
        }
    }

    companion object {
        infix fun loadFrom(config: JsonElement?): KorneaResult<BlaseballApiGetUserEndpoint?> {
            return KorneaResult.successPooled(
                when (config) {
                    JsonNull -> null
                    null -> GuestSibr.Season20
                    is JsonPrimitive ->
                        when (val type = config.contentOrNull?.lowercase(Locale.getDefault())) {
                            "guest s20", "guest_s20", "guests20" -> GuestSibr.Season20
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint string '$type'")
                        }
                    is JsonObject ->
                        when (val type = config.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "guest s20", "guest_s20", "guests20" -> GuestSibr.Season20
                            "static" -> Static(config["data"] ?: JsonNull, JsonTransformer loadAllFrom config.getJsonArrayOrNull("transformers"))
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown type '$type'")
                        }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown endpoint object '$config'")
                }
            )
        }
    }
}