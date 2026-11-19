package com.example.prtv.data

/** Parses ISO-8601 durations as returned by YouTube (e.g. PT1H2M3S, PT45S, P1DT2H). */
object DurationParser {
    private val regex = Regex("^P(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?$")

    fun parseSeconds(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        val m = regex.matchEntire(iso.trim()) ?: return 0L
        fun g(i: Int): Long = m.groupValues[i].toLongOrNull() ?: 0L
        return g(1) * 86_400L + g(2) * 3_600L + g(3) * 60L + g(4)
    }
}
