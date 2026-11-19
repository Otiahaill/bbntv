package com.example.prtv.domain

import com.example.prtv.data.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BroadcastSchedulerTest {

    private fun video(
        id: String,
        seconds: Long,
        embeddable: Boolean = true,
        privacy: String = "public",
        live: String = "none"
    ) = Video(id, "Title $id", null, seconds, embeddable, privacy, live, madeForKids = false)

    // 600 + 900 + 300 = 1800 s, which divides a 86 400 s day exactly (48 cycles).
    private val pool = listOf(video("aaa", 600), video("bbb", 900), video("ccc", 300))
    private val seed = "channel:playlist"
    private val day = 20_000L
    private val dayStart = day * BroadcastScheduler.DAY_MS

    @Test
    fun orderOfInputDoesNotMatter() {
        val a = BroadcastScheduler.lineupForDay(pool, seed, day).map { it.id }
        val b = BroadcastScheduler.lineupForDay(pool.reversed(), seed, day).map { it.id }
        assertEquals(a, b)
    }

    @Test
    fun sameInputsSameResult() {
        val t = dayStart + 12_345_000L
        assertEquals(
            BroadcastScheduler.nowPlaying(pool, seed, t),
            BroadcastScheduler.nowPlaying(pool, seed, t)
        )
    }

    @Test
    fun differentDaysCanShuffleDifferently() {
        val orders = (day..day + 10).map { d ->
            BroadcastScheduler.lineupForDay(pool, seed, d).map { it.id }
        }.toSet()
        assertTrue("expected more than one distinct order over 11 days", orders.size > 1)
    }

    @Test
    fun dayStartPlaysFirstVideoFromZero() {
        val lineup = BroadcastScheduler.lineupForDay(pool, seed, day)
        val np = BroadcastScheduler.nowPlaying(pool, seed, dayStart)!!
        assertEquals(lineup[0].id, np.video.id)
        assertEquals(0L, np.offsetSeconds)
        assertEquals(dayStart, np.startedAtMs)
    }

    @Test
    fun offsetsAcrossVideoBoundary() {
        val lineup = BroadcastScheduler.lineupForDay(pool, seed, day)
        val first = lineup[0].durationSeconds
        val np = BroadcastScheduler.nowPlaying(pool, seed, dayStart + (first + 5) * 1000)!!
        assertEquals(lineup[1].id, np.video.id)
        assertEquals(5L, np.offsetSeconds)
        assertEquals(dayStart + first * 1000, np.startedAtMs)
    }

    @Test
    fun lineupRepeatsWithinTheDay() {
        val a = BroadcastScheduler.nowPlaying(pool, seed, dayStart + 10_000L)!!
        val b = BroadcastScheduler.nowPlaying(pool, seed, dayStart + 1_810_000L)!!   // +1800 s
        assertEquals(a.video.id, b.video.id)
        assertEquals(a.offsetSeconds, b.offsetSeconds)
    }

    @Test
    fun ineligibleVideosAreSkipped() {
        val mixed = pool + listOf(
            video("ddd", 100, embeddable = false),
            video("eee", 100, privacy = "private"),
            video("fff", 100, live = "live"),
            video("ggg", 0)
        )
        val ids = BroadcastScheduler.lineupForDay(mixed, seed, day).map { it.id }.toSet()
        assertEquals(setOf("aaa", "bbb", "ccc"), ids)
    }

    @Test
    fun emptyPoolGivesNull() {
        assertNull(BroadcastScheduler.nowPlaying(emptyList(), seed, dayStart))
        assertNull(BroadcastScheduler.nowPlaying(listOf(video("x", 60, embeddable = false)), seed, dayStart))
    }

    @Test
    fun excludedVideoIsNeverScheduled() {
        val ids = BroadcastScheduler.lineupForDay(pool, seed, day, excluded = setOf("bbb")).map { it.id }
        assertTrue("bbb" !in ids)
        assertEquals(2, ids.size)
    }

    @Test
    fun scheduleIsContiguousAndIncreasing() {
        val slots = BroadcastScheduler.schedule(pool, seed, dayStart + 100_000L, 6)
        assertEquals(6, slots.size)
        for (i in 1 until slots.size) {
            assertEquals(slots[i - 1].endMs, slots[i].startMs)
            assertTrue(slots[i].endMs > slots[i].startMs)
        }
    }

    @Test
    fun slotNeverRunsPastMidnight() {
        // 700 s videos do not divide the day, so the last slot of the day is cut at midnight.
        val odd = listOf(video("a", 700), video("b", 700))
        val nearEnd = dayStart + BroadcastScheduler.DAY_MS - 10_000L
        val np = BroadcastScheduler.nowPlaying(odd, seed, nearEnd)!!
        assertTrue(np.endsAtMs <= dayStart + BroadcastScheduler.DAY_MS)
        val next = BroadcastScheduler.nowPlaying(odd, seed, np.endsAtMs)!!
        assertEquals(dayStart + BroadcastScheduler.DAY_MS, next.startedAtMs)
        assertEquals(0L, next.offsetSeconds)
        assertNotEquals(np.startedAtMs, next.startedAtMs)
    }
}
