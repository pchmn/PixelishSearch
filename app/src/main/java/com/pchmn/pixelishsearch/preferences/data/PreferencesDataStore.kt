package com.pchmn.pixelishsearch.preferences.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// Keep `name = "settings"` — the on-disk file name is part of the storage
// contract. Renaming it would silently wipe the preferences of existing
// installs on upgrade.
val Context.preferencesDataStore by preferencesDataStore(name = "settings")
