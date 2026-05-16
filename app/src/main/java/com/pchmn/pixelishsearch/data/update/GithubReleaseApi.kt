package com.pchmn.pixelishsearch.data.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0,
)

object GithubReleaseApi {
    private const val OWNER = "pchmn"
    private const val REPO = "PixelishSearch"
    private const val LATEST_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private val client = HttpClient(Android) {
        engine {
            connectTimeout = 5_000
            socketTimeout = 10_000
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun fetchLatest(): GithubRelease? = withContext(Dispatchers.IO) {
        runCatching {
            val raw = client.get(LATEST_URL) {
                header("Accept", "application/vnd.github+json")
                header("X-GitHub-Api-Version", "2022-11-28")
            }.bodyAsText()
            json.decodeFromString(GithubRelease.serializer(), raw)
        }.getOrNull()
    }
}

/**
 * Strips a leading "v" and parses the rest as dot-separated integers. Non-numeric
 * suffixes (e.g. "-rc1") are dropped at the segment where they appear.
 * Returns null if no leading numeric segment is found.
 */
fun parseVersion(raw: String): List<Int>? {
    val cleaned = raw.trim().removePrefix("v").removePrefix("V")
    val parts = cleaned.split('.').mapNotNull { part ->
        val numeric = part.takeWhile { it.isDigit() }
        if (numeric.isEmpty()) null else numeric.toIntOrNull()
    }
    return parts.takeIf { it.isNotEmpty() }
}

/**
 * Compares two version strings ("1.2.3" vs "1.2.10"). Missing trailing segments
 * count as zero ("1.2" == "1.2.0"). Returns negative if `a < b`, zero if equal,
 * positive if `a > b`.
 */
fun compareVersions(a: String, b: String): Int {
    val pa = parseVersion(a) ?: return -1
    val pb = parseVersion(b) ?: return 1
    val n = maxOf(pa.size, pb.size)
    for (i in 0 until n) {
        val da = pa.getOrElse(i) { 0 }
        val db = pb.getOrElse(i) { 0 }
        if (da != db) return da - db
    }
    return 0
}
