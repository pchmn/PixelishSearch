package com.pchmn.pixelishsearch.update.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.updateDataStore by preferencesDataStore(name = "update")