package com.example.prtv.data

/**
 * "TV time". The schedule must be identical on every device, so we correct the device clock using the
 * HTTP Date header of YouTube API responses. Until the first response arrives the offset is zero.
 */
object ClockSync {
    @Volatile
    private var offsetMs: Long = 0L

    fun nowMs(): Long = System.currentTimeMillis() + offsetMs

    fun update(serverDateMs: Long, receivedAtMs: Long) {
        offsetMs = serverDateMs - receivedAtMs
    }
}
