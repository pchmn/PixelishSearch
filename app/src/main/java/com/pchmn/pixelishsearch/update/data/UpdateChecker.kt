package com.pchmn.pixelishsearch.update.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object UpdateChecker {

    private const val TAG = "UpdateChecker"

    fun check(scope: CoroutineScope, repo: UpdateRepository, currentVersion: String) {
        scope.launch(Dispatchers.IO) {
            val release = GithubReleaseApi.fetchLatest() ?: return@launch
            if (release.draft || release.prerelease) return@launch

            val tag = release.tagName
            if (compareVersions(tag, currentVersion) <= 0) {
                repo.clear()
                return@launch
            }

            val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            if (apk == null) {
                Log.w(TAG, "Release $tag has no APK asset")
                return@launch
            }

            repo.setAvailable(
                UpdateInfo(
                    versionName = tag.removePrefix("v").removePrefix("V"),
                    changelog = release.body.orEmpty(),
                    downloadUrl = apk.browserDownloadUrl,
                )
            )
        }
    }
}
