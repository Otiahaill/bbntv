package com.example.prtv.data

import com.example.prtv.domain.BroadcastScheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LineupRepository @Inject constructor(
    private val api: YouTubeApi,
    private val cache: LineupCache,
    private val prefs: AppPrefs
) {
    /** Cached lineup, or null if none / older than the retention limit (then it is deleted). */
    fun cached(): Lineup? = cache.read(ClockSync.nowMs())

    fun isStale(lineup: Lineup): Boolean = ClockSync.nowMs() - lineup.fetchedAtMs > REFRESH_AFTER_MS

    /** Fetches the channel (or a chosen playlist), validates it, caches it and remembers the inputs. */
    suspend fun refresh(channelInput: String, playlistInput: String?): Lineup {
        val channel = api.resolveChannel(channelInput)
        val playlistId = playlistInput?.trim().orEmpty()
            .takeIf { it.isNotEmpty() }
            ?.let { extractPlaylistId(it) }
            ?: channel.uploadsPlaylistId

        val ids = api.playlistVideoIds(playlistId)
        val videos = api.videos(ids)
        if (videos.none { BroadcastScheduler.isPlayable(it) }) throw ApiException.NoPlayableVideos()

        val lineup = Lineup(
            channelId = channel.id,
            channelTitle = channel.title,
            playlistId = playlistId,
            videos = videos,
            fetchedAtMs = ClockSync.nowMs()
        )
        cache.write(lineup)
        prefs.channelInput = channelInput.trim()
        prefs.playlistInput = playlistInput?.trim().orEmpty()
        return lineup
    }

    /** Cheap, quota-free request whose Date header corrects the schedule clock. Failures are ignored. */
    suspend fun syncClock() = api.syncClock()

    fun clear() = cache.clear()

    private fun extractPlaylistId(raw: String): String =
        Regex("[?&]list=([A-Za-z0-9_-]+)").find(raw)?.groupValues?.get(1) ?: raw

    private companion object {
        const val REFRESH_AFTER_MS = 24L * 60 * 60 * 1000
    }
}
