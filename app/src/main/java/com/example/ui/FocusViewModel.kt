package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FocusLog
import com.example.data.FocusRepository
import com.example.service.ShortsBlockerAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusRepository(application)
    val preferences = repository.preferences

    val isShieldActive: StateFlow<Boolean> = preferences.isShieldActive
    val timeLimitSeconds: StateFlow<Int> = preferences.timeLimitSeconds
    val cooldownMinutes: StateFlow<Int> = preferences.cooldownMinutes
    val strictOneShort: StateFlow<Boolean> = preferences.strictOneShort
    val hapticEnabled: StateFlow<Boolean> = preferences.hapticEnabled

    val todayLogs: StateFlow<List<FocusLog>> = repository.todayLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allLogs: StateFlow<List<FocusLog>> = repository.allLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val totalBlockedCount: StateFlow<Int> = repository.totalBlockedCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val todayBlockedCount: StateFlow<Int> = repository.todayBlockedCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val todaySecondsWatched: StateFlow<Int> = repository.todaySecondsWatched.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _cooldownRemainingSeconds = MutableStateFlow(0L)
    val cooldownRemainingSeconds: StateFlow<Long> = _cooldownRemainingSeconds.asStateFlow()

    init {
        refreshServiceStatus()

        // Periodic ticker for cooldown countdown
        viewModelScope.launch {
            while (isActive) {
                _cooldownRemainingSeconds.value = preferences.remainingCooldownSeconds()
                delay(1000)
            }
        }
    }

    fun refreshServiceStatus() {
        val app = getApplication<Application>()
        _isAccessibilityEnabled.value =
            ShortsBlockerAccessibilityService.isServiceRunning.value ||
                    ShortsBlockerAccessibilityService.isAccessibilitySettingsEnabled(app)
    }

    fun toggleShieldActive() {
        preferences.setShieldActive(!isShieldActive.value)
    }

    fun setTimeLimit(seconds: Int) {
        preferences.setTimeLimitSeconds(seconds)
    }

    fun setCooldown(minutes: Int) {
        preferences.setCooldownMinutes(minutes)
    }

    fun setStrictOneShort(strict: Boolean) {
        preferences.setStrictOneShort(strict)
    }

    fun setHaptic(enabled: Boolean) {
        preferences.setHapticEnabled(enabled)
    }

    fun recordSandboxSession(durationSeconds: Int, blockedReason: String) {
        viewModelScope.launch {
            repository.recordShortSession(
                durationSeconds = durationSeconds,
                blockedFutureCount = 1,
                triggerReason = blockedReason,
                source = "sandbox_test"
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun triggerTestBlock(context: Context) {
        val intent = Intent(context, BlockedShortsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(BlockedShortsActivity.EXTRA_REASON, "swiped_to_next")
            putExtra(BlockedShortsActivity.EXTRA_SECONDS_WATCHED, preferences.timeLimitSeconds.value)
            putExtra(BlockedShortsActivity.EXTRA_LIMIT_SECONDS, preferences.timeLimitSeconds.value)
        }
        context.startActivity(intent)
    }
}
