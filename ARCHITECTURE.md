# Architecture

```
MainActivity -> AppNavigation (consent -> onair <-> settings / legal;  setup only if no channel is configured)

ui/onair      OnAirScreen + OnAirViewModel + YouTubePlayer (WebView with the official IFrame player)
domain        BroadcastScheduler  (pure Kotlin: deterministic schedule, unit-tested)
data          LineupRepository -> YouTubeApi (OkHttp, org.json) -> Cloudflare Worker -> YouTube Data API v3
              LineupCache (29-day metadata cache), AppPrefs, ApiKeyStore (dev only), ClockSync, BrandConfig
brand/        strings, colours, icons, package id  (read through BrandConfig / R.*)
```

* **One async stack:** coroutines + `StateFlow`. UI collects state with `collectAsStateWithLifecycle`.
* **Schedule:** pool sorted by video id, shuffled with a fixed algorithm (FNV-1a seed + SplitMix64) per UTC day,
  repeated back-to-back through the day. "Now" = seconds since 00:00 UTC (server-corrected) modulo the lineup length.
  A new lineup starts at 00:00 UTC, so nothing ever jumps mid-day. Refreshed catalogue data is applied at the day change
  so all devices stay in step. A video the player refuses is skipped for the rest of the day on that device.
* **Clock:** `ClockSync` takes the HTTP `Date` header from a quota-free request (and from every API response).
* **Key handling:** production = Worker secret (the app has no key). Development = key from `local.properties`
  or typed by the user (stored in EncryptedSharedPreferences).
* **Player:** page loaded with base URL `https://<applicationId>` (Referer/origin), privacy-enhanced host, third-party
  cookies off, paused when the screen is not visible, links leaving the player open in the system.
* **Rebranding safety:** if the brand channel/playlist differs from what the cache was built for, the cache is discarded.
