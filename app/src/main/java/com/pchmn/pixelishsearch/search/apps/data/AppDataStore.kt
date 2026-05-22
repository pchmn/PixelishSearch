package com.pchmn.pixelishsearch.search.apps.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.appIndexCacheDataStore by preferencesDataStore(name = "app_index_cache")
val Context.hiddenAppsDataStore by preferencesDataStore(name = "hidden_apps")