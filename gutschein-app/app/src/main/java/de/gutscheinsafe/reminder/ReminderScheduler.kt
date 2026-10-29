package de.gutscheinsafe.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import de.gutscheinsafe.data.Settings
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val DAILY = "daily_reminders"
    private const val NOW = "reminders_now"

    /** Schedules the daily check at the hour chosen in the settings. */
    fun schedule(context: Context, replace: Boolean = false) {
        val hour = Settings(context).reminderHour
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Runs the check right away (e.g. after adding a voucher that expires soon). */
    fun runNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<ReminderWorker>().build(),
        )
    }
}
