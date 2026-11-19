# What was wrong with the original zip, and what changed

**Security / policy**
- A live YouTube API key sat in `gradle.properties.local` and in `project.md` (public repo). Removed; rotate it.
  CI also uploaded an APK containing the key. CI now builds without one.
- Key logic overwrote a user's own key on every launch and ignored key rotation. Replaced (user key wins, never overwritten).

**Build**
- Compose compiler 1.5.14 needs Kotlin 1.9.24 (was 1.9.23); AGP 8.5 needs Gradle 8.7+ (a shim `gradlew` used whatever Gradle
  the machine had, and no wrapper jar existed). Now pinned and consistent; CI installs Gradle 8.7.
- The key file named `gradle.properties.local` is never read by Gradle, so the build key was always empty.
- Missing: data layer, DI modules, theme/resources, launcher icons, proguard file, hilt-navigation-compose, icons-extended.
- Manifest declared `MainActivity` twice; minSdk 21 vs EncryptedSharedPreferences (API 23).

**Code**
- Syntax errors (stray parentheses), wrong imports (grid, `NavController`, `KeyboardOptions`), nonexistent APIs
  (`windowSizeClass()`, `gridItems`, `NavArgument{}`), Material 2 calls in Material 3.
- ViewModels held plain `var`s but screens called `collectAsStateWithLifecycle()` on them: no compile, no recomposition.
- Navigation routes had no argument placeholders (crash); playlists/channel never passed along; scheduler never used;
  settings unreachable; theme switches inert; white text on a white background in light mode.
- Rx calls without error handling (crash on any network failure), callbacks on the wrong thread.
- Player ignored YouTube's Referer requirement and the lifecycle (leaked WebView, kept playing in background).
- Scheduler reshuffled at midnight mid-video with no day anchor and trusted the device clock.

**Verified here:** scheduler algorithm (Python port), Worker tests (11 passing), brand checker, static import/bracket checks.
**Not verified here:** Gradle/Kotlin compilation and on-device behaviour (no Kotlin compiler or Android SDK in this environment).
