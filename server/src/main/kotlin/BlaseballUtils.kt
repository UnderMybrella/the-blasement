import dev.brella.kornea.blaseball.BlaseballFeedEventType

fun BlaseballFeedEventType.selectEventList(selector: BlaseballFeedEventType.() -> List<Int>): List<Int> =
    BlaseballFeedEventType.selector()

fun BlaseballFeedEventType.buildEventList(selector: BlaseballFeedEventType.(list: MutableList<Int>) -> Unit): List<Int> =
    ArrayList<Int>().apply { BlaseballFeedEventType.selector(this) }

class BlaseballFeedEventTypeProgression : IntIterator(), ClosedRange<Int> {
    companion object : Iterable<Int> {
        val RANGES = BlaseballFeedEventType.run {
            listOf(
                PLAY_BALL..FOUL_BALL,
                PLAYER_SKIPPED_DUE_TO_SHELL..STRIKE_ZAPPED_BY_ELECTRIC_BLOOD,
                MILD_PITCH..SUN_2,
                BIRDS_FLAVOUR_TEXT..MURDER_OF_CROWS,
                TRIPLE_THREAD..FREE_REFILL,
                WIRED..FEEDBACK,
                ALLERGIC_REACTION..REVERB_SHUFFLE,
                SIPHON..SIPHON,
                INCINERATION..FLAG_PLANTED,
                DECREE_PASSED..FLOOD,
                PEANUTS_FLAVOUR_TEXT..TASTING_THE_INFINITE,
                TAROT_READING..EMERGENCY_ALERT,
                RETURN_FROM_ELSEWHERE..UNDER_OVER,
                SUPERYUMMY_TEXT..PERK,
                ADDED_INGAME_MODIFIER..PLAYER_RECRUITED,
                PLAYER_TRADE..PLAYER_REROLL,
                PLAYER_ENTERS_THE_HALL_OF_FAME..PLAYER_ENTERS_THE_HALL_OF_FAME,
                REVERB_SHUFFLE_2..REVERB_SHUFFLE_2,
                NEW_TEAM..PLAYER_HATCHED,
                PLAYER_EVOLVES..PLAYER_EVOLVES,
                TEAM_WINS_INTERNET_SERIES..SUPERYUMMY_TRANSITIONS,
                RETURNED_VALUE_PERMITTED_TO_STAY..WILL_RESULTS,
                TEAM_SHAMED..POSTSEASON_ADVANCE
            )
        }

        override fun iterator(): Iterator<Int> = BlaseballFeedEventTypeProgression()
    }

    override val start: Int = RANGES.first().first
    override val endInclusive: Int = RANGES.last().last

    private var ranges = RANGES.iterator()
    private var range = ranges.next().iterator()

    private var hasNext: Boolean = range.hasNext()
    private var next = range.next()

    override fun hasNext(): Boolean = hasNext

    override fun nextInt(): Int {
        val value = next
        when {
            range.hasNext() -> next = range.next()
            ranges.hasNext() -> range = ranges.next().iterator()
            else -> {
                if (!hasNext) throw kotlin.NoSuchElementException()
                hasNext = false
            }
        }

        return value
    }
}

object BlaseballFeedEventTypeProgressionAsCopiedList : MutableIterable<Int>, ClosedRange<Int> {
    private val RANGE = BlaseballFeedEventType.run {
        listOf(
            PLAY_BALL..FOUL_BALL,
            PLAYER_SKIPPED_DUE_TO_SHELL..STRIKE_ZAPPED_BY_ELECTRIC_BLOOD,
            MILD_PITCH..SUN_2,
            BIRDS_FLAVOUR_TEXT..MURDER_OF_CROWS,
            TRIPLE_THREAD..FREE_REFILL,
            WIRED..FEEDBACK,
            ALLERGIC_REACTION..REVERB_SHUFFLE,
            SIPHON..SIPHON,
            INCINERATION..FLAG_PLANTED,
            DECREE_PASSED..FLOOD,
            PEANUTS_FLAVOUR_TEXT..TASTING_THE_INFINITE,
            TAROT_READING..EMERGENCY_ALERT,
            RETURN_FROM_ELSEWHERE..UNDER_OVER,
            SUPERYUMMY_TEXT..PERK,
            ADDED_INGAME_MODIFIER..PLAYER_RECRUITED,
            PLAYER_TRADE..PLAYER_REROLL,
            PLAYER_ENTERS_THE_HALL_OF_FAME..PLAYER_ENTERS_THE_HALL_OF_FAME,
            REVERB_SHUFFLE_2..REVERB_SHUFFLE_2,
            NEW_TEAM..PLAYER_HATCHED,
            PLAYER_EVOLVES..PLAYER_EVOLVES,
            TEAM_WINS_INTERNET_SERIES..SUPERYUMMY_TRANSITIONS,
            RETURNED_VALUE_PERMITTED_TO_STAY..WILL_RESULTS,
            TEAM_SHAMED..POSTSEASON_ADVANCE
        ).flatten()
    }

    override fun iterator(): MutableIterator<Int> = ArrayList(RANGE).listIterator()

    override val start: Int = RANGE.first()
    override val endInclusive: Int = RANGE.last()

    override fun contains(value: Int): Boolean = RANGE.contains(value)
}

object BlaseballFeedEventTypeProgressionAsNewList : MutableIterable<Int>, ClosedRange<Int> {
    inline fun create() =
        BlaseballFeedEventType.run {
            val list: MutableList<Int> = ArrayList(POSTSEASON_ADVANCE)

            list.addAll(PLAY_BALL..FOUL_BALL)
            list.addAll(PLAYER_SKIPPED_DUE_TO_SHELL..STRIKE_ZAPPED_BY_ELECTRIC_BLOOD)
            list.addAll(MILD_PITCH..SUN_2)
            list.addAll(BIRDS_FLAVOUR_TEXT..MURDER_OF_CROWS)
            list.addAll(TRIPLE_THREAD..FREE_REFILL)
            list.addAll(WIRED..FEEDBACK)
            list.addAll(ALLERGIC_REACTION..REVERB_SHUFFLE)
            list.addAll(SIPHON..SIPHON)
            list.addAll(INCINERATION..FLAG_PLANTED)
            list.addAll(DECREE_PASSED..FLOOD)
            list.addAll(PEANUTS_FLAVOUR_TEXT..TASTING_THE_INFINITE)
            list.addAll(TAROT_READING..EMERGENCY_ALERT)
            list.addAll(RETURN_FROM_ELSEWHERE..UNDER_OVER)
            list.addAll(SUPERYUMMY_TEXT..PERK)
            list.addAll(ADDED_INGAME_MODIFIER..PLAYER_RECRUITED)
            list.addAll(PLAYER_TRADE..PLAYER_REROLL)
            list.addAll(PLAYER_ENTERS_THE_HALL_OF_FAME..PLAYER_ENTERS_THE_HALL_OF_FAME)
            list.addAll(REVERB_SHUFFLE_2..REVERB_SHUFFLE_2)
            list.addAll(NEW_TEAM..PLAYER_HATCHED)
            list.addAll(PLAYER_EVOLVES..PLAYER_EVOLVES)
            list.addAll(TEAM_WINS_INTERNET_SERIES..SUPERYUMMY_TRANSITIONS)
            list.addAll(RETURNED_VALUE_PERMITTED_TO_STAY..WILL_RESULTS)
            list.addAll(TEAM_SHAMED..POSTSEASON_ADVANCE)

            list
        }

    override fun iterator(): MutableListIterator<Int> = create().listIterator()

    override val start: Int = BlaseballFeedEventType.PLAY_BALL
    override val endInclusive: Int = BlaseballFeedEventType.POSTSEASON_ADVANCE

    override fun contains(value: Int): Boolean = create().contains(value)
}