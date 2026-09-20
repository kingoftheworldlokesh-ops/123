package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.FocusPreferences
import com.example.data.FocusRepository
import com.example.ui.BlockedShortsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ShortsBlockerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ShortsBlockerService"
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        /**
         * Checks if the Accessibility Service is enabled in Android System Settings
         */
        fun isAccessibilitySettingsEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${ShortsBlockerAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServices.split(':').any {
                it.equals(expectedServiceName, ignoreCase = true) ||
                        it.contains(ShortsBlockerAccessibilityService::class.java.simpleName, ignoreCase = true)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var preferences: FocusPreferences
    private lateinit var repository: FocusRepository

    // Session tracking state
    private var isWatchingShorts = false
    private var firstShortSignature: String? = null
    private var shortSessionStartTime: Long = 0L
    private var timerJob: Job? = null
    private var hasBlockedCurrentSession = false

    override fun onCreate() {
        super.onCreate()
        preferences = FocusPreferences(this)
        repository = FocusRepository(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceRunning.value = true
        Log.i(TAG, "ShortsBlockerAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName != YOUTUBE_PACKAGE) {
            // User left YouTube
            if (isWatchingShorts) {
                resetSession()
            }
            return
        }

        // YouTube is in foreground
        if (!preferences.isShieldActive.value) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        try {
            val inShorts = detectYouTubeShorts(rootNode)
            if (inShorts) {
                handleShortsDetected(rootNode)
            } else {
                if (isWatchingShorts) {
                    resetSession()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }

    /**
     * Inspects accessibility tree to detect if the YouTube Shorts player is currently active
     */
    private fun detectYouTubeShorts(rootNode: AccessibilityNodeInfo): Boolean {
        // Fast check: look for Shorts specific view IDs or buttons
        val shortsMatches = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_recycler")
        if (shortsMatches.isNotEmpty()) {
            return true
        }

        val pageTreeMatches = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_player_page_tree")
        if (pageTreeMatches.isNotEmpty()) {
            return true
        }

        // Secondary heuristic: check for Shorts interactive elements like remix/sound or reel player
        val textDislike = rootNode.findAccessibilityNodeInfosByText("Dislike this short")
        if (textDislike.isNotEmpty()) {
            return true
        }

        val textRemix = rootNode.findAccessibilityNodeInfosByText("Remix this short")
        if (textRemix.isNotEmpty()) {
            return true
        }

        val textSound = rootNode.findAccessibilityNodeInfosByText("Sound used in this short")
        if (textSound.isNotEmpty()) {
            return true
        }

        return false
    }

    /**
     * Handles when user is in YouTube Shorts.
     * Enforces: Watch first short only, then block future shorts after time limit.
     */
    private fun handleShortsDetected(rootNode: AccessibilityNodeInfo) {
        if (hasBlockedCurrentSession) {
            // Already blocked, ensure they exit
            performBlock("session_already_blocked")
            return
        }

        // Check if cooldown is currently active
        if (preferences.isCurrentlyInCooldown()) {
            performBlock("cooldown_active")
            return
        }

        val currentSignature = extractVideoSignature(rootNode)

        if (!isWatchingShorts) {
            // User just entered Shorts! This is Video 1 (The allowed Short)
            isWatchingShorts = true
            firstShortSignature = currentSignature
            shortSessionStartTime = System.currentTimeMillis()
            Log.d(TAG, "First short session started! Sig: $currentSignature")

            startCountdownTimer()
            return
        }

        // User is already watching shorts.
        // Check if user swiped to a 2nd video (Future Shorts blocked!)
        if (preferences.strictOneShort.value &&
            currentSignature.isNotBlank() &&
            firstShortSignature != null &&
            firstShortSignature != currentSignature
        ) {
            Log.d(TAG, "Swiped to 2nd short detected! Blocking future short. Sig was $firstShortSignature, now $currentSignature")
            performBlock("swiped_to_next")
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        val limitSec = preferences.timeLimitSeconds.value

        timerJob = serviceScope.launch {
            val startTime = System.currentTimeMillis()
            val totalLimitMs = limitSec * 1000L

            while (isActive && isWatchingShorts) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= totalLimitMs) {
                    Log.d(TAG, "Time limit ($limitSec s) expired for the first short! Blocking.")
                    performBlock("time_limit_expired")
                    break
                }
                delay(500)
            }
        }
    }

    private fun performBlock(reason: String) {
        if (hasBlockedCurrentSession) return
        hasBlockedCurrentSession = true
        timerJob?.cancel()

        val elapsedSeconds = if (shortSessionStartTime > 0) {
            ((System.currentTimeMillis() - shortSessionStartTime) / 1000).toInt()
        } else {
            preferences.timeLimitSeconds.value
        }

        triggerHapticNudge()

        // 1. Send Back / Home action to exit the Shorts player immediately
        performGlobalAction(GLOBAL_ACTION_BACK)

        // 2. Record to database
        serviceScope.launch {
            repository.recordShortSession(
                durationSeconds = elapsedSeconds,
                blockedFutureCount = 1,
                triggerReason = reason,
                source = "youtube_app"
            )
        }

        // 3. Launch the mindful blocked intervention activity
        val intent = Intent(this, BlockedShortsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BlockedShortsActivity.EXTRA_REASON, reason)
            putExtra(BlockedShortsActivity.EXTRA_SECONDS_WATCHED, elapsedSeconds)
            putExtra(BlockedShortsActivity.EXTRA_LIMIT_SECONDS, preferences.timeLimitSeconds.value)
        }
        startActivity(intent)

        serviceScope.launch {
            delay(1500)
            resetSession()
        }
    }

    private fun resetSession() {
        isWatchingShorts = false
        firstShortSignature = null
        shortSessionStartTime = 0L
        hasBlockedCurrentSession = false
        timerJob?.cancel()
        timerJob = null
    }

    private fun extractVideoSignature(rootNode: AccessibilityNodeInfo): String {
        // Find text identifiers like title or channel name safely
        val sb = StringBuilder()
        fun traverse(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 4) return
            try {
                val text = node.text?.toString() ?: node.contentDescription?.toString()
                if (!text.isNullOrBlank() && text.length > 2 && !text.equals("Shorts", ignoreCase = true)) {
                    sb.append(text).append("|")
                }
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    traverse(child, depth + 1)
                }
            } catch (e: Exception) {
                // Ignore node accessibility query exceptions
            }
        }
        traverse(rootNode, 0)
        return sb.toString().take(120)
    }

    private fun triggerHapticNudge() {
        if (!preferences.hapticEnabled.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 120), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate", e)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "ShortsBlockerAccessibilityService interrupted")
        resetSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        resetSession()
    }
}
