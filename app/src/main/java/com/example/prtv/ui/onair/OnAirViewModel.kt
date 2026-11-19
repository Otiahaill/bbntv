package com.example.prtv.ui.onair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prtv.data.ApiException
import com.example.prtv.data.AppPrefs
import com.example.prtv.data.BrandConfig
import com.example.prtv.data.ClockSync
import com.example.prtv.data.Lineup
import com.example.prtv.data.LineupRepository
import com.example.prtv.data.userMessage
import com.example.prtv.domain.BroadcastScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class OnAirUi(
    val loading: Boolean = true,
    val needsSetup: Boolean = false,
    val channelId: String = "",
    val channelTitle: String = "",
    val nowPlaying: BroadcastScheduler.NowPlaying? = null,
    val upcoming: List<BroadcastScheduler.Slot> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class OnAirViewModel @Inject constructor(
    private val repo: LineupRepository,
    private val prefs: AppPrefs,
    private val brand: BrandConfig
) : ViewModel() {

    private val _ui = MutableStateFlow(OnAirUi())
    val ui: StateFlow<OnAirUi> = _ui.asStateFlow()

    private var lineup: Lineup? = null
    private var pending: Lineup? = null          // fresher data, applied at the next UTC day change
    private var lineupDay: Long = 0L
    private val excluded = mutableSetOf<String>() // videos the player refused today
    private var tickJob: Job? = null

    init {
        viewModelScope.launch { start() }
    }

    private fun wantedChannel(): String = brand.channel.ifBlank { prefs.channelInput }
    private fun wantedPlaylist(): String = if (brand.channelIsFixed) brand.playlist else prefs.playlistInput

    private suspend fun start() {
        _ui.value = OnAirUi(loading = true)
        // Correct the clock first so the very first schedule matches every other device.
        withTimeoutOrNull(3_000L) { repo.syncClock() }

        var cached = withContext(Dispatchers.IO) { repo.cached() }
        // A rebranded build must not keep playing the previous channel's cached data.
        if (cached != null && brand.channelIsFixed &&
            (prefs.channelInput != brand.channel || prefs.playlistInput != brand.playlist)
        ) {
            withContext(Dispatchers.IO) { repo.clear() }
            cached = null
        }

        if (cached == null) {
            if (wantedChannel().isBlank()) {
                _ui.value = OnAirUi(loading = false, needsSetup = true)
                return
            }
            cached = try {
                withContext(Dispatchers.IO) { repo.refresh(wantedChannel(), wantedPlaylist()) }
            } catch (e: ApiException) {
                _ui.value = OnAirUi(loading = false, message = e.userMessage())
                return
            }
        }

        lineup = cached
        lineupDay = dayOf(ClockSync.nowMs())
        recompute()
        if (repo.isStale(cached)) refreshQuietly()
    }

    fun retry() {
        viewModelScope.launch { start() }
    }

    private suspend fun refreshQuietly() {
        try {
            pending = withContext(Dispatchers.IO) { repo.refresh(wantedChannel(), wantedPlaylist()) }
        } catch (e: ApiException) {
            // Keep playing what we have.
        }
    }

    private fun dayOf(ms: Long): Long = Math.floorDiv(ms, BroadcastScheduler.DAY_MS)

    private fun recompute() {
        tickJob?.cancel()
        val now = ClockSync.nowMs()
        val day = dayOf(now)
        if (day != lineupDay) {
            // New UTC day: adopt freshly fetched data and give refused videos another chance.
            pending?.let { lineup = it; pending = null }
            excluded.clear()
            lineupDay = day
        }
        val l = lineup ?: return
        val seed = "${l.channelId}:${l.playlistId}"

        val np = BroadcastScheduler.nowPlaying(l.videos, seed, now, excluded)
        if (np == null) {
            _ui.value = OnAirUi(
                loading = false,
                channelId = l.channelId,
                channelTitle = l.channelTitle,
                message = "No playable videos right now. Please try again later."
            )
            return
        }
        val upcoming = BroadcastScheduler.schedule(l.videos, seed, np.endsAtMs, UPCOMING_COUNT, excluded)
        _ui.value = OnAirUi(
            loading = false,
            channelId = l.channelId,
            channelTitle = l.channelTitle,
            nowPlaying = np,
            upcoming = upcoming
        )
        tickJob = viewModelScope.launch {
            delay(maxOf(250L, np.endsAtMs - ClockSync.nowMs()))
            tickJob = null
            recompute()
        }
    }

    /** The player refused this video (removed, private, embedding disabled...). Skip it for today. */
    fun onVideoError(videoId: String, code: Int) {
        if (_ui.value.nowPlaying?.video?.id != videoId) return
        excluded.add(videoId)
        recompute()
    }

    /** Safety net for when the timer was suspended (device asleep): re-sync when a video really ends. */
    fun onVideoEnded(videoId: String) {
        val np = _ui.value.nowPlaying ?: return
        if (np.video.id == videoId && ClockSync.nowMs() >= np.endsAtMs - 5_000L) recompute()
    }

    fun onResume() {
        if (lineup != null) recompute()
    }

    private companion object {
        const val UPCOMING_COUNT = 8
    }
}
