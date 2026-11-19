package com.example.prtv.domain

import com.example.prtv.data.Video

/**
 * Deterministic "TV schedule". Every device that has the same video list and the same (server-corrected)
 * time computes the same programme, so everyone tuning in sees the same video at the same offset.
 *
 * How it works
 *  - The playable pool is sorted by video ID (API ordering never matters).
 *  - For each UTC day a lineup is produced by a seeded shuffle (seed = channel + playlist + day number),
 *    and that lineup repeats back-to-back for the whole day.
 *  - "Now" is a position inside the current day's lineup. At 00:00 UTC a new lineup starts at its first
 *    video; nothing ever jumps in the middle of a day.
 *
 * This is a scheduled replay, not a live broadcast, and the UI says so.
 */
object BroadcastScheduler {
    const val DAY_MS = 86_400_000L

    data class NowPlaying(
        val video: Video,
        /** Seconds into the video at the moment [nowPlaying] was evaluated. */
        val offsetSeconds: Long,
        val startedAtMs: Long,
        /** End of this slot: the end of the video, or midnight UTC if that comes first. */
        val endsAtMs: Long
    )

    data class Slot(val video: Video, val startMs: Long, val endMs: Long)

    fun isPlayable(v: Video): Boolean =
        v.embeddable &&
            v.privacyStatus == "public" &&
            v.liveBroadcastContent == "none" &&
            v.durationSeconds > 0

    fun lineupForDay(
        videos: List<Video>,
        seedKey: String,
        dayIndex: Long,
        excluded: Set<String> = emptySet()
    ): List<Video> {
        val list = videos
            .filter { isPlayable(it) && it.id !in excluded }
            .distinctBy { it.id }
            .sortedBy { it.id }
            .toMutableList()
        val rng = SplitMix64(fnv1a64(seedKey) xor (dayIndex * GOLDEN))
        for (i in list.size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }

    fun nowPlaying(
        videos: List<Video>,
        seedKey: String,
        nowMs: Long,
        excluded: Set<String> = emptySet()
    ): NowPlaying? {
        val dayIndex = Math.floorDiv(nowMs, DAY_MS)
        val dayStart = dayIndex * DAY_MS
        val lineup = lineupForDay(videos, seedKey, dayIndex, excluded)
        if (lineup.isEmpty()) return null

        val total = lineup.sumOf { it.durationSeconds }
        val secondsIntoDay = (nowMs - dayStart) / 1000
        val cyclePos = secondsIntoDay % total
        val cycleStart = secondsIntoDay - cyclePos

        var acc = 0L
        for (v in lineup) {
            if (cyclePos < acc + v.durationSeconds) {
                val startSec = cycleStart + acc
                val startedAt = dayStart + startSec * 1000
                return NowPlaying(
                    video = v,
                    offsetSeconds = secondsIntoDay - startSec,
                    startedAtMs = startedAt,
                    endsAtMs = minOf(startedAt + v.durationSeconds * 1000, dayStart + DAY_MS)
                )
            }
            acc += v.durationSeconds
        }
        return null
    }

    /** The slot containing [fromMs] followed by the next ones, [count] slots in total. */
    fun schedule(
        videos: List<Video>,
        seedKey: String,
        fromMs: Long,
        count: Int,
        excluded: Set<String> = emptySet()
    ): List<Slot> {
        val out = ArrayList<Slot>()
        var t = fromMs
        for (n in 0 until count) {
            val np = nowPlaying(videos, seedKey, t, excluded) ?: break
            out.add(Slot(np.video, np.startedAtMs, np.endsAtMs))
            t = np.endsAtMs
        }
        return out
    }

    // ---- deterministic hashing / PRNG (fixed algorithms so every device agrees) ----

    private const val GOLDEN = -7046029254386353131L        // 0x9E3779B97F4A7C15
    private const val FNV_OFFSET = -3750763034362895579L    // 0xCBF29CE484222325
    private const val FNV_PRIME = 1099511628211L

    private fun fnv1a64(s: String): Long {
        var h = FNV_OFFSET
        for (b in s.toByteArray(Charsets.UTF_8)) {
            h = h xor (b.toLong() and 0xFF)
            h *= FNV_PRIME
        }
        return h
    }

    private class SplitMix64(seed: Long) {
        private var state = seed

        fun nextLong(): Long {
            state += GOLDEN
            var z = state
            z = (z xor (z ushr 30)) * -4658895280553007687L    // 0xBF58476D1CE4E5B9
            z = (z xor (z ushr 27)) * -7723592293110705685L    // 0x94D049BB133111EB
            return z xor (z ushr 31)
        }

        fun nextInt(bound: Int): Int = ((nextLong() ushr 1) % bound).toInt()
    }
}
