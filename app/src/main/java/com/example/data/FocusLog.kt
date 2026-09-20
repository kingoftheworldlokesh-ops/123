package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_logs")
data class FocusLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val shortsWatchedCount: Int = 1,
    val durationSeconds: Int = 0,
    val blockedFutureCount: Int = 1,
    val triggerReason: String = "time_limit_expired", // "time_limit_expired", "swiped_to_next", "cooldown_active"
    val source: String = "youtube_app" // "youtube_app" or "sandbox_test"
)
