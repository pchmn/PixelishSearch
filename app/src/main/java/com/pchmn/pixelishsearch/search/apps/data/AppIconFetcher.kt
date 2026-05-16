package com.pchmn.pixelishsearch.search.apps.data

import android.content.pm.PackageManager
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options

/**
 * Cache key for an app icon. `lastUpdateTime` invalidates Coil's caches
 * automatically when the app gets updated and ships a new icon.
 */
data class AppIconRequest(
    val packageName: String,
    val lastUpdateTime: Long,
)

class AppIconFetcher(
    private val request: AppIconRequest,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val drawable = try {
            options.context.packageManager.getApplicationIcon(request.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        return ImageFetchResult(
            image = drawable.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<AppIconRequest> {
        override fun create(
            data: AppIconRequest,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = AppIconFetcher(data, options)
    }
}

class AppIconKeyer : Keyer<AppIconRequest> {
    override fun key(data: AppIconRequest, options: Options): String =
        "app-icon://${data.packageName}/${data.lastUpdateTime}"
}
