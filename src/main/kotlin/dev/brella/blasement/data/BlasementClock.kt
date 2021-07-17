package dev.brella.blasement.data

import dev.brella.blasement.getDoubleOrNull
import dev.brella.blasement.getLongOrNull
import dev.brella.blasement.getStringOrNull
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.successPooled
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.asTimeSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.ZoneId
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import java.time.Instant as jtInstant
import java.time.OffsetDateTime as jtOffsetDateTime
import java.time.Clock as jtClock

interface BlasementClock {
    data class Static(val time: Instant) : BlasementClock {
        @OptIn(ExperimentalTime::class)
        override val temporalUpdateTime: Duration
            get() = Duration.INFINITE

        @OptIn(ExperimentalTime::class)
        override val eventStreamUpdateTime: Duration
            get() = Duration.INFINITE

        override suspend fun getTime(): Instant = time
        override fun describe(): JsonObject =
            buildJsonObject {
                put("type", "static")
                put("time", time.toString())
            }
    }

    @OptIn(ExperimentalTime::class)
    data class UnboundedFrom(
        val start: Instant,
        val base: TimeMark,
        val accelerationRate: Double = 1.0,
        override val temporalUpdateTime: Duration = Duration.seconds(1),
        override val eventStreamUpdateTime: Duration = Duration.seconds(4)
    ) : BlasementClock {
/*
        constructor(
            start: Instant,
            clock: Clock,
            accelerationRate: Double = 1.0,
            temporalUpdateTime: Duration = Duration.seconds(1),
            eventStreamUpdateTime: Duration = Duration.seconds(4)
        ) : this(start, clock.asTimeSource(), accelerationRate, temporalUpdateTime, eventStreamUpdateTime)
*/

        constructor(
            start: Instant,
            timeSource: TimeSource,
            accelerationRate: Double = 1.0,
            temporalUpdateTime: Duration = Duration.seconds(1),
            eventStreamUpdateTime: Duration = Duration.seconds(4)
        ) : this(start, timeSource.markNow(), accelerationRate, temporalUpdateTime, eventStreamUpdateTime)

/*        constructor(
            clock: Clock,
            accelerationRate: Double = 1.0,
            temporalUpdateTime: Duration = Duration.seconds(1),
            eventStreamUpdateTime: Duration = Duration.seconds(4)
        ) : this(clock.now(), clock.asTimeSource(), accelerationRate, temporalUpdateTime, eventStreamUpdateTime)

        constructor(
            start: Instant,
            accelerationRate: Double = 1.0,
            temporalUpdateTime: Duration = Duration.seconds(1),
            eventStreamUpdateTime: Duration = Duration.seconds(4)
        ) : this(start, TimeSource.Monotonic, accelerationRate, temporalUpdateTime, eventStreamUpdateTime)*/

        override suspend fun getTime(): Instant = start + (base.elapsedNow() * accelerationRate)

        override fun describe(): JsonObject =
            buildJsonObject {
                put("type", "unbounded")
                put("time", start.toString())
                put("acceleration_rate", accelerationRate)
                put("temporal_update_time", temporalUpdateTime.toDouble(DurationUnit.SECONDS))
                put("event_stream_update_time", eventStreamUpdateTime.toDouble(DurationUnit.SECONDS))
            }
    }

    @OptIn(ExperimentalTime::class)
    data class BasedOnJavaClock(
        val clock: jtClock,
        override val temporalUpdateTime: Duration = Duration.seconds(1),
        override val eventStreamUpdateTime: Duration = Duration.seconds(4)
    ) : BlasementClock {
        companion object {
            inline fun systemUTC(
                temporalUpdateTime: Duration = Duration.seconds(1),
                eventStreamUpdateTime: Duration = Duration.seconds(4)
            ) = BasedOnJavaClock(jtClock.systemUTC(), temporalUpdateTime, eventStreamUpdateTime)
        }

        override suspend fun getTime(): Instant = clock.instant().let {
            Instant.fromEpochSeconds(it.epochSecond, it.nano)
        }

        override fun describe(): JsonObject =
            buildJsonObject {
                put("type", "clock")
                put("zone", clock.zone.id)
                put("temporal_update_time", temporalUpdateTime.toDouble(DurationUnit.SECONDS))
                put("event_stream_update_time", eventStreamUpdateTime.toDouble(DurationUnit.SECONDS))
            }
    }

    @OptIn(ExperimentalTime::class)
    val temporalUpdateTime: Duration

    @OptIn(ExperimentalTime::class)
    val eventStreamUpdateTime: Duration

    suspend fun getTime(): Instant
    fun describe(): JsonElement?

    companion object {
        @OptIn(ExperimentalTime::class)
        fun loadFrom(clockJson: JsonElement?, createdAt: Long, utc: jtClock): KorneaResult<BlasementClock> {
            return KorneaResult.successPooled(
                when (clockJson) {
                    is JsonPrimitive -> {
                        val clock = clockJson.content

                        if (clock.equals("utc", true)) BasedOnJavaClock(utc)
                        else {
                            val parsed = runCatching { Instant.parse(clock) }
                            if (parsed.isSuccess) UnboundedFrom(parsed.getOrThrow(), JavaTimeMark(utc, createdAt))
                            else return KorneaResult.errorAsIllegalArgument(-1, "Could not parse clock string '$clock' (${parsed.exceptionOrNull()?.stackTraceToString()})")
                        }
                    }
                    is JsonObject -> {
                        when (val type = clockJson.getStringOrNull("type")?.lowercase(Locale.getDefault())) {
                            "static" -> {
                                val time = runCatching { Instant.parse(clockJson.getStringOrNull("time") ?: return KorneaResult.errorAsIllegalArgument(-1, "No time for static clock")) }
                                if (time.isSuccess) Static(time.getOrThrow())
                                else return KorneaResult.errorAsIllegalArgument(-1, "Could not parse clock string '$clockJson' (${time.exceptionOrNull()?.stackTraceToString()})")
                            }
                            "unbounded", "unbounded from", "unbounded_from", "unboundedFrom" -> {
                                val time = runCatching { Instant.parse(clockJson.getStringOrNull("time") ?: return KorneaResult.errorAsIllegalArgument(-1, "No time for unbound clock")) }
                                if (time.isFailure) return KorneaResult.errorAsIllegalArgument(-1, "Could not parse clock string '$clockJson' (${time.exceptionOrNull()?.stackTraceToString()})")

                                val accelerationRate = clockJson.getDoubleOrNull("acceleration_rate", "accelerationRate", "acceleration rate")
                                                       ?: 1.0

                                val temporalUpdateTime = clockJson.getDoubleOrNull(
                                    "temporal_update_time",
                                    "temporal_update_rate",
                                    "temporal_update",
                                    "temporal update time",
                                    "temporal update rate",
                                    "temporal update",
                                    "temporalUpdateTime",
                                    "temporalUpdateRate",
                                    "temporalUpdate"
                                )?.let(Duration::seconds) ?: clockJson.getDoubleOrNull(
                                    "temporal_update_time_ms",
                                    "temporal_update_rate_ms",
                                    "temporal_update_ms",
                                    "temporal update time ms",
                                    "temporal update rate ms",
                                    "temporal update ms",
                                    "temporalUpdateTimeMs",
                                    "temporalUpdateRateMs",
                                    "temporalUpdateMs"
                                )?.let(Duration::milliseconds) ?: Duration.seconds(1)

                                val eventStreamUpdateTime = clockJson.getDoubleOrNull(
                                    "event_stream_update_time",
                                    "event_stream_update_rate",
                                    "event_stream_update",
                                    "event stream update time",
                                    "event stream update rate",
                                    "event stream update",
                                    "eventStreamUpdateTime",
                                    "eventStreamUpdateRate",
                                    "eventStreamUpdate"
                                )?.let(Duration::seconds) ?: clockJson.getDoubleOrNull(
                                    "event_stream_update_time_ms",
                                    "event_stream_update_rate_ms",
                                    "event_stream_update_ms",
                                    "event stream update time ms",
                                    "event stream update rate ms",
                                    "event stream update ms",
                                    "eventStreamUpdateTimeMs",
                                    "eventStreamUpdateRateMs",
                                    "eventStreamUpdateMs"
                                )?.let(Duration::milliseconds) ?: Duration.seconds(4)

                                UnboundedFrom(time.getOrThrow(), JavaTimeMark(utc, clockJson.getLongOrNull("start") ?: createdAt), accelerationRate, temporalUpdateTime, eventStreamUpdateTime)
                            }
                            "clock", "java clock", "java_clock", "javaClock" -> {
                                val clockInstance = when (val zoneString = clockJson.getStringOrNull("zone")) {
                                    "utc", "UTC", "Z", null -> java.time.Clock.systemUTC()
                                    "systemDefaultZone", "systemDefault", "system", "system_default_zone", "system_default", "system default zone", "system default" -> java.time.Clock.systemDefaultZone()
                                    else -> {
                                        val zone = runCatching { ZoneId.of(zoneString) }
                                        if (zone.isFailure) return KorneaResult.errorAsIllegalArgument(-1, "Could not parse zone string '$zoneString': ${zone.exceptionOrNull()?.stackTraceToString()}")
                                        java.time.Clock.system(zone.getOrThrow())
                                    }
                                }

                                val temporalUpdateTime = clockJson.getDoubleOrNull(
                                    "temporal_update_time",
                                    "temporal_update_rate",
                                    "temporal_update",
                                    "temporal update time",
                                    "temporal update rate",
                                    "temporal update",
                                    "temporalUpdateTime",
                                    "temporalUpdateRate",
                                    "temporalUpdate"
                                )?.let(Duration::seconds) ?: clockJson.getDoubleOrNull(
                                    "temporal_update_time_ms",
                                    "temporal_update_rate_ms",
                                    "temporal_update_ms",
                                    "temporal update time ms",
                                    "temporal update rate ms",
                                    "temporal update ms",
                                    "temporalUpdateTimeMs",
                                    "temporalUpdateRateMs",
                                    "temporalUpdateMs"
                                )?.let(Duration::milliseconds) ?: Duration.seconds(1)

                                val eventStreamUpdateTime = clockJson.getDoubleOrNull(
                                    "event_stream_update_time",
                                    "event_stream_update_rate",
                                    "event_stream_update",
                                    "event stream update time",
                                    "event stream update rate",
                                    "event stream update",
                                    "eventStreamUpdateTime",
                                    "eventStreamUpdateRate",
                                    "eventStreamUpdate"
                                )?.let(Duration::seconds) ?: clockJson.getDoubleOrNull(
                                    "event_stream_update_time_ms",
                                    "event_stream_update_rate_ms",
                                    "event_stream_update_ms",
                                    "event stream update time ms",
                                    "event stream update rate ms",
                                    "event stream update ms",
                                    "eventStreamUpdateTimeMs",
                                    "eventStreamUpdateRateMs",
                                    "eventStreamUpdateMs"
                                )?.let(Duration::milliseconds) ?: Duration.seconds(4)

                                BasedOnJavaClock(clockInstance, temporalUpdateTime, eventStreamUpdateTime)
                            }
                            else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown clock type '$type'")
                        }
                    }
                    else -> return KorneaResult.errorAsIllegalArgument(-1, "Unknown clock object '$clockJson'")
                }
            )
        }
    }
}