package com.pchmn.pixelishsearch.search.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pchmn.pixelishsearch.PixelishSearchApp
import com.pchmn.pixelishsearch.core.data.HistoryEntry
import com.pchmn.pixelishsearch.search.apps.data.AppEntry
import com.pchmn.pixelishsearch.search.apps.data.AppIndex
import com.pchmn.pixelishsearch.search.contacts.data.ContactAction
import com.pchmn.pixelishsearch.search.contacts.data.ContactEntry
import com.pchmn.pixelishsearch.search.contacts.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.search.contacts.data.ContactRepository
import com.pchmn.pixelishsearch.search.settings.data.FlashlightController
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageEntry
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageHistoryEntry
import com.pchmn.pixelishsearch.search.settings.data.SettingsPageIndex
import com.pchmn.pixelishsearch.search.settings.data.SettingsTileId
import com.pchmn.pixelishsearch.search.settings.data.SettingsTileRepository
import com.pchmn.pixelishsearch.search.settings.data.SettingsTileResult
import com.pchmn.pixelishsearch.search.settings.data.isActive
import com.pchmn.pixelishsearch.search.web.data.WebSearchHistoryEntry
import com.pchmn.pixelishsearch.search.web.data.WebSuggestionsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Item rendered in the fused blank-state recents block. Storage stays
 * per-feature (`ContactHistoryRepository`, `SettingsPageHistoryRepository`) —
 * this wrapper only exists to drive a single ranked list at the UI layer.
 * See `docs/adr/0002-merge-contacts-and-settings-recents-in-display.md`.
 */
sealed interface RecentEntity {
    val entry: HistoryEntry
    data class Contact(override val entry: ContactHistoryEntry) : RecentEntity
    data class SettingsPage(override val entry: SettingsPageHistoryEntry) : RecentEntity
}

data class SearchUiState(
    val query: String = "",
    val appRecents: List<AppEntry> = emptyList(),
    val appResults: List<AppEntry> = emptyList(),
    val fusedRecents: List<RecentEntity> = emptyList(),
    val contactResults: List<ContactEntry> = emptyList(),
    val webRecents: List<String> = emptyList(),
    val webResults: List<String> = emptyList(),
    val tileResults: List<SettingsTileResult> = emptyList(),
    val settingsPageResults: List<SettingsPageEntry> = emptyList(),
)

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PixelishSearchApp
    private val appHistory get() = app.appHistory
    private val searchHistory get() = app.searchHistory
    private val contactHistory get() = app.contactHistory
    private val settingsPageHistory get() = app.settingsPageHistory
    private val hiddenApps get() = app.hiddenApps
    private val preferences get() = app.preferences

    private val _query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var webJob: Job? = null

    init {
        // Default suggestions: top 4 apps by usage score (time decay),
        // tiebreaker = alphabetical order. Combining on appHistory.byKey makes
        // the lambda re-fire whenever usage data refreshes.
        viewModelScope.launch {
            combine(
                AppIndex.apps,
                appHistory.byKey,
                hiddenApps.hidden,
            ) { apps, _, hidden ->
                appHistory.ranked(apps.filterNot { it.packageName in hidden }).take(4)
            }.collect { suggested ->
                _uiState.value = _uiState.value.copy(appRecents = suggested)
            }
        }

        viewModelScope.launch {
            searchHistory.history.collect { entries ->
                _uiState.value = _uiState.value.copy(
                    webRecents = entries.map { it.query }
                )
            }
        }

        // Fused blank-state recents: contacts + settings pages merged into a
        // single block, ranked by HistoryEntry.score(), capped at 2. Stale
        // settings entries (component no longer in SettingsPageIndex) are
        // filtered at display time, not deleted.
        viewModelScope.launch {
            combine(
                contactHistory.recents,
                settingsPageHistory.recents,
                SettingsPageIndex.entries,
                preferences.contactSearchEnabled,
            ) { contactRecents, pageRecents, pageEntries, contactsEnabled ->
                val known = pageEntries.mapTo(HashSet(pageEntries.size)) { it.component }
                val now = System.currentTimeMillis()
                val contacts = if (contactsEnabled) {
                    contactRecents.map { RecentEntity.Contact(it) }
                } else emptyList()
                val pages = pageRecents
                    .filter { it.component in known }
                    .map { RecentEntity.SettingsPage(it) }
                (contacts + pages)
                    .sortedWith(
                        compareByDescending<RecentEntity> { it.entry.score(now) }
                            .thenByDescending { it.entry.lastUsedEpochMillis }
                    )
                    .take(2)
            }.collect { fused ->
                _uiState.value = _uiState.value.copy(fusedRecents = fused)
            }
        }

        // Re-run the local search when the contact-search toggle flips so
        // results / recents reflect the new state without waiting for the
        // next keystroke.
        viewModelScope.launch {
            preferences.contactSearchEnabled.collect {
                runLocalSearch(_query.value)
            }
        }

        // Same reactivity for tile visibility — the user can hide/show tiles
        // in Settings while the search activity is still alive.
        viewModelScope.launch {
            preferences.disabledTileIds.collect {
                runLocalSearch(_query.value)
            }
        }

        // Instant local search on every keystroke (apps + contacts are fast)
        viewModelScope.launch {
            _query
                .onEach { runLocalSearch(it) }
                .debounce(90) // debounce network calls only
                .collect { runWebSearch(it) }
        }

        // Flashlight is the only tile that toggles in-process (the others
        // dismiss the activity), so its `isActive` snapshot would stay stale
        // after a tap. Patch the FLASHLIGHT tile in `tileResults` whenever the
        // torch state changes.
        viewModelScope.launch {
            FlashlightController.isOn.collect { on ->
                val current = _uiState.value
                val patched = current.tileResults.map { result ->
                    if (result.tile.id == SettingsTileId.FLASHLIGHT && result.isActive != on) {
                        result.copy(isActive = on)
                    } else result
                }
                if (patched !== current.tileResults) {
                    _uiState.value = current.copy(tileResults = patched)
                }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    /**
     * Clear the current query. Used when the activity is re-brought to the
     * foreground via singleTask so the user doesn't see their previous search.
     */
    fun reset() {
        _query.value = ""
    }

    fun onAppLaunched(packageName: String) {
        viewModelScope.launch { appHistory.record(packageName) }
    }

    fun onSearchLaunched(query: String) {
        viewModelScope.launch { searchHistory.record(query) }
    }

    fun onContactUsed(contact: ContactEntry, action: ContactAction) {
        viewModelScope.launch {
            contactHistory.record(
                id = contact.id,
                name = contact.name,
                photoUri = contact.photoUri,
                phoneNumber = contact.phoneNumber,
                action = action,
            )
        }
    }

    fun onSettingsPageOpened(entry: SettingsPageEntry) {
        viewModelScope.launch {
            settingsPageHistory.record(component = entry.component, label = entry.label)
        }
    }

    fun removeSearchHistory(query: String) {
        viewModelScope.launch { searchHistory.remove(WebSearchHistoryEntry(query)) }
    }

    fun hideAppFromRecents(packageName: String) {
        viewModelScope.launch { hiddenApps.hide(packageName) }
    }

    fun removeRecentContact(entry: ContactHistoryEntry) {
        viewModelScope.launch { contactHistory.remove(entry) }
    }

    fun removeRecentSettingsPage(entry: SettingsPageHistoryEntry) {
        viewModelScope.launch { settingsPageHistory.remove(entry) }
    }

    /**
     * Re-snapshot every tile's `isActive`. Activity stays alive when a tile is
     * tapped (we don't call `finish()` on click), so when the user comes back
     * from the Settings screen the previous snapshot is stale. Cheap — all
     * reads are in-memory or quick Settings.* lookups.
     */
    fun refreshTileStates() {
        val current = _uiState.value
        if (current.tileResults.isEmpty()) return
        val context = getApplication<Application>()
        var changed = false
        val updated = current.tileResults.map { result ->
            val active = result.tile.id.isActive(context)
            if (active != result.isActive) {
                changed = true
                result.copy(isActive = active)
            } else result
        }
        if (changed) _uiState.value = current.copy(tileResults = updated)
    }

    private fun runLocalSearch(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                query = query,
                appResults = emptyList(),
                contactResults = emptyList(),
                webResults = emptyList(),
                tileResults = emptyList(),
                settingsPageResults = emptyList(),
            )
            return
        }

        val now = System.currentTimeMillis()
        val appScores = appHistory.byKey.value
        val contactScores = contactHistory.byKey.value
        val pageScores = settingsPageHistory.byKey.value

        val apps = AppIndex.search(query, limit = 6) { pkg ->
            appScores[pkg]?.score(now) ?: 0f
        }
        val contacts = if (preferences.contactSearchEnabled.value) {
            ContactRepository.search(getApplication(), query, limit = 3) { id ->
                contactScores[id]?.score(now) ?: 0f
            }
        } else emptyList()
        val tiles = SettingsTileRepository.search(
            getApplication(),
            query,
            preferences.disabledTileIds.value,
            limit = 4,
        )
        val pages = SettingsPageIndex.search(query, limit = 3) { component ->
            pageScores[component]?.score(now) ?: 0f
        }

        _uiState.value = _uiState.value.copy(
            query = query,
            appResults = apps,
            contactResults = contacts,
            tileResults = tiles,
            settingsPageResults = pages,
        )
    }

    private fun runWebSearch(query: String) {
        webJob?.cancel()
        if (query.isBlank()) return
        webJob = viewModelScope.launch {
            val suggestions = WebSuggestionsRepository.fetch(query, limit = 5)
            // Make sure the query hasn't changed in the meantime
            if (_query.value == query) {
                _uiState.value = _uiState.value.copy(webResults = suggestions)
            }
        }
    }

}
