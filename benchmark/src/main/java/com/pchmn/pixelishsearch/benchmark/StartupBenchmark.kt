package com.pchmn.pixelishsearch.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reproducible cold-start measurements.
 *
 * Run with:
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest
 *
 * Three variants are measured:
 *   - startupNone:        no AOT compilation — pessimistic baseline
 *   - startupBaselineProfile: AOT-compiled with our generated baseline profile
 *   - startupFull:        fully AOT-compiled — optimistic ceiling
 *
 * The delta between None and BaselineProfile is the actual user-facing win.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupNone() = startup(CompilationMode.None())

    @Test
    fun startupBaselineProfile() = startup(CompilationMode.Partial())

    @Test
    fun startupFull() = startup(CompilationMode.Full())

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = "com.pchmn.pixelishsearch",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = mode,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
