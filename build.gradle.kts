// Top-level build file. Plugin versions are pinned here and must stay mutually compatible:
//   AGP 8.5.x      -> needs Gradle 8.7+ and JDK 17
//   Kotlin 1.9.24  -> pairs with Compose compiler 1.5.14 (see app/build.gradle.kts)
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
