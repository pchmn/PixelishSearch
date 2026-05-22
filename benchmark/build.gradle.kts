plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    // The test APK runs in its own process and targets the app under test.
    // The app's `benchmarkRelease` variant uses `applicationIdSuffix = ".benchmark"`
    // → app installs as `com.pchmn.pixelishsearch.benchmark`. So the test APK
    // gets a further `.test` suffix to avoid colliding on the device.
    namespace = "com.pchmn.pixelishsearch.benchmark.test"
    compileSdk = 37

    defaultConfig {
        minSdk = 31
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"

    // Required for the macrobenchmark runner.
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

baselineProfile {
    // Generate the profile on the same device the tests run on (no managed
    // device — simpler, just plug your phone in).
    useConnectedDevices = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0-alpha06")
}
