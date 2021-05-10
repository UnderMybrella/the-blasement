package dev.brella.blasement.utils

inline operator fun <K, V> Map<K, V>?.get(key: K): V? = this?.get(key)