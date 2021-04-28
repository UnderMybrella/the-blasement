package dev.brella.blasement.common

import com.soywiz.klock.DateTimeTz
import dev.brella.blasement.common.events.FanID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.json.BlaseballDateTimeSerialiser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun createFanPayload(id: FanID, email: String, favouriteTeam: TeamID) =
    BlaseballFanPayload(
        id,
        email,
        null,
        null,
        null,
        null,
        null,
        1000,
        DateTimeTz.nowLocal(),
        DateTimeTz.nowLocal(),
        0,
        favouriteTeam,
        true,
        true,
        0,
        0,
        null,
        BlaseballFanSnacksPayload(
            snakeOil = 1,
            peanuts = 1000
        ),
        false,
        3,
        emptyList(),
        0,
        0,
        listOf(BlaseballSnacks.SNAKE_OIL, BlaseballSnacks.PEANUTS),
        BlaseballFanTrackersPayload(
            begs = 0,
            bets = 0,
            votesCast = 0,
            snacksBought = 2,
            snackUpgrades = 0
        )
    )

@Serializable
data class BlaseballFanPayload(
    val id: FanID,
    val email: String,
    val appleId: String?,
    val googleId: String?,
    val facebookId: String?,
    val name: String?,
    val password: String?,
    val coins: Int,
    val lastActive: @Serializable(BlaseballDateTimeSerialiser::class) DateTimeTz,
    val created: @Serializable(BlaseballDateTimeSerialiser::class) DateTimeTz,
    val loginStreak: Int,
    val favoriteTeam: TeamID,
    val unlockedShop: Boolean,
    val unlockedElection: Boolean,
    val peanutsEaten: Int,
    val squirrels: Int,
    val idol: PlayerID?,
    val snacks: BlaseballFanSnacksPayload,
    val lightMode: Boolean,
    val packSize: Int,
    val spread: List<Int>,
    val coffee: Int,
    val favNumber: Int,
    val snackOrder: List<String>,
    val trackers: BlaseballFanTrackersPayload
)

object BlaseballSnacks {
    const val SUNFLOWER_SEEDS = "Idol_Hits"
    const val HOT_DOG = "Idol_Homers"
    const val CHIPS = "Idol_Strikeouts"
    const val BURGER = "Idol_Shutouts"
    const val VOTES = "Votes"
    const val SNAKE_OIL = "Max_Bet"
    const val POPCORN = "Team_Win"
    const val STALE_POPCORN = "Team_Loss"
    const val TAROT_SPREAD = "Team_Tarot"
    const val PIZZA = "Stadium_Access"
    const val CHEESEBOARD = "Wills_Access"
    const val APPLE = "Forbidden_Knowledge_Access"
    const val FLUTES = "Flutes"
    const val PEANUTS = "Peanuts"
    const val BREAD_CRUMBS = "Beg"
    const val SLUSHIE = "Team_Slush"
    const val WET_PRETZEL = "Black_Hole"
    const val SUNDAE = "Incineration"
    const val PICKLES = "Idol_Steal"
    const val HOT_FRIES = "Idol_Pitcher_Win"
    const val MEATBALL = "Idol_Homer_Allowed"
    const val TAFFY = "Team_Shaming"
    const val LEMONADE = "Team_Shamed"
    const val DOUGHNUT = "Sun_2"
    const val BREAKFAST = "Breakfast"
    const val SLOT = "Slot"
}

@Serializable
data class BlaseballFanSnacksPayload(
    @SerialName(BlaseballSnacks.SUNFLOWER_SEEDS)
    val sunflowerSeeds: Int = 0,
    @SerialName(BlaseballSnacks.HOT_DOG)
    val hotDog: Int = 0,
    @SerialName(BlaseballSnacks.CHIPS)
    val chips: Int = 0,
    @SerialName(BlaseballSnacks.BURGER)
    val burger: Int = 0,
    @SerialName(BlaseballSnacks.VOTES)
    val votes: Int = 0,
    @SerialName(BlaseballSnacks.SNAKE_OIL)
    val snakeOil: Int = 0,
    @SerialName(BlaseballSnacks.POPCORN)
    val popcorn: Int = 0,
    @SerialName(BlaseballSnacks.STALE_POPCORN)
    val stalePopcorn: Int = 0,
    @SerialName(BlaseballSnacks.TAROT_SPREAD)
    val tarotSpread: Int = 0,
    @SerialName(BlaseballSnacks.PIZZA)
    val pizza: Int = 0,
    @SerialName(BlaseballSnacks.CHEESEBOARD)
    val cheeseBoard: Int = 0,
    @SerialName(BlaseballSnacks.APPLE)
    val apple: Int = 0,
    @SerialName(BlaseballSnacks.FLUTES)
    val flutes: Int = 0,
    @SerialName(BlaseballSnacks.PEANUTS)
    val peanuts: Int = 0,
    @SerialName(BlaseballSnacks.BREAD_CRUMBS)
    val breadCrumbs: Int = 0,
    @SerialName("Tarot_Reroll")
    val Tarot_Reroll: Int = 0,
    @SerialName(BlaseballSnacks.SLUSHIE)
    val slushie: Int = 0,
    @SerialName(BlaseballSnacks.WET_PRETZEL)
    val wetPretzel: Int = 0,
    @SerialName(BlaseballSnacks.SUNDAE)
    val sundae: Int = 0,
    @SerialName(BlaseballSnacks.PICKLES)
    val pickles: Int = 0,
    @SerialName(BlaseballSnacks.HOT_FRIES)
    val hotFries: Int = 0,
    @SerialName(BlaseballSnacks.MEATBALL)
    val meatball: Int = 0,
    @SerialName(BlaseballSnacks.TAFFY)
    val taffy: Int = 0,
    @SerialName(BlaseballSnacks.LEMONADE)
    val lemonade: Int = 0,
    @SerialName(BlaseballSnacks.DOUGHNUT)
    val doughnut: Int = 0,
    @SerialName(BlaseballSnacks.BREAKFAST)
    val breakfast: Int = 0,
//    @SerialName(BlaseballSnacks.SLOT)
//    val slot: Int = 0
)

@Serializable
data class BlaseballFanTrackersPayload(
    @SerialName("BEGS")
    val begs: Int,
    @SerialName("BETS")
    val bets: Int,
    @SerialName("VOTES_CAST")
    val votesCast: Int,
    @SerialName("SNACKS_BOUGHT")
    val snacksBought: Int,
    @SerialName("SNACK_UPGRADES")
    val snackUpgrades: Int
)