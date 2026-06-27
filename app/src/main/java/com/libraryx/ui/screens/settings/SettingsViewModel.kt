package com.libraryx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.LateFeeSettings
import com.libraryx.data.model.NotificationSettings
import com.libraryx.data.model.NotifyFor
import com.libraryx.ui.navigation.SaasSessionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Mirrors src/pages/Settings.tsx: loads current settings into editable local state, saves on demand. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionHolder: SaasSessionHolder
) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings.Default)
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            sessionHolder.repository.flatMapLatest { repo ->
                if (repo == null) flowOf(AppSettings.Default) else repo.settings
            }.collectLatest { _settings.value = it }
        }
    }

    /** Mirrors `handleSave`. */
    fun save(
        labName: String,
        totalSeats: Int,
        overdueDays: Int,
        dueDate: Int,
        lateFeeEnabled: Boolean,
        lateFeeAmount: Double,
        lateFeeAfterDays: Int,
        lateFeeCompound: Boolean,
        notifEnabled: Boolean,
        notifTime: String,
        notifFor: NotifyFor,
        notifSound: Boolean,
        portalEnabled: Boolean,
        onSaved: () -> Unit
    ) {
        val repo = sessionHolder.repository.value ?: return
        val updated = AppSettings(
            labName = labName.trim().ifBlank { "Study Lab" },
            totalSeats = totalSeats,
            overdueDays = overdueDays,
            dueDate = dueDate,
            lateFee = LateFeeSettings(lateFeeEnabled, lateFeeAmount, lateFeeAfterDays, lateFeeCompound),
            notifications = NotificationSettings(notifEnabled, notifTime, notifFor, notifSound),
            studentPortalEnabled = portalEnabled
        )
        viewModelScope.launch {
            repo.updateSettings(updated)
            onSaved()
        }
    }
}
