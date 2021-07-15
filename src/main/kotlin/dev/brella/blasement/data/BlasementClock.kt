package dev.brella.blasement.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.asTimeSource
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

interface BlasementClock {
    data class Static(val time: Instant) : BlasementClock {
        @OptIn(ExperimentalTime::class)
        override val temporalUpdateTime: Duration
            get() = Duration.INFINITE

        @OptIn(ExperimentalTime::class)
        override val eventStreamUpdateTime: Duration
            get() = Duration.INFINITE

        override suspend fun getTime(): Instant = time
    }

    @OptIn(ExperimentalTime::class)
    data class UnboundedFrom(
        val start: Instant,
        val base: TimeMark,
        val accelerationRate: Double = 1.0,
        override val temporalUpdateTime: Duration = Duration.seconds(1),
        override val eventStreamUpdateTime: Duration = Duration.seconds(4)
    ) : BlasementClock {
        constructor(
            start: Instant,
            clock: Clock,
            accelerationRate: Double = 1.0,
            temporalUpdateTime: Duration = Duration.seconds(1),
            eventStreamUpdateTime: Duration = Duration.seconds(4)
        ): this(start, clock.asTimeSource(), accelerationRate, temporalUpdateTime, eventStreamUpdateTime)

        constructor(
            start: Instant,
            timeSource: TimeSource,
            accelerationRate: Double = 1.0,
            temporalUpdateTime: Duration = Duration.seconds(1),
            eventStreamUpdateTime: Duration = Duration.seconds(4)
        ): this(start, timeSource.markNow(), accelerationRate, temporalUpdateTime, eventStreamUpdateTime)

        constructor(
            clock: Clock,
            accelerationRate: Double = 1.0,
            temporalUpdateTime: Duration = Duration.seconds(1),
            eventStreamUpdateTime: Duration = Duration.seconds(4)
        ): this(clock.now(), clock.asTimeSource(), accelerationRate, temporalUpdateTime, eventStreamUpdateTime)

        constructor(
            start: Instant,
            accelerationRate: Double = 1.0,
            temporalUpdateTime: Duration = Duration.seconds(1),
            eventStreamUpdateTime: Duration = Duration.seconds(4)
        ): this(start, TimeSource.Monotonic, accelerationRate, temporalUpdateTime, eventStreamUpdateTime)

        override suspend fun getTime(): Instant = start + (base.elapsedNow() * accelerationRate)
    }

    @OptIn(ExperimentalTime::class)
    val temporalUpdateTime: Duration

    @OptIn(ExperimentalTime::class)
    val eventStreamUpdateTime: Duration

    suspend fun getTime(): Instant
}