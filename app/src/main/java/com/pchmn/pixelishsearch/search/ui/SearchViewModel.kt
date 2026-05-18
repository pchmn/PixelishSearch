package com.pchmn.pixelishsearch.search.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pchmn.pixelishsearch.PixelishSearchApp
import com.pchmn.pixelishsearch.search.apps.data.AppEntry
import com.pchmn.pixelishsearch.search.apps.data.AppHistoryEntry
import com.pchmn.pixelishsearch.search.apps.data.AppIndex
import com.pchmn.pixelishsearch.search.contacts.data.ContactAction
import com.pchmn.pixelishsearch.search.contacts.data.ContactEntry
import com.pchmn.pixelishsearch.search.contacts.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.search.contacts.data.ContactRepository
import com.pchmn.pixelishsearch.search.settings.data.SettingsTileRepository
import com.pchmn.pixelishsearch.search.settings.data.SettingsTileResult
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

data class SearchUiState(
    val query: String = "",
    val appRecents: List<AppEntry> = emptyList(),
    val appResults: List<AppEntry> = emptyList(),
    val contactRecents: List<ContactHistoryEntry> = emptyList(),
    val contactResults: List<ContactEntry> = emptyList(),
    val webRecents: List<String> = emptyList(),
    val webResults: List<String> = emptyList(),
    val tileResults: List<SettingsTileResult> = emptyList(),
)

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PixelishSearchApp
    private val appHistory get() = app.appHistory
    private val searchHistory get() = app.searchHistory
    private val contactHistory get() = app.contactHistory
    private val hiddenApps get() = app.hiddenApps
    private val settings get() = app.settings

    private val _query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Local snapshot of the app-launch history, kept in sync with the repository's
    // Flow. Read synchronously on every keystroke by runLocalSearch().
    private var historyByPkg: Map<String, AppHistoryEntry> = emptyMap()
    private var contactHistoryById: Map<Long, ContactHistoryEntry> = emptyMap()

    private var webJob: Job? = null

    init {
        // Default suggestions: top 4 apps by usage score (time decay),
        // tiebreaker = alphabetical order. Also refreshes the local cache used
        // by runLocalSearch.
        viewModelScope.launch {
            combine(
                AppIndex.apps,
                appHistory.recents,
                hiddenApps.hidden,
            ) { apps, history, hidden ->
                historyByPkg = history.associateBy { it.packageName }
                rankByUsage(apps.filterNot { it.packageName in hidden }, historyByPkg).take(4)
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

        viewModelScope.launch {
            combine(
                contactHistory.recents,
                settings.contactSearchEnabled,
            ) { recents, enabled ->
                contactHistoryById = recents.associateBy { it.id }
                if (enabled) recents else emptyList()
            }.collect { displayed ->
                _uiState.value = _uiState.value.copy(contactRecents = displayed)
            }
        }

        // Re-run the local search when the contact-search toggle flips so
        // results / recents reflect the new state without waiting for the
        // next keystroke.
        viewModelScope.launch {
            settings.contactSearchEnabled.collect {
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

    fun removeSearchHistory(query: String) {
        viewModelScope.launch { searchHistory.remove(WebSearchHistoryEntry(query)) }
    }

    fun hideAppFromRecents(packageName: String) {
        viewModelScope.launch { hiddenApps.hide(packageName) }
    }

    fun removeRecentContact(entry: ContactHistoryEntry) {
        viewModelScope.launch { contactHistory.remove(entry) }
    }

    private fun runLocalSearch(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                query = query,
                appResults = emptyList(),
                contactResults = emptyList(),
                webResults = emptyList(),
                tileResults = emptyList(),
            )
            return
        }

        val now = System.currentTimeMillis()
        val apps = AppIndex.search(query, limit = 6) { pkg ->
            historyByPkg[pkg]?.score(now) ?: 0f
        }
        val contacts = if (settings.contactSearchEnabled.value) {
            ContactRepository.search(getApplication(), query, limit = 3) { id ->
                contactHistoryById[id]?.score(now) ?: 0f
            }
        } else emptyList()
        val tiles = SettingsTileRepository.search(getApplication(), query, limit = 4)

        _uiState.value = _uiState.value.copy(
            query = query,
            appResults = apps,
            contactResults = contacts,
            tileResults = tiles,
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

    private fun rankByUsage(
        apps: List<AppEntry>,
        history: Map<String, AppHistoryEntry>,
    ): List<AppEntry> {
        val now = System.currentTimeMillis()
        return apps.sortedWith(
            compareByDescending<AppEntry> {
                history[it.packageName]?.score(now) ?: 0f
            }.thenBy { it.label.lowercase() }
        )
    }
}
