package com.example.prtv.ui.onair

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.prtv.domain.BroadcastScheduler
import com.example.prtv.ui.component.YouTubeAttribution
import java.text.DateFormat
import java.util.Date

@Composable
fun OnAirScreen(
    onOpenSettings: () -> Unit,
    onNeedsSetup: () -> Unit,
    vm: OnAirViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(ui.needsSetup) { if (ui.needsSetup) onNeedsSetup() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val wide = maxWidth > 840.dp
        val np = ui.nowPlaying

        if (ui.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (np == null) {
            MessagePane(
                message = ui.message ?: "Nothing to play yet.",
                onRetry = vm::retry,
                onOpenSettings = onOpenSettings
            )
        } else if (wide) {
            // TV / tablet landscape: player on the left, guide on the right. Nothing overlaps the player.
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PlayerBox(np, vm, Modifier.weight(2f).align(Alignment.CenterVertically))
                SidePanel(ui, np, onOpenSettings, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                PlayerBox(np, vm, Modifier.fillMaxWidth())
                SidePanel(
                    ui, np, onOpenSettings,
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerBox(np: BroadcastScheduler.NowPlaying, vm: OnAirViewModel, modifier: Modifier) {
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .background(Color.Black)
    ) {
        YouTubePlayer(
            videoId = np.video.id,
            startedAtMs = np.startedAtMs,
            onEnded = vm::onVideoEnded,
            onError = vm::onVideoError,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SidePanel(
    ui: OnAirUi,
    np: BroadcastScheduler.NowPlaying,
    onOpenSettings: () -> Unit,
    modifier: Modifier
) {
    val timeFormat = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    Column(modifier) {
        Text(
            "SCHEDULED PLAYBACK",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "A scheduled sequence of videos from YouTube, not a live broadcast.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            np.video.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            ui.channelTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // YouTube branding stays on screen next to YouTube content, small and clickable.
        YouTubeAttribution(channelId = ui.channelId)
        Spacer(Modifier.height(12.dp))
        Text(
            "Up next",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyColumn(Modifier.weight(1f)) {
            items(ui.upcoming) { slot ->
                UpcomingRow(slot, timeFormat.format(Date(slot.startMs)))
            }
        }
        TextButton(onClick = onOpenSettings) { Text("Settings, legal and data") }
    }
}

@Composable
private fun UpcomingRow(slot: BroadcastScheduler.Slot, time: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            time,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        AsyncImage(
            model = slot.video.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(96.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            slot.video.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MessagePane(message: String, onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Try again") }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) {
            Text("Settings and legal")
        }
    }
}
