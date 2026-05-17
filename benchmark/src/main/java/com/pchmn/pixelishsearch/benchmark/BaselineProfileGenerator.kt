package com.pchmn.pixelishsearch.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Generates a baseline profile for PixelishSearch.
 *
 * Run with:
 *   ./gradlew :benchmark:generateBaselineProfile
 *
 * The generated profile lands in:
 *   app/src/main/generated/baselineProfiles/baseline-prof.txt
 * and is automatically packaged into the release APK by the baselineprofile
 * consumer plugin applied to :app.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.pchmn.pixelishsearch",
        includeInStartupProfile = true,
    ) {
        // Launch the search Activity from a stopped state (cold start).
        pressHome()
        startActivityAndWait()

        // The first frame is already what we care about for cold start, but
        // letting the IME settle ensures the focus/recompose path is also
        // profiled.
        device.waitForIdle()
    }
}
