package com.pchmn.pixelishsearch.update.data

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
 * Parsed semver-ish version: a numeric core ("1.2.3") plus an optional list of
 * pre-release identifiers ("beta", "2"). Build metadata (after "+") is dropped
 * since semver says it has no precedence.
 */
data class SemVer(
    val core: List<Int>,
    val preRelease: List<String>,
)

/**
 * Strips a leading "v" and parses the rest as semver: dot-separated integer
 * core, optional `-pre.release.ids` suffix, optional `+buildmetadata` (ignored).
 * Returns null if the core can't be parsed as integers.
 */
fun parseVersion(raw: String): SemVer? {
    val cleaned = raw.trim().removePrefix("v").removePrefix("V").substringBefore('+')
    val dash = cleaned.indexOf('-')
    val coreStr = if (dash < 0) cleaned else cleaned.substring(0, dash)
    val preStr = if (dash < 0) "" else cleaned.substring(dash + 1)
    val core = coreStr.split('.').map { it.toIntOrNull() ?: return null }
    if (core.isEmpty()) return null
    val preRelease = if (preStr.isEmpty()) emptyList() else preStr.split('.')
    return SemVer(core, preRelease)
}

/**
 * Compares two versions using semver precedence rules:
 *  - Compare core numerically; missing trailing segments are zero.
 *  - If core is equal, a version *without* a pre-release suffix outranks one
 *    *with* a suffix (so `1.0.0` > `1.0.0-rc.1`).
 *  - When both have pre-release suffixes, identifiers are compared field by
 *    field: numeric vs numeric numerically; numeric is lower than alphanumeric;
 *    alphanumeric compared lexically; a shorter list of fields is lower than a
 *    longer one if all preceding fields match.
 *
 * Returns negative if `a < b`, zero if equal, positive if `a > b`.
 */
fun compareVersions(a: String, b: String): Int {
    val pa = parseVersion(a) ?: return -1
    val pb = parseVersion(b) ?: return 1

    val coreLen = maxOf(pa.core.size, pb.core.size)
    for (i in 0 until coreLen) {
        val cmp = pa.core.getOrElse(i) { 0 }.compareTo(pb.core.getOrElse(i) { 0 })
        if (cmp != 0) return cmp
    }

    val aHasPre = pa.preRelease.isNotEmpty()
    val bHasPre = pb.preRelease.isNotEmpty()
    if (!aHasPre && !bHasPre) return 0
    if (!aHasPre) return 1
    if (!bHasPre) return -1

    val preLen = maxOf(pa.preRelease.size, pb.preRelease.size)
    for (i in 0 until preLen) {
        val ai = pa.preRelease.getOrNull(i) ?: return -1
        val bi = pb.preRelease.getOrNull(i) ?: return 1
        val aNum = ai.toIntOrNull()
        val bNum = bi.toIntOrNull()
        val cmp = when {
            aNum != null && bNum != null -> aNum.compareTo(bNum)
            aNum != null -> -1
            bNum != null -> 1
            else -> ai.compareTo(bi)
        }
        if (cmp != 0) return cmp
    }
    return 0
}
