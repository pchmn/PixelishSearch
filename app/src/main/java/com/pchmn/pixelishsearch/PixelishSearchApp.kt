package com.pchmn.pixelishsearch

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.pchmn.pixelishsearch.settings.data.SettingsRepository
import com.pchmn.pixelishsearch.search.apps.data.AppHistoryRepository
import com.pchmn.pixelishsearch.search.apps.data.AppIconFetcher
import com.pchmn.pixelishsearch.search.apps.data.AppIconKeyer
import com.pchmn.pixelishsearch.search.apps.data.AppIndex
import com.pchmn.pixelishsearch.search.apps.data.HiddenAppsRepository
import com.pchmn.pixelishsearch.search.contacts.data.ContactHistoryRepository
import com.pchmn.pixelishsearch.search.contacts.data.ContactRepository
import com.pchmn.pixelishsearch.search.web.data.WebSearchHistoryRepository
import com.pchmn.pixelishsearch.search.web.data.WebSuggestionsRepository
import com.pchmn.pixelishsearch.update.data.UpdateChecker
import com.pchmn.pixelishsearch.update.data.UpdateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application class. Preloads the apps index as soon as the process is created,
 * so the search Activity has nothing to do at launch.
 * This is the key to beating PixelSearch on cold start speed.
 */
class PixelishSearchApp : Application(), SingletonImageLoader.Factory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var appHistory: AppHistoryRepository
        private set
    lateinit var searchHistory: WebSearchHistoryRepository
        private set
    lateinit var contactHistory: ContactHistoryRepository
        private set
    lateinit var hiddenApps: HiddenAppsRepository
        private set
    lateinit var settings: SettingsRepository
        private set
    lateinit var updates: UpdateRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Eagerly construct repositories so their StateFlows start collecting
        // from DataStore right away — scores/history are ready when the
        // search screen opens.
        appHistory = AppHistoryRepository(this, appScope)
        searchHistory = WebSearchHistoryRepository(this, appScope)
        contactHistory = ContactHistoryRepository(this, appScope)
        hiddenApps = HiddenAppsRepository(this, appScope)
        settings = SettingsRepository(this, appScope)
        updates = UpdateRepository(this, appScope)

        // Async preload of the index
        AppIndex.preload(this@PixelishSearchApp, appScope)

        // Warm up the TLS connection to Google Suggest so the first real call
        // doesn't have to pay DNS + TCP + TLS handshake cost.
        WebSuggestionsRepository.warmUp(appScope)

        // Wake the (out-of-process) Contacts provider so the first keystroke
        // doesn't pay the binder + provider startup cost.
        ContactRepository.warmUp(this, appScope)

        // Background check for a new GitHub release; persists the result so the
        // banner is available instantly on the next cold start.
        UpdateChecker.check(appScope, updates, currentVersionName())
    }

    private fun currentVersionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
            .getOrNull()
            .orEmpty()

    /**
     * Coil's singleton ImageLoader, wired with our custom keyer + fetcher so that
     * `AsyncImage(model = AppIconRequest(pkg, lastUpdateTime))` resolves icons
     * through PackageManager once and caches them on disk thereafter.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(AppIconKeyer())
                add(AppIconFetcher.Factory())
            }
            .build()

    companion object {
        lateinit var instance: PixelishSearchApp
            private set
    }
}
