package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("shorts_shield_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TIME_LIMIT_SECONDS = "key_time_limit_seconds"
        private const val KEY_COOLDOWN_MINUTES = "key_cooldown_minutes"
        private const val KEY_SHIELD_ACTIVE = "key_shield_active"
        private const val KEY_STRICT_ONE_SHORT = "key_strict_one_short"
        private const val KEY_HAPTIC_ENABLED = "key_haptic_enabled"
        private const val KEY_LAST_WATCHED_TIMESTAMP = "key_last_watched_timestamp"

        const val DEFAULT_TIME_LIMIT_SECONDS = 60
        const val DEFAULT_COOLDOWN_MINUTES = 30
    }

    private val _timeLimitSeconds = MutableStateFlow(prefs.getInt(KEY_TIME_LIMIT_SECONDS, DEFAULT_TIME_LIMIT_SECONDS))
    val timeLimitSeconds: StateFlow<Int> = _timeLimitSeconds.asStateFlow()

    private val _cooldownMinutes = MutableStateFlow(prefs.getInt(KEY_COOLDOWN_MINUTES, DEFAULT_COOLDOWN_MINUTES))
    val cooldownMinutes: StateFlow<Int> = _cooldownMinutes.asStateFlow()

    private val _isShieldActive = MutableStateFlow(prefs.getBoolean(KEY_SHIELD_ACTIVE, true))
    val isShieldActive: StateFlow<Boolean> = _isShieldActive.asStateFlow()

    private val _strictOneShort = MutableStateFlow(prefs.getBoolean(KEY_STRICT_ONE_SHORT, true))
    val strictOneShort: StateFlow<Boolean> = _strictOneShort.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC_ENABLED, true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _lastWatchedTimestamp = MutableStateFlow(prefs.getLong(KEY_LAST_WATCHED_TIMESTAMP, 0L))
    val lastWatchedTimestamp: StateFlow<Long> = _lastWatchedTimestamp.asStateFlow()

    fun setTimeLimitSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_TIME_LIMIT_SECONDS, seconds).apply()
        _timeLimitSeconds.value = seconds
    }

    fun setCooldownMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_COOLDOWN_MINUTES, minutes).apply()
        _cooldownMinutes.value = minutes
    }

    fun setShieldActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_SHIELD_ACTIVE, active).apply()
        _isShieldActive.value = active
    }

    fun setStrictOneShort(strict: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_ONE_SHORT, strict).apply()
        _strictOneShort.value = strict
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
        _hapticEnabled.value = enabled
    }

    fun setLastWatchedTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_WATCHED_TIMESTAMP, timestamp).apply()
        _lastWatchedTimestamp.value = timestamp
    }

    /**
     * Checks if user is currently in a cooldown lockout period after watching a Short
     */
    fun isCurrentlyInCooldown(): Boolean {
        val cooldownMs = _cooldownMinutes.value * 60 * 1000L
        if (cooldownMs <= 0) return false
        val lastWatched = _lastWatchedTimestamp.value
        if (lastWatched == 0L) return false
        val elapsed = System.currentTimeMillis() - lastWatched
        return elapsed < cooldownMs
    }

    fun remainingCooldownSeconds(): Long {
        val cooldownMs = _cooldownMinutes.value * 60 * 1000L
        val lastWatched = _lastWatchedTimestamp.value
        if (lastWatched == 0L) return 0L
        val elapsed = System.currentTimeMillis() - lastWatched
        val remaining = (cooldownMs - elapsed) / 1000
        return if (remaining > 0) remaining else 0L
    }
}
