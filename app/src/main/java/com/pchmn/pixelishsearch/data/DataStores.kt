package com.pchmn.pixelishsearch.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.appHistoryDatastore by preferencesDataStore(name = "app_history")
val Context.searchHistoryDataStore by preferencesDataStore(name = "search_history")
val Context.contactHistoryDataStore by preferencesDataStore(name = "contact_history")