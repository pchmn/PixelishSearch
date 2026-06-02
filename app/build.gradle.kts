import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.baselineprofile")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
}

android {
    namespace = "com.pchmn.pixelishsearch"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pchmn.pixelishsearch"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0-beta.4"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "PixelishDebug")
        }
        release {
            resValue("string", "app_name", "PixelishSearch")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // The androidx.baselineprofile plugin auto-creates `nonMinifiedRelease`
        // (used to generate the profile) and `benchmarkRelease` (used to measure
        // with the profile applied) via `initWith(release)` — see the
        // `afterEvaluate` block below for our overrides.
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Override the baselineprofile plugin's auto-created variants. Runs in
// `afterEvaluate` because the plugin uses `initWith(release)` to copy
// signingConfig + resValues *after* the `buildTypes { … }` block evaluates,
// so any in-block override would be silently clobbered.
//
// - `applicationIdSuffix = ".benchmark"`: gives the benchmark variants a
//   distinct package (`com.pchmn.pixelishsearch.benchmark`) so they coexist
//   with the production `release` install instead of overwriting it (which
//   would wipe DataStore + permissions and trigger an Auto Backup restore).
// - `signingConfig`: aligned with `release` so the benchmark APK is signed
//   with our release key (instead of the AGP default debug key) for consistent
//   profiling.
// - `app_name`: distinct launcher label so the variant is visually
//   distinguishable from `release` on the device.
afterEvaluate {
    listOf("benchmarkRelease", "nonMinifiedRelease").forEach { name ->
        android.buildTypes.findByName(name)?.apply {
            applicationIdSuffix = ".benchmark"
            if (keystorePropertiesFile.exists()) {
                signingConfig = android.signingConfigs.getByName("release")
            }
            resValue("string", "app_name", "PixelishBenchmark")
        }
    }
}

// Wire the baseline profile produced by :benchmark into all release-flavor
// variants of :app. The plugin generates `baseline-prof.txt` from the
// `BaselineProfileGenerator` test and packages it into the APK.
baselineProfile {
    // Cold-start use case: also generate a startup-profile so the runtime
    // pre-compiles the startup-critical methods even before the user opens the
    // search screen.
    saveInSrc = true
    automaticGenerationDuringBuild = false
}

dependencies {
    "baselineProfile"(project(":benchmark"))

    // Installs the bundled baseline profile into the ART runtime on first
    // launch (required so the profile actually takes effect).
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2026.05.00"))

    // Material 3 Expressive (alpha at the time of writing — check the latest version)
    implementation("androidx.compose.material3:material3:1.4.0-alpha15")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // Tracing (Trace.beginSection / endSection visible in Perfetto)
    implementation("androidx.tracing:tracing-ktx:1.2.0")

    // DataStore (persisted app usage counters)
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // Networking for web suggestions (lightweight, no Retrofit)
    implementation("io.ktor:ktor-client-android:3.4.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")

    // Coil 3 for app-icon caching (in-memory + on-disk PNG cache).
    // Custom fetcher resolves "AppIconRequest" via PackageManager; disk cache
    // means subsequent cold starts decode from data dir instead of the APK.
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
