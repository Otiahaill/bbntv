# Power Rangers TV: a reusable "YouTube channel as a TV set" app

Turns one YouTube channel (or playlist) into a scheduled, TV-style channel for Android TV, Google TV and
phones. Everyone who tunes in sees the same video at the same point. It is a *scheduled replay*, and the
app says so on screen.

**Reusable by design.** For a new project you edit only the `brand/` folder:

| You change | Where |
|---|---|
| Channel (`@handle`, URL or `UC…` ID) and optional playlist | `brand/res/values/brand_strings.xml` |
| App name, contact email, proxy URL | `brand/res/values/brand_strings.xml` |
| Colours | `brand/res/values/brand_colors.xml` |
| Logo | replace `brand/logo.png`, then run `scripts/make-icons.sh` |
| Package id, version | `brand/brand.properties` |

No Kotlin changes are needed. The API key never goes in the app: it lives in a Cloudflare Worker secret.

## Start a new project (about 15 minutes)

1. Copy this folder. Edit `brand/` as above. Run `scripts/make-icons.sh`.
2. Put the Worker online (`cloudflare/README.md`), paste its URL into `brand_proxy_url`.
3. Download the official YouTube logo and save it as `brand/res/drawable-nodpi/developed_with_youtube.png`
   (see `COMPLIANCE.md`).
4. Host `docs/PRIVACY_POLICY.md` and `docs/TERMS_OF_USE.md` at public URLs; set `contact_email`.
5. `python3 scripts/check_brand.py` (add `--release` before publishing). Fix everything it reports.
6. Build and test (below), then walk through `COMPLIANCE.md`.

## Build

Requirements: JDK 17, Android SDK 34, Gradle 8.7 (AGP 8.5 does not work with Gradle 9).

```bash
gradle wrapper --gradle-version 8.7     # once: creates gradlew + gradle-wrapper.jar, then commit them
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Without a proxy (development only) give the build a *restricted* key via `local.properties`
(`youtubeApiKey=…`, see `local.properties.example`), `-PyoutubeApiKey=…` or the `YOUTUBE_API_KEY`
environment variable. With no key and no proxy the app asks for one on first run.

## Layout

```
brand/            <- the only folder you edit per project
app/              shared Android code (Kotlin, Compose, Hilt)
cloudflare/       Worker that keeps the API key off the device
scripts/          make-icons.sh, check_brand.py
docs/             privacy policy + terms (hosting copies), FIX_REPORT.md, archive/
COMPLIANCE.md     YouTube policy checklist and owner to-dos
ARCHITECTURE.md   how it works
```
