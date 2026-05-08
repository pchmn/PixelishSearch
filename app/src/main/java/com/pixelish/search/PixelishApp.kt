package com.pixelish.search

import android.app.Application
import com.pixelish.search.data.AppIndex
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

        // Préchargement asynchrone de l'index
        appScope.launch {
            AppIndex.preload(this@PixelishApp)
        }
    }

    companion object {
        lateinit var instance: PixelishApp
            private set
    }
}
