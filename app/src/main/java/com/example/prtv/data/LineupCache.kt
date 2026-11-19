package com.example.prtv.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores API *metadata* (titles, durations, thumbnail URLs) so the app works between refreshes.
 * YouTube policy: non-authorized API data may be kept at most 30 days, then refreshed or deleted.
 * We delete after [MAX_AGE_MS] (29 days) to stay safely inside that limit. No media is ever stored.
 */
@Singleton
class LineupCache @Inject constructor(
    @ApplicationContext context: Context
) {
    private val file = File(context.filesDir, "lineup.json")

    fun read(nowMs: Long): Lineup? {
        if (!file.exists()) return null
        return try {
            val o = JSONObject(file.readText())
            val fetchedAt = o.getLong("fetchedAtMs")
            if (nowMs - fetchedAt > MAX_AGE_MS) {
                file.delete()
                return null
            }
            val arr = o.getJSONArray("videos")
            val videos = (0 until arr.length()).map { i ->
                val v = arr.getJSONObject(i)
                Video(
                    id = v.getString("id"),
                    title = v.optString("title"),
                    thumbnailUrl = v.optString("thumb").takeIf { it.isNotBlank() },
                    durationSeconds = v.getLong("dur"),
                    embeddable = v.getBoolean("emb"),
                    privacyStatus = v.optString("priv"),
                    liveBroadcastContent = v.optString("live", "none"),
                    madeForKids = v.optBoolean("mfk", false)
                )
            }
            Lineup(
                channelId = o.getString("channelId"),
                channelTitle = o.optString("channelTitle"),
                playlistId = o.getString("playlistId"),
                videos = videos,
                fetchedAtMs = fetchedAt
            )
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    fun write(lineup: Lineup) {
        val arr = JSONArray()
        lineup.videos.forEach { v ->
            arr.put(
                JSONObject()
                    .put("id", v.id)
                    .put("title", v.title)
                    .put("thumb", v.thumbnailUrl ?: "")
                    .put("dur", v.durationSeconds)
                    .put("emb", v.embeddable)
                    .put("priv", v.privacyStatus)
                    .put("live", v.liveBroadcastContent)
                    .put("mfk", v.madeForKids)
            )
        }
        val o = JSONObject()
            .put("channelId", lineup.channelId)
            .put("channelTitle", lineup.channelTitle)
            .put("playlistId", lineup.playlistId)
            .put("fetchedAtMs", lineup.fetchedAtMs)
            .put("videos", arr)
        file.writeText(o.toString())
    }

    fun clear() {
        file.delete()
    }

    companion object {
        const val MAX_AGE_MS = 29L * 24 * 60 * 60 * 1000
    }
}
