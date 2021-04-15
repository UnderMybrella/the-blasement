inline class BlaseballDate(val data: Int) {
    constructor(season: Int, day: Int): this((season shl 8) or day)

    val season: Int
        inline get() = (data shr 8) and 0xFF

    val day: Int
        inline get() = (data and 0xFF)

    val bettable: Boolean
        get() = day > 0

    operator fun component1(): Int = season
    operator fun component2(): Int = day
}