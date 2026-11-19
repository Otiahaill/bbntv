package com.example.prtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin client for the official YouTube Data API v3 (unauthenticated, API-key calls only).
 * Quota cost per call is 1 unit for every endpoint used here (we never call search.list).
 */
@Singleton
class YouTubeApi @Inject constructor(
    private val client: OkHttpClient,
    private val keyStore: ApiKeyStore,
    private val brand: BrandConfig
) {
    data class ChannelInfo(val id: String, val title: String, val uploadsPlaylistId: String)

    suspend fun resolveChannel(input: String): ChannelInfo {
        val ref = ChannelRef.parse(input) ?: throw ApiException.ChannelNotFound()
        val params = mutableMapOf("part" to "snippet,contentDetails")
        when (ref) {
            is ChannelRef.Id -> params["id"] = ref.value
            is ChannelRef.Handle -> params["forHandle"] = ref.value
        }
        val items = get("channels", params).optJSONArray("items")
        if (items == null || items.length() == 0) throw ApiException.ChannelNotFound()
        val item = items.getJSONObject(0)
        val uploads = item.optJSONObject("contentDetails")
            ?.optJSONObject("relatedPlaylists")
            ?.optString("uploads")
            .orEmpty()
        if (uploads.isBlank()) throw ApiException.ChannelNotFound()
        return ChannelInfo(
            id = item.getString("id"),
            title = item.optJSONObject("snippet")?.optString("title").orEmpty(),
            uploadsPlaylistId = uploads
        )
    }

    suspend fun playlistVideoIds(playlistId: String, max: Int = MAX_VIDEOS): List<String> {
        val ids = LinkedHashSet<String>()
        var token: String? = null
        do {
            val params = mutableMapOf(
                "part" to "contentDetails",
                "playlistId" to playlistId,
                "maxResults" to "50"
            )
            token?.let { params["pageToken"] = it }
            val json = get("playlistItems", params)
            val items = json.optJSONArray("items")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val videoId = items.getJSONObject(i)
                        .optJSONObject("contentDetails")
                        ?.optString("videoId")
                        .orEmpty()
                    if (videoId.isNotBlank()) ids.add(videoId)
                }
            }
            token = json.optString("nextPageToken").takeIf { it.isNotBlank() }
        } while (token != null && ids.size < max)
        return ids.toList().take(max)
    }

    suspend fun videos(ids: List<String>): List<Video> {
        val out = ArrayList<Video>()
        for (chunk in ids.chunked(50)) {
            val json = get(
                "videos",
                mapOf("part" to "snippet,contentDetails,status", "id" to chunk.joinToString(","))
            )
            val items = json.optJSONArray("items") ?: continue
            for (i in 0 until items.length()) out.add(parseVideo(items.getJSONObject(i)))
        }
        return out
    }

    /** HEAD on a tiny Google endpoint (no API quota). The OkHttp interceptor reads the Date header. */
    suspend fun syncClock() {
        try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url("https://www.google.com/generate_204").head().build()
                client.newCall(request).execute().close()
            }
        } catch (e: IOException) {
            // Offline: keep using the device clock.
        }
    }

    private fun parseVideo(o: JSONObject): Video {
        val snippet = o.optJSONObject("snippet")
        val content = o.optJSONObject("contentDetails")
        val status = o.optJSONObject("status")
        val thumbs = snippet?.optJSONObject("thumbnails")
        val thumb = thumbs?.optJSONObject("medium")?.optString("url")
            ?: thumbs?.optJSONObject("default")?.optString("url")
        return Video(
            id = o.getString("id"),
            title = snippet?.optString("title").orEmpty(),
            thumbnailUrl = thumb?.takeIf { it.isNotBlank() },
            durationSeconds = DurationParser.parseSeconds(content?.optString("duration")),
            embeddable = status?.optBoolean("embeddable", false) ?: false,
            privacyStatus = status?.optString("privacyStatus").orEmpty(),
            liveBroadcastContent = snippet?.optString("liveBroadcastContent", "none") ?: "none",
            madeForKids = status?.optBoolean("madeForKids", false) ?: false
        )
    }

    private suspend fun get(resource: String, params: Map<String, String>): JSONObject {
        // Production: our Cloudflare Worker adds the key server-side, so the app holds no key.
        // Development: call YouTube directly with a key from local.properties or typed in by the user.
        val proxy = if (brand.usesProxy) brand.proxyUrl.toHttpUrlOrNull() else null
        val key = keyStore.apiKey()
        if (proxy == null && key.isBlank()) throw ApiException.MissingKey()

        val url = (proxy?.newBuilder()
            ?: HttpUrl.Builder().scheme("https").host("www.googleapis.com"))
            .addPathSegments("youtube/v3/$resource")
            .apply {
                params.forEach { (k, v) -> addQueryParameter(k, v) }
                if (proxy == null) addQueryParameter("key", key)
            }
            .build()
        val request = Request.Builder().url(url).get().build()

        val (code, body) = try {
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { it.code to it.body?.string().orEmpty() }
            }
        } catch (e: IOException) {
            throw ApiException.Network(e)
        }

        if (code in 200..299) {
            return try {
                JSONObject(body)
            } catch (e: JSONException) {
                throw ApiException.Http(code, "unreadable response")
            }
        }
        throw mapError(code, body)
    }

    private fun mapError(code: Int, body: String): ApiException {
        val error = runCatching { JSONObject(body).getJSONObject("error") }.getOrNull()
        val reason = runCatching {
            error?.getJSONArray("errors")?.getJSONObject(0)?.optString("reason")
        }.getOrNull().orEmpty()
        val message = error?.optString("message").orEmpty()
        return when {
            reason == "quotaExceeded" || reason == "rateLimitExceeded" -> ApiException.QuotaExceeded()
            reason == "playlistNotFound" -> ApiException.PlaylistNotFound()
            reason == "keyInvalid" || reason == "ipRefererBlocked" ||
                message.contains("API key", ignoreCase = true) -> ApiException.InvalidKey()
            else -> ApiException.Http(code, message.ifBlank { reason.ifBlank { "unknown" } })
        }
    }

    companion object {
        /** Upper bound for one lineup (keeps quota use to roughly 8 units per refresh). */
        const val MAX_VIDEOS = 200
    }
}
