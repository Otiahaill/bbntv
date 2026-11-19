import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

// API key lookup order: local.properties -> -PyoutubeApiKey / gradle.properties -> env YOUTUBE_API_KEY -> empty.
// An empty key is valid: the app then asks the user for their own key on first run.
// NOTE: any key compiled into the APK can be extracted. Restrict it in Google Cloud.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
// Project identity (applicationId, version) comes from brand/brand.properties.
val brand = Properties().apply {
    rootProject.file("brand/brand.properties").inputStream().use { load(it) }
}

val youtubeApiKey: String = (
    localProps.getProperty("youtubeApiKey")
        ?: providers.gradleProperty("youtubeApiKey").orNull
        ?: System.getenv("YOUTUBE_API_KEY")
        ?: ""
    ).trim()

android {
    namespace = "com.example.prtv"
    compileSdk = 34

    defaultConfig {
        applicationId = brand.getProperty("applicationId")
        minSdk = 23            // EncryptedSharedPreferences requires API 23+
        targetSdk = 34
        versionCode = brand.getProperty("versionCode").toInt()
        versionName = brand.getProperty("versionName")
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
    }

    // Everything brand-specific (name, channel, colours, icons) lives in /brand/res.
    sourceSets.getByName("main").res.srcDir(rootProject.file("brand/res"))

    buildTypes {
        release {
            isMinifyEnabled = true
            // Resources are looked up by name for the optional official YouTube logo,
            // so resource shrinking stays off.
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"   // matches Kotlin 1.9.24
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

kapt { correctErrorTypes = true }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation("junit:junit:4.13.2")
}
