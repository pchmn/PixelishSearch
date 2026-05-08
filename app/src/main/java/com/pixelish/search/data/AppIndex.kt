package com.pixelish.search.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppEntry(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val launchIntent: Intent
) {
    // Label en minuscules sans accents pour matcher rapidement
    val normalizedLabel: String = label.lowercase()
        .replace("[àáâãäå]".toRegex(), "a")
        .replace("[èéêë]".toRegex(), "e")
        .replace("[ìíîï]".toRegex(), "i")
        .replace("[òóôõö]".toRegex(), "o")
        .replace("[ùúûü]".toRegex(), "u")
        .replace("[ç]".toRegex(), "c")
}

/**
 * Singleton qui maintient l'index des apps en mémoire.
 * Préchargé au démarrage de l'app et au boot du téléphone.
 */
object AppIndex {

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    fun preload(context: Context) {
        if (_isLoaded.value) return

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        val entries = resolveInfos
            .mapNotNull { ri ->
                try {
                    val pkg = ri.activityInfo.packageName
                    // On exclut notre propre app
                    if (pkg == context.packageName) return@mapNotNull null

                    val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null

                    AppEntry(
                        label = ri.loadLabel(pm).toString(),
                        packageName = pkg,
                        icon = ri.loadIcon(pm),
                        launchIntent = launchIntent.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }

        _apps.value = entries
        _isLoaded.value = true
    }

    /**
     * Recherche fuzzy : commence par les apps dont le label commence par la query,
     * puis celles qui la contiennent. C'est le comportement de Pixel Launcher.
     */
    fun search(query: String, limit: Int = 8): List<AppEntry> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
            .replace("[àáâãäå]".toRegex(), "a")
            .replace("[èéêë]".toRegex(), "e")
            .replace("[ìíîï]".toRegex(), "i")
            .replace("[òóôõö]".toRegex(), "o")
            .replace("[ùúûü]".toRegex(), "u")
            .replace("[ç]".toRegex(), "c")

        val all = _apps.value
        val startsWith = mutableListOf<AppEntry>()
        val contains = mutableListOf<AppEntry>()

        for (entry in all) {
            when {
                entry.normalizedLabel.startsWith(q) -> startsWith += entry
                entry.normalizedLabel.contains(q) -> contains += entry
            }
            if (startsWith.size + contains.size >= limit * 2) break
        }

        return (startsWith + contains).take(limit)
    }
}
