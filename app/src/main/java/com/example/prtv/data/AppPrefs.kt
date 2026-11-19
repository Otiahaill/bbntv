package com.example.prtv.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Small non-sensitive settings: consent flag and the channel/playlist the user picked. */
@Singleton
class AppPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sp = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var consentAccepted: Boolean
        get() = sp.getBoolean("consent_accepted", false)
        set(value) = sp.edit().putBoolean("consent_accepted", value).apply()

    var channelInput: String
        get() = sp.getString("channel_input", "") ?: ""
        set(value) = sp.edit().putString("channel_input", value).apply()

    var playlistInput: String
        get() = sp.getString("playlist_input", "") ?: ""
        set(value) = sp.edit().putString("playlist_input", value).apply()

    fun clearAll() {
        sp.edit().clear().apply()
    }
}
