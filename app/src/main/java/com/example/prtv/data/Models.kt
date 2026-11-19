package com.example.prtv.data

/** A YouTube video as returned by the Data API (metadata only; no media is ever stored). */
data class Video(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val embeddable: Boolean,
    val privacyStatus: String,
    val liveBroadcastContent: String,
    val madeForKids: Boolean
)

/** The set of videos the "TV channel" draws from, plus when it was fetched. */
data class Lineup(
    val channelId: String,
    val channelTitle: String,
    val playlistId: String,
    val videos: List<Video>,
    val fetchedAtMs: Long
)
