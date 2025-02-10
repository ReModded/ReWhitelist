package dev.remodded.rewhitelist.utils

import kotlin.math.absoluteValue

fun Number.ordinal(): String {
    val value = toLong().absoluteValue
    return "$this" + if (value % 100 in 11..13) "th" else when(value % 10) {
        1L -> "st"
        2L -> "nd"
        3L -> "rd"
        else -> "th"
    }
}
