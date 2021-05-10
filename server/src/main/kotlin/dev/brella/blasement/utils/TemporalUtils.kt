package dev.brella.blasement.utils

import kotlin.time.ExperimentalTime
import kotlin.time.Duration as KDuration

import com.soywiz.klock.hours as klockHours

@OptIn(ExperimentalTime::class)
inline val Int.seconds get() = KDuration.seconds(this)

inline val Int.klockHours get() = klockHours