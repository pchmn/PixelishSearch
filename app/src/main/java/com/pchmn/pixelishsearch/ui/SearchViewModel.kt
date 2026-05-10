package com.pchmn.pixelishsearch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pchmn.pixelishsearch.data.AppEntry
import com.pchmn.pixelishsearch.data.AppIndex
import com.pchmn.pixelishsearch.data.AppUsageRepository
import com.pchmn.pixelishsearch.data.ContactAction
import com.pchmn.pixelishsearch.data.ContactEntry
import com.pchmn.pixelishsearch.data.ContactHistoryEntry
import com.pchmn.pixelishsearch.data.ContactHistoryRepository
import com.pchmn.pixelishsearch.data.ContactRepository
import com.pchmn.pixelishsearch.data.SearchHistoryRepository
import com.pchmn.pixelishsearch.data.UsageStat
import com.pchmn.pixelishsearch.data.WebSuggestRepository
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
    val apps: List<AppEntry> = emptyList(),
    val contacts: List<ContactEntry> = emptyList(),
    val webSuggestions: List<String> = emptyList(),
    val suggestedApps: List<AppEntry> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val recentContacts: List<ContactHistoryEntry> = emptyList(),
)

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val _query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var webJob: Job? = null

    init {
        // Ensures the index is loaded (in case the app is launched before preload finishes)
        viewModelScope.launch {
            AppIndex.preload(application)
        }

        // Default suggestions: top 4 apps by usage score (time decay),
        // tiebreaker = alphabetical order. Recomputed whenever the index or stats change.
        viewModelScope.launch {
            combine(AppIndex.apps, AppUsageRepository.stats) { apps, stats ->
                rankByUsage(apps, stats).take(4)
            }.collect { suggested ->
                _uiState.value = _uiState.value.copy(suggestedApps = suggested)
            }
        }

        viewModelScope.launch {
            SearchHistoryRepository.history.collect { entries ->
                _uiState.value = _uiState.value.copy(
                    searchHistory = entries.map { it.query }
                )
            }
        }

        viewModelScope.launch {
            ContactHistoryRepository.recents.collect { recents ->
                _uiState.value = _uiState.value.copy(recentContacts = recents)
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

    fun onAppLaunched(packageName: String) {
        AppUsageRepository.recordLaunch(packageName)
    }

    fun onSearchLaunched(query: String) {
        SearchHistoryRepository.record(query)
    }

    fun onContactUsed(contact: ContactEntry, action: ContactAction) {
        ContactHistoryRepository.record(
            id = contact.id,
            name = contact.name,
            photoUri = contact.photoUri,
            phoneNumber = contact.phoneNumber,
            action = action,
        )
    }

    private fun runLocalSearch(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                query = query,
                apps = emptyList(),
                contacts = emptyList(),
                webSuggestions = emptyList()
            )
            return
        }

        val stats = AppUsageRepository.stats.value
        val now = System.currentTimeMillis()
        val apps = AppIndex.search(query, limit = 6) { pkg ->
            AppUsageRepository.scoreOf(pkg, stats, now)
        }
        val contacts = ContactRepository.search(getApplication(), query, limit = 3)

        _uiState.value = _uiState.value.copy(
            query = query,
            apps = apps,
            contacts = contacts
        )
    }

    private fun runWebSearch(query: String) {
        webJob?.cancel()
        if (query.isBlank()) return
        webJob = viewModelScope.launch {
            val suggestions = WebSuggestRepository.fetch(query, limit = 5)
            // Make sure the query hasn't changed in the meantime
            if (_query.value == query) {
                _uiState.value = _uiState.value.copy(webSuggestions = suggestions)
            }
        }
    }

    private fun rankByUsage(
        apps: List<AppEntry>,
        stats: Map<String, UsageStat>,
    ): List<AppEntry> {
        val now = System.currentTimeMillis()
        return apps.sortedWith(
            compareByDescending<AppEntry> {
                AppUsageRepository.scoreOf(it.packageName, stats, now)
            }.thenBy { it.label.lowercase() }
        )
    }
}
