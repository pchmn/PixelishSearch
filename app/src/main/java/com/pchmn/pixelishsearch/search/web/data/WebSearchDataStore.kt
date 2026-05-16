package com.pchmn.pixelishsearch.search.web.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.searchHistoryDataStore by preferencesDataStore(name = "search_history")