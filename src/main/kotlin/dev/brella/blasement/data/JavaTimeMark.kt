package dev.brella.blasement.data

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark

@OptIn(ExperimentalTime::class)
class JavaTimeMark(val clock: java.time.Clock, val since: Long): TimeMark() {
    override fun elapsedNow(): Duration =
        Duration.milliseconds(clock.millis() - since)
}