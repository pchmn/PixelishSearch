package com.pchmn.pixelishsearch

import android.app.Application
import androidx.tracing.trace
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.pchmn.pixelishsearch.core.ui.theme.warmUpGoogleSans
import com.pchmn.pixelishsearch.preferences.data.PreferencesRepository
import com.pchmn.pixelishsearch.search.apps.data.AppHistoryRepository
import com.pchmn.pixelishsearch.search.apps.data.AppIconFetcher
import com.pchmn.pixelishsearch.search.apps.data.AppIconKeyer
import com.pchmn.pixelishsearch.search.apps.data.AppIndex
import com.pchmn.pixelishsearch.search.apps.data.HiddenAppsRepository
import com.pchmn.pixelishsearch.search.calendar.data.CalendarRepository
import com.pchmn.pixelishsearch.search.contacts.data.ContactHistoryRepository
import com.pchmn.pixelishsearch.search.contacts.data.ContactRepository
import com.pchmn.pixelishsearch.search.settings.data.FlashlightController
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageHistoryRepository
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageIndex
import com.pchmn.pixelishsearch.search.shortcuts.data.ShortcutHistoryRepository
import com.pchmn.pixelishsearch.search.shortcuts.data.ShortcutIndex
import com.pchmn.pixelishsearch.search.shortcuts.data.ShortcutIconFetcher
import com.pchmn.pixelishsearch.search.shortcuts.data.ShortcutIconKeyer
import com.pchmn.pixelishsearch.search.web.data.WebSearchHistoryRepository
import com.pchmn.pixelishsearch.search.web.data.WebSuggestionsRepository
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
    lateinit var settingsPageHistory: SettingsPageHistoryRepository
        private set
    lateinit var shortcutHistory: ShortcutHistoryRepository
        private set
    lateinit var hiddenApps: HiddenAppsRepository
        private set
    lateinit var preferences: PreferencesRepository
        private set
    lateinit var updates: UpdateRepository
        private set

    override fun onCreate() = trace("PixelishSearchApp.onCreate") {
        super.onCreate()
        instance = this

        // Eagerly construct repositories so their StateFlows start collecting
        // from DataStore right away — scores/history are ready when the
        // search screen opens.
        trace("repos.construct") {
            appHistory = AppHistoryRepository(this, appScope)
            searchHistory = WebSearchHistoryRepository(this, appScope)
            contactHistory = ContactHistoryRepository(this, appScope)
            settingsPageHistory = SettingsPageHistoryRepository(this, appScope)
            shortcutHistory = ShortcutHistoryRepository(this, appScope)
            hiddenApps = HiddenAppsRepository(this, appScope)
            preferences = PreferencesRepository(this, appScope)
            updates = UpdateRepository(this, appScope)
        }

        // Phase A only — hydrate the three indexes from their persisted disk
        // caches (cheap DataStore reads, no PackageManager / getResources). This
        // feeds both the typed app results AND the blank-state recents block
        // (recent apps / shortcuts / settings pages) on the first frame. The
        // expensive phase B (re-enumeration / re-parse, which loads other
        // packages' resources) is deferred to MainActivity — see the NOTE below.
        trace("AppIndex.preloadFromCache.dispatch") {
            AppIndex.preloadFromCache(this@PixelishSearchApp, appScope)
        }
        trace("ShortcutIndex.preloadFromCache.dispatch") {
            ShortcutIndex.preloadFromCache(this@PixelishSearchApp, appScope)
        }
        trace("SettingsPageIndex.preloadFromCache.dispatch") {
            SettingsPageIndex.preloadFromCache(appScope, this@PixelishSearchApp)
        }

        // Warm up the TLS connection to Google Suggest so the first real call
        // doesn't have to pay DNS + TCP + TLS handshake cost.
        trace("WebSuggestionsRepository.warmUp.dispatch") {
            WebSuggestionsRepository.warmUp(appScope)
        }

        // Wake the (out-of-process) Contacts provider so the first keystroke
        // doesn't pay the binder + provider startup cost.
        trace("ContactRepository.warmUp.dispatch") {
            ContactRepository.warmUp(this, appScope)
        }

        // Same for the (out-of-process) Calendar provider.
        trace("CalendarRepository.warmUp.dispatch") {
            CalendarRepository.warmUp(this, appScope)
        }

        // Register the torch callback now so the flashlight tile shows the
        // correct on/off state the first time it appears (Android delivers
        // one onTorchModeChanged per camera right after registration).
        trace("FlashlightController.warmUp.dispatch") {
            FlashlightController.warmUp(this, appScope)
        }

        // Pre-resolve the GoogleSans device font so the first text layout
        // doesn't pay the lookup during the heavy post-first-frame composition
        // (which delays WINDOW_FOCUS_CHANGED → the IME). See ADR-0009 notes /
        // docs/performance-analysis.md.
        trace("warmUpGoogleSans.dispatch") {
            warmUpGoogleSans(appScope, this@PixelishSearchApp)
        }

        // NOTE: the phase-B refreshes of all three indexes (AppIndex.refresh,
        // ShortcutIndex.refresh, SettingsPageIndex.refresh) are deliberately NOT
        // dispatched here. Their cross-package getResources (every app's
        // resources via loadLabel, the Settings .arsc + RRO overlay cascade,
        // each shortcut-declaring app's resources) contends with the first-frame
        // composition on ART locks (see docs/performance-analysis.md, ADR-0009).
        // The phase-A cache hydrates above already feed the first frame, so the
        // authoritative re-scan can wait: MainActivity kicks the refreshes off
        // (on appScope) once the first content frame is actually drawn.
    }

    /**
     * Background scope shared with `MainActivity` for the GitHub update check
     * (triggered on every search reopen, throttled inside `UpdateChecker`).
     */
    val backgroundScope: CoroutineScope get() = appScope

    fun currentVersionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
            .getOrNull()
            .orEmpty()

    /**
     * Coil's singleton ImageLoader, wired with our custom keyer + fetcher so that
     * `AsyncImage(model = AppIconRequest(pkg, lastUpdateTime))` resolves icons
     * through PackageManager once and caches them on disk thereafter.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        trace("Coil.newImageLoader") {
            ImageLoader.Builder(context)
                .components {
                    add(AppIconKeyer())
                    add(AppIconFetcher.Factory())
                    add(ShortcutIconKeyer())
                    add(ShortcutIconFetcher.Factory())
                }
                .build()
        }

    companion object {
        lateinit var instance: PixelishSearchApp
            private set
    }
}
