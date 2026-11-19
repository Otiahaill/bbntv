package com.example.prtv.ui.onair

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.prtv.data.ClockSync

private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

/**
 * The official YouTube IFrame player inside an OS WebView. Nothing is drawn over it, it is never
 * modified or blocked, and it pauses whenever the screen is not showing it (no background playback).
 *
 * The page is loaded with a base URL of https://<applicationId> so YouTube receives the Referer/origin
 * it requires from apps; without it the player can fail with a configuration error.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayer(
    videoId: String,
    startedAtMs: Long,
    onEnded: (String) -> Unit,
    onError: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentVideoId by rememberUpdatedState(videoId)
    val currentStartedAt by rememberUpdatedState(startedAtMs)
    val currentOnEnded by rememberUpdatedState(onEnded)
    val currentOnError by rememberUpdatedState(onError)
    var ready by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webViewClient = PlayerWebViewClient(context)
            addJavascriptInterface(
                PlayerBridge(
                    onReady = { ready = true },
                    onEnded = { currentOnEnded(currentVideoId) },
                    onError = { code -> currentOnError(currentVideoId, code) }
                ),
                "Android"
            )
            val origin = "https://${context.packageName.lowercase()}"
            loadDataWithBaseURL(origin, playerHtml(origin), "text/html", "utf-8", null)
        }
    }

    // Jump to where the schedule says we should be.
    fun loadCurrent() {
        if (!VIDEO_ID.matches(currentVideoId)) return
        val offset = ((ClockSync.nowMs() - currentStartedAt) / 1000).coerceAtLeast(0L)
        webView.evaluateJavascript("loadVideo('$currentVideoId',$offset)", null)
    }

    LaunchedEffect(ready, videoId, startedAtMs) {
        if (ready) loadCurrent()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webView.evaluateJavascript("pauseVideo()", null)
                    webView.onPause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webView.onResume()
                    if (ready) loadCurrent()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webView.removeJavascriptInterface("Android")
            webView.destroy()
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)
}

private class PlayerBridge(
    private val onReady: () -> Unit,
    private val onEnded: () -> Unit,
    private val onError: (Int) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun ready() {
        main.post { onReady() }
    }

    @JavascriptInterface
    fun ended() {
        main.post { onEnded() }
    }

    @JavascriptInterface
    fun error(code: Int) {
        main.post { onError(code) }
    }
}

/** Links that leave the player page open in the system (YouTube app / browser); they are never blocked. */
private class PlayerWebViewClient(private val context: Context) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) return false
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, request.url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            // No handler (some TVs).
        }
        return true
    }
}

private fun playerHtml(origin: String): String = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  html, body { margin: 0; height: 100%; background: #000; overflow: hidden; }
  #player { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
</style>
</head>
<body>
<div id="player"></div>
<script>
  var player = null, isReady = false, pending = null;
  var tag = document.createElement('script');
  tag.src = 'https://www.youtube.com/iframe_api';
  document.head.appendChild(tag);

  function onYouTubeIframeAPIReady() {
    player = new YT.Player('player', {
      width: '100%', height: '100%',
      host: 'https://www.youtube-nocookie.com',
      playerVars: { playsinline: 1, rel: 0, fs: 0, origin: '$origin' },
      events: {
        onReady: function () {
          isReady = true;
          Android.ready();
          if (pending) { loadVideo(pending[0], pending[1]); pending = null; }
        },
        onStateChange: function (e) { if (e.data === 0) { Android.ended(); } },
        onError: function (e) { Android.error(e.data); }
      }
    });
  }
  function loadVideo(id, start) {
    if (!isReady) { pending = [id, start]; return; }
    player.loadVideoById({ videoId: id, startSeconds: start });
  }
  function pauseVideo() { if (isReady) { player.pauseVideo(); } }
</script>
</body>
</html>
""".trimIndent()
