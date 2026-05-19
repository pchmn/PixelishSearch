package com.pchmn.pixelishsearch.search.settings.data

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton that owns the torch state. Tracked via [CameraManager.TorchCallback]
 * so the state stays in sync with external changes (Quick Settings tile,
 * hardware buttons).
 *
 * [warmUp] registers the callback eagerly at app startup so the first chip
 * render after process start has the right state. Android delivers a
 * `onTorchModeChanged` for each camera right after registration.
 */
internal object FlashlightController {
    @Volatile
    var isOn: Boolean = false
        private set

    private var registered: Boolean = false

    // Callbacks must be dispatched on a thread with a Looper — we register from
    // `Dispatchers.IO` which has none, so we pin the dispatch to the main Looper.
    // Body just sets a volatile boolean, so main-thread cost is negligible.
    private val mainHandler = Handler(Looper.getMainLooper())
    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            isOn = enabled
        }
    }

    fun warmUp(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val cm = context.getSystemService(CameraManager::class.java) ?: return@launch
            ensureRegistered(cm)
        }
    }

    fun toggle(context: Context): Boolean {
        val cm = context.getSystemService(CameraManager::class.java) ?: return false
        ensureRegistered(cm)
        val cameraId = runCatching {
            cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull() ?: return false
        return try {
            cm.setTorchMode(cameraId, !isOn)
            true
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    private fun ensureRegistered(cm: CameraManager) {
        if (registered) return
        val ok = runCatching { cm.registerTorchCallback(callback, mainHandler) }.isSuccess
        if (ok) registered = true
    }
}
