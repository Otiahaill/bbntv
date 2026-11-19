import { validate, cacheKeyFor } from "./validate.js";

const json = (obj, status) =>
  new Response(JSON.stringify(obj), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });

export default {
  async fetch(request, env, ctx) {
    if (request.method !== "GET") return json({ error: { message: "method not allowed" } }, 405);
    const url = new URL(request.url);
    if (url.pathname === "/healthz") return new Response("ok");

    const v = validate(url, env);
    if (!v.ok) return json({ error: { message: v.message } }, v.status);
    if (!env.YOUTUBE_API_KEY) return json({ error: { message: "proxy is not configured" } }, 500);

    // Edge cache: protects the daily quota. Responses are kept for hours, far below YouTube's 30-day limit.
    const cache = caches.default;
    const cacheKey = new Request(cacheKeyFor(url, v.params));
    const hit = await cache.match(cacheKey);
    if (hit) return hit;

    const upstream = new URL("https://www.googleapis.com" + url.pathname);
    for (const [k, val] of Object.entries(v.params)) upstream.searchParams.set(k, val);
    upstream.searchParams.set("key", env.YOUTUBE_API_KEY);

    const r = await fetch(upstream.toString(), { headers: { Accept: "application/json" } });
    const body = await r.text();
    const out = new Response(body, {
      status: r.status,
      headers: {
        "content-type": "application/json; charset=utf-8",
        "cache-control": r.ok ? `public, max-age=${v.ttl}` : "no-store",
      },
    });
    if (r.ok) ctx.waitUntil(cache.put(cacheKey, out.clone()));
    return out;
  },
};
