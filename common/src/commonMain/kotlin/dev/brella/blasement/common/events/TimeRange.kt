package dev.brella.blasement.common.events

import com.soywiz.klock.DateTimeTz

data class TimeRange(val start: DateTimeTz?, val end: DateTimeTz?) {
    companion object {
        val ANY = TimeRange(null, null)
    }

    operator fun contains(time: DateTimeTz): Boolean =
        (start == null || time >= start) && (end == null || time <= end)
}

infix operator fun DateTimeTz?.rangeTo(other: DateTimeTz?) =
    TimeRange(this, other)