# Changelog

## 0.2.0
- Rebuilt on a consistent toolchain (Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24, Compose compiler 1.5.14, minSdk 23).
- One async stack (coroutines/StateFlow); removed RxJava, Room and Glide-kapt.
- Working navigation, observable state, a player wired to the scheduler, daily-stable schedule.
- Official-player rules: Referer/origin, no overlays, no background playback.
- Legal screens, consent gate, YouTube attribution, data deletion.
- `brand/` folder, icon generator, brand checker, Cloudflare Worker key proxy.
- Removed the exposed API key from the project and from CI; CI builds with no key.
