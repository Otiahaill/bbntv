package com.example.prtv.data

import android.content.Context
import com.example.prtv.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Values from /brand/res/values/brand_strings.xml. Edit those, not this code. */
@Singleton
class BrandConfig @Inject constructor(
    @ApplicationContext context: Context
) {
    val appName: String = context.getString(R.string.app_name).trim()

    /** Channel @handle / URL / ID fixed by the brand. Blank = the user chooses on first run. */
    val channel: String = context.getString(R.string.brand_channel).trim()

    /** Optional playlist fixed by the brand. */
    val playlist: String = context.getString(R.string.brand_playlist).trim()

    /** Cloudflare Worker base URL. Blank = call YouTube directly with a development key. */
    val proxyUrl: String = context.getString(R.string.brand_proxy_url).trim().trimEnd('/')

    val contactEmail: String = context.getString(R.string.contact_email).trim()

    val channelIsFixed: Boolean get() = channel.isNotEmpty()
    val usesProxy: Boolean get() = proxyUrl.isNotEmpty()
}
