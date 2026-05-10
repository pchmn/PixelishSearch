package com.pixelish.search

import android.app.Application
import com.pixelish.search.data.AppIndex
import com.pixelish.search.data.AppUsageRepository
import com.pixelish.search.data.ContactHistoryRepository
import com.pixelish.search.data.SearchHistoryRepository
import com.pixelish.search.data.WebSuggestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class. Pré-charge l'index des apps dès la création du process,
 * pour que l'Activity de recherche n'ait rien à faire au démarrage.
 * C'est la clé pour battre PixelSearch en vitesse de cold start.
 */
class PixelishApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Init des compteurs d'usage : on lance la collecte du DataStore tout de suite
        // pour que les scores soient prêts quand l'écran de recherche s'ouvre.
        AppUsageRepository.init(this)
        SearchHistoryRepository.init(this)
        ContactHistoryRepository.init(this)

        // Préchargement asynchrone de l'index
        appScope.launch {
            AppIndex.preload(this@PixelishApp)
        }

        // Préchauffe la connexion TLS vers Google Suggest pour que le premier
        // appel réel n'ait pas à payer DNS + TCP + TLS handshake.
        WebSuggestRepository.warmUp(appScope)
    }

    companion object {
        lateinit var instance: PixelishApp
            private set
    }
}
