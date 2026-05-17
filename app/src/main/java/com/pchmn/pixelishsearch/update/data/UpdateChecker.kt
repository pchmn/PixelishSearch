package com.pchmn.pixelishsearch.update.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Outcome of a single GitHub-release check. Returned by [UpdateChecker.checkNow]
 * for callers that want to react synchronously (e.g. the "Check for updates"
 * button in Settings); [UpdateChecker.check] fires-and-forgets and just writes
 * to the repository.
 */
sealed interface CheckOutcome {
    data class Available(val info: UpdateInfo) : CheckOutcome
    data object UpToDate : CheckOutcome
    data object Failed : CheckOutcome
}

object UpdateChecker {

    private const val TAG = "UpdateChecker"

    // Minimum gap between two automatic checks. The fire-and-forget
    // [check] is called from `MainActivity.onCreate` / `onNewIntent`
    // (i.e. every time the user opens the search), so we throttle to avoid
    // spamming the GitHub API. Manual "Check for updates" from Settings goes
    // through [checkNow] and bypasses the throttle.
    private const val MIN_INTERVAL_MS = 6L * 60 * 60 * 1000  // 6 hours

    @Volatile
    private var lastCheckedAt = 0L

    /**
     * Throttled fire-and-forget check. No-op if a check ran successfully less
     * than [MIN_INTERVAL_MS] ago. On network failure the throttle is reset so
     * the next call retries immediately instead of waiting six hours.
     */
    @Synchronized
    fun check(scope: CoroutineScope, repo: UpdateRepository, currentVersion: String) {
        val now = System.currentTimeMillis()
        if (now - lastCheckedAt < MIN_INTERVAL_MS) return
        lastCheckedAt = now

        scope.launch(Dispatchers.IO) {
            if (checkNow(repo, currentVersion) is CheckOutcome.Failed) {
                lastCheckedAt = 0L
            }
        }
    }

    /**
     * Synchronous (suspend) check. Bypasses the throttle and reports the
     * outcome to the caller so the UI can show "checking…" / "up to date" /
     * "check failed" feedback. Also updates [repo] as a side-effect.
     */
    suspend fun checkNow(
        repo: UpdateRepository,
        currentVersion: String,
    ): CheckOutcome = withContext(Dispatchers.IO) {
        val release = when (val result = GithubReleaseApi.fetchLatest()) {
            is LatestReleaseResult.Found -> result.release
            LatestReleaseResult.NotFound -> {
                // No releases on GitHub anymore (all deleted) — drop any stale
                // cached UpdateInfo so the badge disappears.
                repo.clear()
                return@withContext CheckOutcome.UpToDate
            }
            LatestReleaseResult.Error -> return@withContext CheckOutcome.Failed
        }
        if (release.draft || release.prerelease) {
            return@withContext CheckOutcome.UpToDate
        }

        val tag = release.tagName
        if (compareVersions(tag, currentVersion) <= 0) {
            repo.clear()
            return@withContext CheckOutcome.UpToDate
        }

        val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        if (apk == null) {
            Log.w(TAG, "Release $tag has no APK asset")
            return@withContext CheckOutcome.UpToDate
        }

        val info = UpdateInfo(
            versionName = tag.removePrefix("v").removePrefix("V"),
            changelog = release.body.orEmpty(),
            downloadUrl = apk.browserDownloadUrl,
        )
        repo.setAvailable(info)
        CheckOutcome.Available(info)
    }
}
