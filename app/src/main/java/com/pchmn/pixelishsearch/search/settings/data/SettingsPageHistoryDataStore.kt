package com.pchmn.pixelishsearch.search.settings.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsPageHistoryDataStore by preferencesDataStore(name = "settings_page_history")
