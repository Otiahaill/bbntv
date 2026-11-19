# YouTube compliance status

Checked against YouTube API Services Developer Policies (updated 2026-11-06) and Branding Guidelines
(updated 2025-08-20). Re-read both before every release: https://developers.google.com/youtube/terms/developer-policies
Not legal advice. Status: **done** (in code) / **owner** (you must do it) / **review** (judgement call).

## Player
- [x] done  Official IFrame player in the OS WebView, Referer/origin = `https://<applicationId>`
- [x] done  Nothing overlays the player; guide and branding sit beside or below it
- [x] done  No background playback (pauses when the screen is not showing)
- [x] done  Links in the player are never blocked (they open in the system)
- [x] done  No download, no caching of media, no ad blocking, no scraping (official API + player only)
- [ ] review  Viewport is 16:9 at full width: height drops under 200 px on screens narrower than ~356 dp

## Terms, privacy, consent
- [x] done  In-app Terms of Use + Privacy Policy, YouTube ToS and Google Privacy Policy links, API-use statement
- [x] done  First-run consent gate; "Clear stored data" in Settings
- [ ] owner  Host `docs/PRIVACY_POLICY.md` and `docs/TERMS_OF_USE.md` at public URLs (Play requires one)
- [x] done  Set `contact_email` in `brand_strings.xml` → hemprodEnterprises@gmail.com

## Branding
- [x] done  App name check (no "YouTube"/"YT") in `scripts/check_brand.py` → "BBN tv"
- [x] done  Attribution shown with all YouTube content, small, clickable
- [ ] owner  Add the OFFICIAL logo: `brand/res/drawable-nodpi/developed_with_youtube.png`
              (https://www.youtube.com/yt/brand/using-logo.html). Do not recolour or redraw it. Check its minimum size there.
              Until it exists a plain-text "Developed with YouTube" link is shown (not the official logo).
- [x] done  Do not use YouTube marks next to your own logo or app name

## Credentials and quota
- [x] done  No key in the app or repo when the Worker is used; CI builds without a key
- [ ] owner  Store the key as a Worker secret; restrict it in Google Cloud to YouTube Data API v3
- [ ] owner  Rotate the key that was in the original zip (it was exposed in `project.md`)
- [x] done  29-day metadata cache limit; Worker caches for hours only
- [ ] owner  Monitor quota (default 10,000 units/day per project); apply for an audit/extension if needed

## Children (Made for Kids)
- [x] done  Privacy-enhanced player host, third-party cookies off, no sign-in, no analytics or ad SDKs
- [x] done  `madeForKids` is read for every video and stored
- [x] done  Channel is NOT child-directed (user confirmed)
- [ ] review  Confirm with YouTube's Made-for-Kids guidance whether further per-video handling is required

## Third-party marks
- [x] done  Non-affiliation notice in the Terms
- [ ] owner  Brand names/characters (e.g. Power Rangers) belong to their owners: get permission or use a neutral name
              → BBN tv appears to be your own brand; verify no third-party trademarks are used

## Risk flags (conservative readings)
- Browse-clone: the app has no channel/playlist browser; value is the scheduled TV experience. (III.I.1)
- Aggregation: one channel / one playlist per install; no cross-channel merging. (III.E.2)
- Derived data: only durations summed for scheduling; nothing else is computed or displayed. (III.E.4)
- Proxy: the Worker serves only this app, three read-only calls, whitelisted parameters. Do not widen it. (III.I.15)
- If unsure about any item, submit YouTube's API Compliance Audit form before shipping.

---

## Brand Check Summary (as of 2026-11-19)

### ✅ Completed
- `brand_strings.xml`: app_name="BBN tv", brand_channel="UCP8PPvgcD1Efbf0j-jCzZXQ", contact_email="hemprodEnterprises@gmail.com"
- `brand_colors.xml`: Apple-inspired dark theme (primary #5AC8FA, background #000000, surface #1C1C1E)
- `brand.properties`: applicationId=com.bbntv.tv, versionCode=1, versionName=1.0.0
- `wrangler.toml`: name="bbntv-proxy", ALLOWED_CHANNEL_IDS="UCP8PPvgcD1Efbf0j-jCzZXQ", ALLOWED_HANDLES="GasperAswenMinistries"
- `PRIVACY_POLICY.md` & `TERMS_OF_USE.md`: contact email updated
- `brand/logo.png`: 664x672 (needs to be square 512x512+)
- `brand/res/drawable-nodpi/developed_with_youtube.png`: placeholder uploaded (needs official YouTube logo)

### ⚠️ Warnings (check_brand.py --release)
1. `brand_proxy_url` is empty — set after Worker deploy
2. `brand/logo.png` is not square (664x672) — crop to 512x512 or larger square
3. Official YouTube logo missing — download from https://www.youtube.com/yt/brand/using-logo.html ("Developed with YouTube" lockup, PNG, do not edit)

### 🔴 Errors (release mode)
- Local API key in `local.properties` — **remove before release**; use Worker secret instead

---

## What You Must Do Before Release

1. **Crop logo to square 512x512+** and replace `brand/logo.png`, then run `scripts/make-icons.sh`
2. **Download official "Developed with YouTube" logo** (PNG) → `brand/res/drawable-nodpi/developed_with_youtube.png`
3. **Deploy Cloudflare Worker** (needs computer with browser for OAuth):
   ```bash
   cd bbntv/cloudflare/worker
   npm install
   npx wrangler login
   npx wrangler secret put YOUTUBE_API_KEY
   npx wrangler deploy
   ```
   Copy the Worker URL → `brand_strings.xml` → `brand_proxy_url`
4. **Remove API key from `local.properties`** before committing/pushing
5. **Host `docs/PRIVACY_POLICY.md` and `docs/TERMS_OF_USE.md`** at public URLs (GitHub Pages, Netlify, etc.)
6. **Build via GitHub Actions** (CI/CD already configured in `.github/workflows/ci.yml`)
7. **Test on device** — verify scheduled playback, consent flow, settings

---

## Notes
- **Child-directed**: User confirmed NO — no COPPA/GDPR extra steps needed
- **Third-party marks**: "BBN tv" appears to be your brand; ensure no unauthorized trademarks
- **Build**: Use GitHub Actions (`.github/workflows/ci.yml`) — local Gradle has platform limitations on Android
- **Worker URL**: Update `brand_proxy_url` after deploy; app will use it automatically
