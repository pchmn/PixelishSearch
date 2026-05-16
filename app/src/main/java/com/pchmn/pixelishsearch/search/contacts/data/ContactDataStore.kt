package com.pchmn.pixelishsearch.search.contacts.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.contactHistoryDataStore by preferencesDataStore(name = "contact_history")