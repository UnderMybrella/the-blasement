package dev.brella.blasement.common.events

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.PatternDateFormat
import com.soywiz.klock.parse

data class TimeRange(val start: DateTimeTz?, val end: DateTimeTz?) {
    companion object {
        val ANY = TimeRange(null, null)
        val CHRONICLER_PATTERN = PatternDateFormat("yyyy-MM-dd'T'HH:mm:ss[.SS[S[SSS]]]Z", options = PatternDateFormat.Options.WITH_OPTIONAL)

        fun fromChronicler(from: String?, to: String?) =
            TimeRange(from?.let(CHRONICLER_PATTERN::parse), to?.let(CHRONICLER_PATTERN::parse))
    }

    operator fun contains(time: DateTimeTz): Boolean =
        (start == null || time >= start) && (end == null || time <= end)
}

infix operator fun DateTimeTz?.rangeTo(other: DateTimeTz?) =
    TimeRange(this, other)
