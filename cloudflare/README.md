# Cloudflare Worker: keeps the YouTube API key off the device

The app calls this Worker; the Worker adds the key (stored as a Cloudflare **secret**) and forwards three read-only
YouTube Data API calls: `channels`, `playlistItems`, `videos`. It rejects everything else, caches responses for hours
at the edge, and can be locked to your channel.

## One-time setup (per project)
1. Create a free Cloudflare account. Install Node 18+.
2. In Google Cloud Console: create a project, enable **YouTube Data API v3**, create an API key and restrict it to
   that API. (Restrict by IP is not possible for Workers; the Worker's own allow-list and rate limit do the guarding.)
3. In `cloudflare/worker`:
   ```bash
   npm install
   npx wrangler login
   npx wrangler secret put YOUTUBE_API_KEY      # paste the key when asked; it is never written to a file
   ```
4. Edit `wrangler.toml`: set `name`, and lock it: `ALLOWED_CHANNEL_IDS` (a `UC…` id), and `ALLOWED_HANDLES`
   if `brand_channel` is a `@handle`.
5. `npx wrangler deploy`. Copy the URL it prints (`https://<name>.<you>.workers.dev`) into
   `brand/res/values/brand_strings.xml` -> `brand_proxy_url`.
6. Check it: `https://<url>/healthz` answers `ok`.
7. In the Cloudflare dashboard add a **rate limiting rule** (for example 60 requests/minute per IP) on the Worker route.

## Rules for this Worker (YouTube policy)
- Only this app uses it. Do not turn it into a general YouTube API for others.
- Do not add endpoints or parameters. Keep the allow-lists filled in production.
- Never log or echo the key. Do not store responses for more than 30 days (it stores hours).

Tests: `node --test`.
