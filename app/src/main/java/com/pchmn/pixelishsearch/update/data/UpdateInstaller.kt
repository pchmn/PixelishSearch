package com.pchmn.pixelishsearch.update.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Downloads the APK on demand and hands it to the system installer. The APK
 * is cached in the app's external files dir (`updates/`), so a FileProvider
 * can expose it to the system installer without requesting any permission.
 *
 * Installation on Android 8+ requires the user to grant the
 * `REQUEST_INSTALL_PACKAGES` runtime permission via the system
 * "Install unknown apps" settings page (`ACTION_MANAGE_UNKNOWN_APP_SOURCES`).
 */
object UpdateInstaller {
    private const val UPDATES_DIR = "updates"
    private const val APK_FILENAME = "PixelishSearch-update.apk"

    private val client = HttpClient(Android) {
        engine {
            connectTimeout = 10_000
            socketTimeout = 60_000
        }
    }

    /**
     * Downloads the APK to `getExternalFilesDir("updates")`, deleting any
     * previous file in that directory first. Emits progress in `[0, 1]` via
     * `onProgress`; emits a final `1f` once the file is fully written.
     * Returns the resulting file, or null on any failure.
     */
    suspend fun download(
        context: Context,
        url: String,
        onProgress: suspend (Float) -> Unit = {},
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.getExternalFilesDir(null), UPDATES_DIR).apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, APK_FILENAME)

            client.prepareGet(url).execute { response ->
                val total = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                val channel = response.bodyAsChannel()
                target.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read == -1) break
                        if (read == 0) continue
                        out.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(downloaded.toFloat() / total)
                    }
                }
            }
            onProgress(1f)
            target
        }.getOrNull()
    }

    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /**
     * Opens the system "Install unknown apps" page scoped to this package, so
     * the user can flip the toggle and come back.
     */
    fun requestInstallPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Triggers the system package installer for the given APK. The file must
     * live under a path declared in `res/xml/file_provider_paths.xml`.
     */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
