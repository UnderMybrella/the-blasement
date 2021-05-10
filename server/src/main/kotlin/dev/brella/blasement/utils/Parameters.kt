package dev.brella.blasement.utils

import io.ktor.http.*
import io.ktor.util.*

object ParametersComparator : Comparator<Pair<String, String>> {
    override fun compare(o1: Pair<String, String>, o2: Pair<String, String>): Int {
        val a = o1.first.compareTo(o2.first)
        if (a != 0) return a

        return o1.second.compareTo(o2.second)
    }
}

inline fun Parameters.toStableString(): String =
    flattenEntries().sortedWith(ParametersComparator)
        .joinToString("&") { (k, v) -> "$k=$v" }