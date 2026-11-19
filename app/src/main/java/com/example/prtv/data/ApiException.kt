package com.example.prtv.data

sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MissingKey : ApiException("No YouTube API key is configured.")
    class InvalidKey : ApiException("The YouTube API key was rejected.")
    class QuotaExceeded : ApiException("The daily YouTube API quota is used up.")
    class ChannelNotFound : ApiException("Channel not found.")
    class PlaylistNotFound : ApiException("Playlist not found.")
    class NoPlayableVideos : ApiException("No playable videos were found.")
    class Network(cause: Throwable) : ApiException("Network error.", cause)
    class Http(val code: Int, detail: String) : ApiException("YouTube API error ($code): $detail")
}

/** Short, user-facing text for an [ApiException]. */
fun ApiException.userMessage(): String = when (this) {
    is ApiException.MissingKey -> "Add a YouTube Data API key to continue."
    is ApiException.InvalidKey -> "That API key was rejected. Check it and its restrictions in Google Cloud."
    is ApiException.QuotaExceeded -> "The daily YouTube quota is used up. Try again tomorrow."
    is ApiException.ChannelNotFound -> "Channel not found. Use a @handle, channel URL or channel ID."
    is ApiException.PlaylistNotFound -> "Playlist not found. Check the playlist ID or URL."
    is ApiException.NoPlayableVideos -> "No playable videos found (they may be private, live, or not embeddable)."
    is ApiException.Network -> "Network problem. Check your connection and try again."
    is ApiException.Http -> message ?: "YouTube returned an error."
}
