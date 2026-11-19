# Testing

| What | Command | Notes |
|---|---|---|
| Scheduler, duration parser, channel parser | `./gradlew :app:testDebugUnitTest` | pure Kotlin, no device |
| Worker validation | `cd cloudflare/worker && node --test` | no dependencies |
| Brand config | `python3 scripts/check_brand.py [--release]` | contrast, name, icons, secrets |

Manual checks on a device/emulator (cannot be automated here):
1. First run shows the consent screen; "I agree" goes on; Settings -> Clear stored data returns to consent.
2. The video starts at the right offset; closing and reopening the app resumes in step with the schedule.
3. Two devices show the same video within a couple of seconds.
4. Airplane mode after first load still shows the cached lineup; the player shows its own error offline.
5. A removed or private video is skipped automatically.
6. Android TV: every button reachable with the D-pad; focus ring visible; legal text scrolls.
