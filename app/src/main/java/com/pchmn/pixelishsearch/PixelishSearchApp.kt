package com.pchmn.pixelishsearch

import android.app.Application
import com.pchmn.pixelishsearch.data.AppHistoryRepository
import com.pchmn.pixelishsearch.data.AppIndex
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
class PixelishSearchApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var appHistory: AppHistoryRepository
        private set
    lateinit var searchHistory: SearchHistoryRepository
        private set
    lateinit var contactHistory: ContactHistoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Eagerly construct repositories so their StateFlows start collecting
        // from DataStore right away — scores/history are ready when the
        // search screen opens.
        appHistory = AppHistoryRepository(this)
        searchHistory = SearchHistoryRepository(this)
        contactHistory = ContactHistoryRepository(this)

        // Async preload of the index
        appScope.launch {
            AppIndex.preload(this@PixelishSearchApp)
        }

        // Warm up the TLS connection to Google Suggest so the first real call
        // doesn't have to pay DNS + TCP + TLS handshake cost.
        WebSuggestRepository.warmUp(appScope)
    }

    companion object {
        lateinit var instance: PixelishSearchApp
            private set
    }
}
