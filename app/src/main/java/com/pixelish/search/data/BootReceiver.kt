package com.pixelish.search.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Préchage l'index dès que le téléphone démarre, pour que le premier lancement
 * de l'app de recherche soit instantané.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.Default).launch {
                AppIndex.preload(context.applicationContext)
            }
        }
    }
}
