package com.pixelish.search.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Récupère les suggestions de recherche depuis l'endpoint public de Google Suggest.
 * C'est le même endpoint qu'utilise le navigateur Android.
 */
object WebSuggestRepository {

    private val client = HttpClient(Android)

    suspend fun fetch(query: String, limit: Int = 5): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val url = URLBuilder("https://www.google.com/complete/search").apply {
                parameters.append("client", "firefox")
                parameters.append("q", query)
            }.buildString()

            val response = client.get(url).bodyAsText()
            // Format: ["query", ["suggestion1", "suggestion2", ...]]
            val json = JSONArray(response)
            val suggestions = json.getJSONArray(1)

            val list = mutableListOf<String>()
            for (i in 0 until minOf(suggestions.length(), limit)) {
                list += suggestions.getString(i)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
