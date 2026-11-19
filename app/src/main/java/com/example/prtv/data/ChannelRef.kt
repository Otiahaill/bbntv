package com.example.prtv.data

/** What the user typed to identify a channel: a UC… channel ID or an @handle. */
sealed class ChannelRef {
    data class Id(val value: String) : ChannelRef()
    data class Handle(val value: String) : ChannelRef()

    companion object {
        private val idRegex = Regex("^UC[0-9A-Za-z_-]{22}$")
        private val idInUrl = Regex("channel/(UC[0-9A-Za-z_-]{22})")
        private val bareName = Regex("^[A-Za-z0-9._-]{3,30}$")

        fun parse(raw: String): ChannelRef? {
            val s = raw.trim()
            if (s.isEmpty()) return null
            idInUrl.find(s)?.let { return Id(it.groupValues[1]) }
            if (idRegex.matches(s)) return Id(s)
            val at = s.indexOf('@')
            if (at >= 0) {
                val handle = s.substring(at).takeWhile { it !in "/?#& " }
                return if (handle.length > 1) Handle(handle) else null
            }
            if (bareName.matches(s)) return Handle("@$s")
            return null
        }
    }
}
