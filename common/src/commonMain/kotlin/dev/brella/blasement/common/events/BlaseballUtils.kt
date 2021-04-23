package dev.brella.blasement.common.events

import dev.brella.kornea.blaseball.base.common.BlaseballFeedEventType

fun BlaseballFeedEventType.selectEventList(selector: BlaseballFeedEventType.() -> IntArray): IntArray =
    BlaseballFeedEventType.selector()

fun BlaseballFeedEventType.buildEventList(selector: BlaseballFeedEventType.(list: MutableList<Int>) -> Unit): List<Int> =
    ArrayList<Int>().apply { BlaseballFeedEventType.selector(this) }

//class BlaseballFeedEventTypeProgressionAsRange : IntIterator(), ClosedRange<Int> {
//    companion object : Iterable<Int> {
//        val RANGES = BlaseballFeedEventType.run {
//            listOf(
//                PLAY_BALL..FOUL_BALL,
//                PLAYER_SKIPPED_DUE_TO_SHELL..STRIKE_ZAPPED_BY_ELECTRIC_BLOOD,
//                MILD_PITCH..SUN_2,
//                BIRDS_FLAVOUR_TEXT..MURDER_OF_CROWS,
//                TRIPLE_THREAD..FREE_REFILL,
//                WIRED..FEEDBACK,
//                ALLERGIC_REACTION..REVERB_SHUFFLE,
//                SIPHON..SIPHON,
//                INCINERATION..FLAG_PLANTED,
//                DECREE_PASSED..FLOOD,
//                PEANUTS_FLAVOUR_TEXT..TASTING_THE_INFINITE,
//                TAROT_READING..EMERGENCY_ALERT,
//                RETURN_FROM_ELSEWHERE..UNDER_OVER,
//                SUPERYUMMY_TEXT..PERK,
//                ADDED_INGAME_MODIFIER..PLAYER_RECRUITED,
//                PLAYER_TRADE..PLAYER_REROLL,
//                PLAYER_ENTERS_THE_HALL_OF_FAME..PLAYER_ENTERS_THE_HALL_OF_FAME,
//                REVERB_SHUFFLE_2..REVERB_SHUFFLE_2,
//                NEW_TEAM..PLAYER_HATCHED,
//                PLAYER_EVOLVES..PLAYER_EVOLVES,
//                TEAM_WINS_INTERNET_SERIES..SUPERYUMMY_TRANSITIONS,
//                RETURNED_VALUE_PERMITTED_TO_STAY..WILL_RESULTS,
//                TEAM_SHAMED..POSTSEASON_ADVANCE
//            )
//        }
//
//        override fun iterator(): Iterator<Int> = BlaseballFeedEventTypeProgressionAsRange()
//
//        operator fun contains(type: Int): Boolean =
//            RANGES.any { type in it }
//    }
//
//    override val start: Int = RANGES.first().first
//    override val endInclusive: Int = RANGES.last().last
//
//    private var ranges = RANGES.iterator()
//    private var range = ranges.next().iterator()
//
//    private var hasNext: Boolean = range.hasNext()
//    private var next = range.next()
//
//    override fun hasNext(): Boolean = hasNext
//
//    override fun nextInt(): Int {
//        val value = next
//        when {
//            range.hasNext() -> next = range.next()
//            ranges.hasNext() -> range = ranges.next().iterator()
//            else -> {
//                if (!hasNext) throw kotlin.NoSuchElementException()
//                hasNext = false
//            }
//        }
//
//        return value
//    }
//}