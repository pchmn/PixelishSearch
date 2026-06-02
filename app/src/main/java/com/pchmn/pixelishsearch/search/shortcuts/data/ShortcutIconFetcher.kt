package com.pchmn.pixelishsearch.search.shortcuts.data

import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.core.content.res.ResourcesCompat
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options

/**
 * Cache key for an App shortcut's own icon — a `@drawable` resource resolved
 * against the *declaring* app's resources (not an app launcher icon, so it can't
 * reuse [AppIconRequest][com.pchmn.pixelishsearch.search.apps.data.AppIconRequest]).
 * When a shortcut declares no icon (`iconResId == 0`), the UI passes an
 * `AppIconRequest` for the parent app instead. `lastUpdateTime` invalidates the
 * cache when the app ships a new version.
 */
data class ShortcutIconRequest(
    val packageName: String,
    val iconResId: Int,
    val lastUpdateTime: Long,
)

class ShortcutIconFetcher(
    private val request: ShortcutIconRequest,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val res = try {
            options.context.packageManager.getResourcesForApplication(request.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        val drawable = try {
            ResourcesCompat.getDrawable(res, request.iconResId, null)
        } catch (e: Resources.NotFoundException) {
            null
        } ?: return null
        return ImageFetchResult(
            image = drawable.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<ShortcutIconRequest> {
        override fun create(
            data: ShortcutIconRequest,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = ShortcutIconFetcher(data, options)
    }
}

class ShortcutIconKeyer : Keyer<ShortcutIconRequest> {
    override fun key(data: ShortcutIconRequest, options: Options): String =
        "shortcut-icon://${data.packageName}/${data.iconResId}/${data.lastUpdateTime}"
}
