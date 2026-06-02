package com.pchmn.pixelishsearch.preferences.ui

import android.app.Application
import android.app.LocaleManager
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pchmn.pixelishsearch.PixelishSearchApp
import com.pchmn.pixelishsearch.search.calendar.data.CalendarRepository
import com.pchmn.pixelishsearch.search.contacts.data.ContactRepository
import com.pchmn.pixelishsearch.search.settings.data.SettingsTileId
import com.pchmn.pixelishsearch.update.UpdateActivity
import com.pchmn.pixelishsearch.update.data.CheckOutcome
import com.pchmn.pixelishsearch.update.data.UpdateChecker
import com.pchmn.pixelishsearch.update.data.UpdateInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Transient state of the manual "Check for updates" action. The persisted
 * "an update is available" flag lives in `UpdateRepository`; this only models
 * the in-flight feedback (checking / up-to-date / failed) shown on the row.
 */
sealed interface CheckUiState {
    data object Idle : CheckUiState
    data object Checking : CheckUiState
    data object UpToDate : CheckUiState
    data object Failed : CheckUiState
}

data class PreferencesUiState(
    val contactSearchEnabled: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val calendarSearchEnabled: Boolean = false,
    val hasCalendarPermission: Boolean = false,
    val shortcutSearchEnabled: Boolean = true,
    val disabledTileIds: Set<String> = emptySet(),
    val updateAvailable: UpdateInfo? = null,
    val currentVersion: String = "",
    val updateCheck: CheckUiState = CheckUiState.Idle,
    val currentLanguageTag: String? = null,
) {
    /**
     * The contact toggle reads as on only when the user enabled it *and* the
     * runtime READ_CONTACTS permission is still granted. Mirrors the guard
     * `ContactRepository.search` applies on the actual search path.
     */
    val effectiveContactSearch: Boolean get() = contactSearchEnabled && hasContactsPermission

    /** Same gating as [effectiveContactSearch], for the READ_CALENDAR grant. */
    val effectiveCalendarSearch: Boolean get() = calendarSearchEnabled && hasCalendarPermission
}

/**
 * Funnels every Preferences screen's state and gestures behind one interface,
 * the same shape as `SearchViewModel`: a single `uiState` plus `onX()` verbs.
 *
 * The contacts permission dialog must be launched from the Activity (it needs a
 * composition-scoped `ActivityResultLauncher`), so the screen owns that one
 * launch and reports the outcome back via [onContactsPermissionResult]. The
 * resume re-check is driven by `PreferencesActivity.onResume`, mirroring how
 * `MainActivity` calls `vm.refreshTileStates()`.
 */
class PreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PixelishSearchApp
    private val preferences get() = app.preferences
    private val updates get() = app.updates

    private val _uiState = MutableStateFlow(
        PreferencesUiState(
            hasContactsPermission = ContactRepository.hasPermission(app),
            hasCalendarPermission = CalendarRepository.hasPermission(app),
            currentVersion = app.currentVersionName(),
            currentLanguageTag = currentLanguageTag(),
        )
    )
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    private var updateClearJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.contactSearchEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(contactSearchEnabled = enabled)
            }
        }
        viewModelScope.launch {
            preferences.calendarSearchEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(calendarSearchEnabled = enabled)
            }
        }
        viewModelScope.launch {
            preferences.shortcutSearchEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(shortcutSearchEnabled = enabled)
            }
        }
        viewModelScope.launch {
            preferences.disabledTileIds.collect { ids ->
                _uiState.value = _uiState.value.copy(disabledTileIds = ids)
            }
        }
        viewModelScope.launch {
            updates.available.collect { info ->
                _uiState.value = _uiState.value.copy(updateAvailable = info)
            }
        }
    }

    // region Contact search + permission
    fun setContactSearch(enabled: Boolean) {
        viewModelScope.launch { preferences.setContactSearchEnabled(enabled) }
    }

    /**
     * Outcome of the system permission dialog the screen launched. Persisting
     * the pref here (rather than in the screen) keeps "granted ⇒ enabled" in
     * one place.
     */
    fun onContactsPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasContactsPermission = granted)
        if (granted) setContactSearch(true)
    }

    /**
     * Re-read the permission after the user may have changed it in system
     * settings while we were paused. Called from `PreferencesActivity.onResume`.
     */
    fun refreshContactsPermission() {
        val granted = ContactRepository.hasPermission(app)
        if (granted != _uiState.value.hasContactsPermission) {
            _uiState.value = _uiState.value.copy(hasContactsPermission = granted)
        }
    }
    // endregion

    // region Calendar search + permission
    fun setCalendarSearch(enabled: Boolean) {
        viewModelScope.launch { preferences.setCalendarSearchEnabled(enabled) }
    }

    fun onCalendarPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasCalendarPermission = granted)
        if (granted) setCalendarSearch(true)
    }

    fun refreshCalendarPermission() {
        val granted = CalendarRepository.hasPermission(app)
        if (granted != _uiState.value.hasCalendarPermission) {
            _uiState.value = _uiState.value.copy(hasCalendarPermission = granted)
        }
    }
    // endregion

    // region Shortcut search (no permission — default on)
    fun setShortcutSearch(enabled: Boolean) {
        viewModelScope.launch { preferences.setShortcutSearchEnabled(enabled) }
    }
    // endregion

    fun onTileToggled(id: SettingsTileId, enabled: Boolean) {
        viewModelScope.launch { preferences.setTileEnabled(id, enabled) }
    }

    // region Update check
    /**
     * Manual "Check for updates". Bypasses the throttle (via `checkNow`), opens
     * `UpdateActivity` if a release is available, and otherwise surfaces a
     * transient up-to-date / failed state that auto-clears after 3s.
     */
    fun onCheckForUpdates() {
        if (_uiState.value.updateCheck is CheckUiState.Checking) return
        updateClearJob?.cancel()
        _uiState.value = _uiState.value.copy(updateCheck = CheckUiState.Checking)
        viewModelScope.launch {
            val next = when (UpdateChecker.checkNow(updates, _uiState.value.currentVersion)) {
                is CheckOutcome.Available -> {
                    openUpdate()
                    CheckUiState.Idle
                }

                CheckOutcome.UpToDate -> CheckUiState.UpToDate
                CheckOutcome.Failed -> CheckUiState.Failed
            }
            _uiState.value = _uiState.value.copy(updateCheck = next)
            if (next is CheckUiState.UpToDate || next is CheckUiState.Failed) {
                updateClearJob = viewModelScope.launch {
                    delay(3_000)
                    _uiState.value = _uiState.value.copy(updateCheck = CheckUiState.Idle)
                }
            }
        }
    }

    private fun openUpdate() {
        app.startActivity(
            Intent(app, UpdateActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
    // endregion

    // region Language (API 33+)
    fun onLanguageSelected(tag: String?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val localeManager = app.getSystemService(LocaleManager::class.java)
        localeManager.applicationLocales = if (tag == null) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList(Locale.forLanguageTag(tag))
        }
        _uiState.value = _uiState.value.copy(currentLanguageTag = tag)
    }

    private fun currentLanguageTag(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val localeManager = app.getSystemService(LocaleManager::class.java)
        return localeManager.applicationLocales.takeIf { !it.isEmpty }?.get(0)?.language
    }
    // endregion
}
