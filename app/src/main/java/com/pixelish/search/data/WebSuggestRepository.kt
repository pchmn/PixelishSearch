package com.pixelish.search.data

import android.util.LruCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.parameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Récupère les suggestions de recherche depuis l'endpoint public de Google Suggest.
 * C'est le même endpoint qu'utilise le navigateur Android.
 */
object WebSuggestRepository {

    private val client = HttpClient(Android) {
        engine {
            connectTimeout = 1500
            socketTimeout = 1500
        }
    }

    // Cache en mémoire : la majorité des frappes répètent un préfixe déjà vu
    // (l'utilisateur tape, efface, retape). Une réponse cachée = résultat instantané.
    private val cache = LruCache<String, List<String>>(64)

    /**
     * Préchauffe la connexion TLS vers Google Suggest pour éliminer le coût
     * DNS + TCP + TLS handshake (~200-400ms) du premier appel réel.
     * À appeler depuis Application.onCreate().
     */
    fun warmUp(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                client.get("https://www.google.com/complete/search?client=firefox&q=a")
                    .bodyAsText()
            }
        }
    }

    suspend fun fetch(query: String, limit: Int = 5): List<String> {
        if (query.isBlank()) return emptyList()

        val key = "$query|$limit"
        cache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val url = URLBuilder("https://www.google.com/complete/search").apply {
                    parameters.append("client", "firefox")
                    parameters.append("q", query)
                }.buildString()

                val response = client.get(url).bodyAsText()
                // Format: ["query", ["suggestion1", "suggestion2", ...]]
                val json = JSONArray(response)
                val suggestions = json.getJSONArray(1)

                val list = buildList {
                    for (i in 0 until minOf(suggestions.length(), limit)) {
                        add(suggestions.getString(i))
                    }
                }
                cache.put(key, list)
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
