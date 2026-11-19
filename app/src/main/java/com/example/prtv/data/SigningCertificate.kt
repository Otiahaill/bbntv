package com.example.prtv.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * SHA-1 of the app's signing certificate (upper-case hex, no colons). Sent as X-Android-Cert so a Google
 * Cloud API key restricted to "Android apps" accepts our direct REST calls.
 */
object SigningCertificate {
    @Suppress("DEPRECATION")
    fun sha1Hex(context: Context): String? = runCatching {
        val pm = context.packageManager
        val signature = if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo.apkContentsSigners.first()
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures.first()
        }
        MessageDigest.getInstance("SHA-1").digest(signature.toByteArray())
            .joinToString("") { "%02X".format(it) }
    }.getOrNull()
}
