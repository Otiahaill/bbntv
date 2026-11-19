import { test } from "node:test";
import assert from "node:assert/strict";
import { validate, cacheKeyFor } from "../src/validate.js";

const u = (path, qs) => new URL(`https://w.example${path}?${qs}`);
const CH = "UCabcdefghijklmnopqrstuv";           // UC + 22
const UPLOADS = "UUabcdefghijklmnopqrstuv";
const VID = "abcdefghijk";                        // 11 chars

test("unknown path is 404", () => assert.equal(validate(u("/youtube/v3/search", "part=snippet&q=x")).status, 404));
test("non-whitelisted parameter is rejected", () =>
  assert.equal(validate(u("/youtube/v3/videos", `part=snippet&id=${VID}&key=abc`)).status, 400));
test("part outside the whitelist is rejected", () =>
  assert.equal(validate(u("/youtube/v3/videos", `part=snippet,statistics&id=${VID}`)).status, 400));
test("valid videos request passes", () => assert.equal(validate(u("/youtube/v3/videos", `part=snippet,status&id=${VID}`)).ok, true));
test("more than 50 ids is rejected", () => {
  const ids = Array.from({ length: 51 }, () => VID).join(",");
  assert.equal(validate(u("/youtube/v3/videos", `part=snippet&id=${ids}`)).status, 400);
});
test("maxResults is bounded", () => {
  assert.equal(validate(u("/youtube/v3/playlistItems", `part=contentDetails&playlistId=${UPLOADS}&maxResults=500`)).status, 400);
  assert.equal(validate(u("/youtube/v3/playlistItems", `part=contentDetails&playlistId=${UPLOADS}&maxResults=50`)).ok, true);
});
test("channels needs exactly one of id / forHandle", () => {
  assert.equal(validate(u("/youtube/v3/channels", "part=snippet")).status, 400);
  assert.equal(validate(u("/youtube/v3/channels", `part=snippet&id=${CH}&forHandle=@abc`)).status, 400);
});
test("open mode (no allow-lists) accepts any valid channel", () =>
  assert.equal(validate(u("/youtube/v3/channels", `part=snippet,contentDetails&id=${CH}`), {}).ok, true));
test("locked mode blocks other channels and handles", () => {
  const env = { ALLOWED_CHANNEL_IDS: CH, ALLOWED_HANDLES: "@Mine" };
  assert.equal(validate(u("/youtube/v3/channels", `part=snippet&id=${CH}`), env).ok, true);
  assert.equal(validate(u("/youtube/v3/channels", `part=snippet&id=UCzzzzzzzzzzzzzzzzzzzzzz`), env).status, 403);
  assert.equal(validate(u("/youtube/v3/channels", "part=snippet&forHandle=@mine"), env).ok, true);
  assert.equal(validate(u("/youtube/v3/channels", "part=snippet&forHandle=@other"), env).status, 403);
});
test("locked mode allows only the uploads playlist of allowed channels", () => {
  const env = { ALLOWED_CHANNEL_IDS: CH };
  assert.equal(validate(u("/youtube/v3/playlistItems", `part=contentDetails&playlistId=${UPLOADS}`), env).ok, true);
  assert.equal(validate(u("/youtube/v3/playlistItems", "part=contentDetails&playlistId=PLsomethingelse123"), env).status, 403);
});
test("cache key ignores parameter order", () => {
  const url = u("/youtube/v3/videos", "x=1");
  assert.equal(cacheKeyFor(url, { b: "2", a: "1" }), cacheKeyFor(url, { a: "1", b: "2" }));
});
