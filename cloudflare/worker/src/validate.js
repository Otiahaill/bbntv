// Request validation for the YouTube proxy. Pure functions (no network) so they are easy to test.
//
// The proxy exists only to keep the API key off the device. It is NOT a general YouTube API:
// it answers three read-only calls the app needs, with whitelisted parameters, and nothing else.

export const RESOURCES = {
  channels: { params: ["part", "id", "forHandle"], parts: ["snippet", "contentDetails"], ttl: 86400 },
  playlistItems: { params: ["part", "playlistId", "maxResults", "pageToken"], parts: ["contentDetails"], ttl: 21600 },
  videos: { params: ["part", "id"], parts: ["snippet", "contentDetails", "status"], ttl: 21600 },
};

const CHANNEL_ID = /^UC[0-9A-Za-z_-]{22}$/;
const VIDEO_ID = /^[A-Za-z0-9_-]{11}$/;
const PLAYLIST_ID = /^[A-Za-z0-9_-]{10,64}$/;
const PAGE_TOKEN = /^[A-Za-z0-9_-]{1,200}$/;
const HANDLE = /^@?[A-Za-z0-9._-]{3,30}$/;

const list = (s) => (s || "").split(",").map((x) => x.trim()).filter(Boolean);
const bare = (h) => h.replace(/^@/, "").toLowerCase();
const fail = (status, message) => ({ ok: false, status, message });

export function validate(url, env = {}) {
  const m = /^\/youtube\/v3\/(channels|playlistItems|videos)$/.exec(url.pathname);
  if (!m) return fail(404, "not found");
  const resource = m[1];
  const spec = RESOURCES[resource];

  const params = {};
  for (const [k, v] of url.searchParams) {
    if (!spec.params.includes(k)) return fail(400, `parameter not allowed: ${k}`);
    if (k in params) return fail(400, `duplicate parameter: ${k}`);
    params[k] = v;
  }

  if (!params.part) return fail(400, "part is required");
  const parts = params.part.split(",");
  if (!parts.every((p) => spec.parts.includes(p))) return fail(400, "part not allowed");

  const channelIds = list(env.ALLOWED_CHANNEL_IDS);
  const handles = list(env.ALLOWED_HANDLES).map(bare);
  const extraPlaylists = list(env.ALLOWED_PLAYLIST_IDS);
  const locked = channelIds.length + handles.length + extraPlaylists.length > 0;

  if (resource === "channels") {
    const hasId = "id" in params, hasHandle = "forHandle" in params;
    if (hasId === hasHandle) return fail(400, "exactly one of id or forHandle is required");
    if (hasId) {
      if (!CHANNEL_ID.test(params.id)) return fail(400, "invalid channel id");
      if (locked && !channelIds.includes(params.id)) return fail(403, "channel not allowed");
    } else {
      if (!HANDLE.test(params.forHandle)) return fail(400, "invalid handle");
      if (locked && !handles.includes(bare(params.forHandle))) return fail(403, "channel not allowed");
    }
  }

  if (resource === "playlistItems") {
    if (!PLAYLIST_ID.test(params.playlistId || "")) return fail(400, "invalid playlistId");
    if (params.maxResults !== undefined) {
      const n = Number(params.maxResults);
      if (!Number.isInteger(n) || n < 1 || n > 50) return fail(400, "maxResults must be 1-50");
    }
    if (params.pageToken !== undefined && !PAGE_TOKEN.test(params.pageToken)) return fail(400, "invalid pageToken");
    if (locked) {
      // A channel's uploads playlist id is "UU" + the channel id without its "UC" prefix.
      const uploads = channelIds.map((c) => "UU" + c.slice(2));
      if (!uploads.includes(params.playlistId) && !extraPlaylists.includes(params.playlistId)) {
        return fail(403, "playlist not allowed");
      }
    }
  }

  if (resource === "videos") {
    const ids = (params.id || "").split(",");
    if (!params.id || ids.length > 50 || !ids.every((i) => VIDEO_ID.test(i))) return fail(400, "invalid video ids");
  }

  return { ok: true, resource, params, ttl: spec.ttl };
}

/** Same parameters in the same order -> same cache entry. The API key is never part of it. */
export function cacheKeyFor(url, params) {
  const sorted = Object.keys(params).sort().map((k) => `${k}=${encodeURIComponent(params[k])}`).join("&");
  return `${url.origin}${url.pathname}?${sorted}`;
}
