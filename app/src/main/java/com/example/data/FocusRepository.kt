package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class FocusRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val dao = database.focusLogDao()
    val preferences = FocusPreferences(context)

    val allLogs: Flow<List<FocusLog>> = dao.getAllLogs()

    val todayLogs: Flow<List<FocusLog>> = dao.getLogsSince(getStartOfTodayTimestamp())

    val totalBlockedCount: Flow<Int> = dao.getTotalBlockedCount().map { it ?: 0 }

    val todayBlockedCount: Flow<Int> = todayLogs.map { list ->
        list.sumOf { it.blockedFutureCount }
    }

    val todaySecondsWatched: Flow<Int> = todayLogs.map { list ->
        list.sumOf { it.durationSeconds }
    }

    suspend fun recordShortSession(
        durationSeconds: Int,
        blockedFutureCount: Int,
        triggerReason: String,
        source: String
    ) {
        val log = FocusLog(
            timestamp = System.currentTimeMillis(),
            shortsWatchedCount = 1,
            durationSeconds = durationSeconds,
            blockedFutureCount = blockedFutureCount,
            triggerReason = triggerReason,
            source = source
        )
        dao.insertLog(log)
        preferences.setLastWatchedTimestamp(System.currentTimeMillis())
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }

    private fun getStartOfTodayTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
