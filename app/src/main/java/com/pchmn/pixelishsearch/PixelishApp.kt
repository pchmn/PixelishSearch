package com.pchmn.pixelishsearch

import android.app.Application
import com.pchmn.pixelishsearch.data.AppIndex
import com.pchmn.pixelishsearch.data.AppUsageRepository
import com.pchmn.pixelishsearch.data.ContactHistoryRepository
import com.pchmn.pixelishsearch.data.SearchHistoryRepository
import com.pchmn.pixelishsearch.data.WebSuggestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class. Preloads the apps index as soon as the process is created,
 * so the search Activity has nothing to do at launch.
 * This is the key to beating PixelSearch on cold start speed.
 */
class PixelishApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Init usage counters: start collecting from DataStore right away
        // so scores are ready when the search screen opens.
        AppUsageRepository.init(this)
        SearchHistoryRepository.init(this)
        ContactHistoryRepository.init(this)

        // Async preload of the index
        appScope.launch {
            AppIndex.preload(this@PixelishApp)
        }

        // Warm up the TLS connection to Google Suggest so the first real call
        // doesn't have to pay DNS + TCP + TLS handshake cost.
        WebSuggestRepository.warmUp(appScope)
    }

    companion object {
        lateinit var instance: PixelishApp
            private set
    }
}
