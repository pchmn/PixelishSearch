package com.pixelish.search.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelish.search.data.AppEntry
import com.pixelish.search.data.AppIndex
import com.pixelish.search.data.ContactEntry
import com.pixelish.search.data.ContactRepository
import com.pixelish.search.data.WebSuggestRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val apps: List<AppEntry> = emptyList(),
    val contacts: List<ContactEntry> = emptyList(),
    val webSuggestions: List<String> = emptyList(),
    val suggestedApps: List<AppEntry> = emptyList()
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
            // Suggestions par défaut : 4 premières apps (à terme : apps les plus utilisées)
            _uiState.value = _uiState.value.copy(
                suggestedApps = AppIndex.apps.value.take(4)
            )
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

        val apps = AppIndex.search(query, limit = 6)
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
}
