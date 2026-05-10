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
 * Fetches search suggestions from the public Google Suggest endpoint.
 * Same endpoint used by the Android browser.
 */
object WebSuggestRepository {

    private val client = HttpClient(Android) {
        engine {
            connectTimeout = 1500
            socketTimeout = 1500
        }
    }

    // In-memory cache: most keystrokes repeat a prefix already seen
    // (user types, deletes, retypes). A cached response = instant result.
    private val cache = LruCache<String, List<String>>(64)

    /**
     * Warms up the TLS connection to Google Suggest to eliminate the
     * DNS + TCP + TLS handshake cost (~200-400ms) on the first real call.
     * Call from Application.onCreate().
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
