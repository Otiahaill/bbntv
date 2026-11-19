package com.example.prtv.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.prtv.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the YouTube API key comes from. A key typed in by the user (kept encrypted on the device) wins
 * over the build-time key; nothing here ever overwrites the user's key.
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences? by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    fun userKey(): String? = prefs?.getString(KEY_USER, null)?.takeIf { it.isNotBlank() }

    fun hasUserKey(): Boolean = userKey() != null

    fun hasBuildKey(): Boolean = BuildConfig.YOUTUBE_API_KEY.isNotBlank()

    /** Key used for API calls; empty string when none is configured. */
    fun apiKey(): String = userKey() ?: BuildConfig.YOUTUBE_API_KEY

    fun saveUserKey(key: String) {
        prefs?.edit()?.putString(KEY_USER, key.trim())?.apply()
    }

    fun clearUserKey() {
        prefs?.edit()?.remove(KEY_USER)?.apply()
    }

    private companion object {
        const val FILE_NAME = "secure_prefs"
        const val KEY_USER = "user_api_key"
    }
}
