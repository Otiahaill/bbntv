package com.example.prtv.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * YouTube attribution, shown wherever YouTube content (player, titles, thumbnails) is displayed.
 *
 * YouTube Branding Guidelines: use the official "developed with YouTube" logo, never alter it, make it
 * clickable (links to YouTube content), and never make it the most prominent element on screen.
 *
 * The official logo files cannot be bundled by this code: download them from the YouTube brand
 * resources and save one as res/drawable-nodpi/developed_with_youtube.png (see COMPLIANCE.md).
 * Until that file exists, a plain-text "Developed with YouTube" link is shown instead.
 */
@SuppressLint("DiscouragedApi")
@Composable
fun YouTubeAttribution(channelId: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val url = if (channelId.isNullOrBlank()) {
        "https://www.youtube.com"
    } else {
        "https://www.youtube.com/channel/$channelId"
    }
    val logoRes = remember {
        context.resources.getIdentifier("developed_with_youtube", "drawable", context.packageName)
    }
    Row(
        modifier = modifier
            .clickable { openLink(context, url) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (logoRes != 0) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = "Developed with YouTube. Opens YouTube.",
                modifier = Modifier.height(24.dp)
            )
        } else {
            Text(
                text = "Developed with YouTube",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
