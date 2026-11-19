#!/usr/bin/env python3
"""Checks the /brand folder before you build or release.  usage: python3 scripts/check_brand.py [--release]

Errors always fail. Warnings fail only with --release (use it before publishing).
Standard library only.
"""
import os, re, struct, sys
import xml.etree.ElementTree as ET

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RELEASE = "--release" in sys.argv
errors, warnings = [], []

def read(path):
    with open(os.path.join(ROOT, path), encoding="utf-8") as f:
        return f.read()

def values(path, tag):
    out = {}
    for el in ET.fromstring(read(path)).iter(tag):
        out[el.get("name")] = (el.text or "").strip()
    return out

def lum(hexcolor):
    h = hexcolor.lstrip("#")
    if len(h) == 8: h = h[2:]            # AARRGGBB -> RRGGBB
    r, g, b = (int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))
    f = lambda c: c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)

def contrast(a, b):
    la, lb = sorted((lum(a), lum(b)), reverse=True)
    return (la + 0.05) / (lb + 0.05)

strings = values("brand/res/values/brand_strings.xml", "string")
colors = values("brand/res/values/brand_colors.xml", "color")
props = dict(l.strip().split("=", 1) for l in read("brand/brand.properties").splitlines()
             if l.strip() and not l.startswith("#") and "=" in l)

# --- name ---
name = strings.get("app_name", "")
if not name: errors.append("app_name is empty")
if re.search(r"you\s*-?\s*tube|\byt\b", name, re.I):
    errors.append('app_name must not contain "YouTube" or "YT" (YouTube branding guidelines)')

# --- channel / proxy / contact ---
if not strings.get("brand_channel"):
    warnings.append("brand_channel is empty: users will be asked to type a channel on first run")
if not strings.get("brand_proxy_url"):
    warnings.append("brand_proxy_url is empty: the app will call YouTube directly with a key (development only)")
elif not strings["brand_proxy_url"].startswith("https://"):
    errors.append("brand_proxy_url must start with https://")
if "CHANGE_ME" in strings.get("contact_email", "CHANGE_ME") or "@" not in strings.get("contact_email", ""):
    warnings.append("contact_email is still a placeholder (required in the privacy policy)")

# --- application id ---
app_id = props.get("applicationId", "")
if not re.fullmatch(r"[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+", app_id):
    errors.append(f"applicationId looks invalid: {app_id!r}")
if app_id.startswith("com.example."):
    warnings.append("applicationId starts with com.example. (Google Play rejects it)")

# --- colours & contrast (dark theme) ---
need = ["brand_primary", "brand_on_primary", "brand_secondary", "brand_background", "brand_surface",
        "brand_surface_variant", "brand_on_background", "brand_on_surface_variant", "brand_error",
        "ic_launcher_background"]
for k in need:
    v = colors.get(k)
    if not v or not re.fullmatch(r"#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})", v):
        errors.append(f"colour {k} missing or not #RRGGBB: {v!r}")
if not errors:
    pairs = [("brand_on_background", "brand_background", 7.0),
             ("brand_on_background", "brand_surface", 4.5),
             ("brand_on_surface_variant", "brand_background", 4.5),
             ("brand_on_primary", "brand_primary", 4.5),
             ("brand_primary", "brand_background", 3.0)]
    for fg, bg, minimum in pairs:
        c = contrast(colors[fg], colors[bg])
        if c < minimum:
            errors.append(f"low contrast {fg} on {bg}: {c:.1f}:1 (need {minimum}:1)")
    if lum(colors["brand_background"]) > 0.25:
        errors.append("brand_background is light; this template is dark-theme only")

# --- logo / icons ---
def png_size(rel):
    try:
        with open(os.path.join(ROOT, rel), "rb") as f:
            head = f.read(24)
        if head[:8] != b"\x89PNG\r\n\x1a\n": return None
        return struct.unpack(">II", head[16:24])
    except OSError:
        return None

s = png_size("brand/logo.png")
if s is None: errors.append("brand/logo.png missing or not a PNG")
else:
    if s[0] != s[1]: warnings.append(f"brand/logo.png is not square ({s[0]}x{s[1]})")
    if min(s) < 288: warnings.append(f"brand/logo.png is small ({s[0]}x{s[1]}); use 512x512 or more")
for rel in ["brand/res/mipmap-xxxhdpi/ic_launcher_foreground.png", "brand/res/mipmap-xxxhdpi/ic_launcher.png",
            "brand/res/drawable-xhdpi/banner.png"]:
    if png_size(rel) is None:
        errors.append(f"{rel} missing: run scripts/make-icons.sh")
if png_size("brand/res/drawable-nodpi/developed_with_youtube.png") is None:
    warnings.append("official YouTube logo missing: save it as brand/res/drawable-nodpi/developed_with_youtube.png "
                    "(see COMPLIANCE.md); a text fallback is shown until then")

# --- secrets in the tree ---
key_re = re.compile(r"AIza[0-9A-Za-z_\-]{20,}")
skip = {".git", ".gradle", "build", "node_modules", ".idea"}
for dirpath, dirnames, files in os.walk(ROOT):
    dirnames[:] = [d for d in dirnames if d not in skip]
    for fn in files:
        if fn.endswith((".png", ".jpg", ".zip", ".jar")): continue
        p = os.path.join(dirpath, fn)
        try:
            if key_re.search(open(p, encoding="utf-8", errors="ignore").read()):
                errors.append(f"looks like a Google API key in {os.path.relpath(p, ROOT)}: remove it and rotate the key")
        except OSError:
            pass

for w in warnings: print("WARN ", w)
for e in errors: print("ERROR", e)
failed = bool(errors) or (RELEASE and bool(warnings))
print("brand check:", "FAILED" if failed else "OK", f"({len(errors)} errors, {len(warnings)} warnings{', release mode' if RELEASE else ''})")
sys.exit(1 if failed else 0)
