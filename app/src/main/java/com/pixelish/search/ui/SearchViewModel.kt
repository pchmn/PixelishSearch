package com.pixelish.search.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelish.search.data.AppEntry
import com.pixelish.search.data.AppIndex
import com.pixelish.search.data.AppUsageRepository
import com.pixelish.search.data.ContactAction
import com.pixelish.search.data.ContactEntry
import com.pixelish.search.data.ContactHistoryEntry
import com.pixelish.search.data.ContactHistoryRepository
import com.pixelish.search.data.ContactRepository
import com.pixelish.search.data.SearchHistoryRepository
import com.pixelish.search.data.UsageStat
import com.pixelish.search.data.WebSuggestRepository
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
        // S'assure que l'index est chargé (au cas où l'app est lancée avant la fin du préchargement)
        viewModelScope.launch {
            AppIndex.preload(application)
        }

        // Suggestions par défaut : top 4 apps par score d'usage (decay temporel),
        // tiebreaker = ordre alpha. Recalculé dès que l'index ou les stats changent.
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

        // Recherche locale instantanée à chaque frappe (apps + contacts sont rapides)
        viewModelScope.launch {
            _query
                .onEach { runLocalSearch(it) }
                .debounce(180) // debounce uniquement pour le réseau
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
            // Vérifie que la query n'a pas changé entre temps
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
