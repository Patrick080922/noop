package de.gutscheinsafe.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.gutscheinsafe.data.AppDatabase
import de.gutscheinsafe.data.Format
import de.gutscheinsafe.data.Settings
import de.gutscheinsafe.data.Voucher
import de.gutscheinsafe.ideas.Suggestions
import java.time.LocalDate

/** Runs once a day: sends expiry reminders and (once a month) a seasonal idea. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        val vouchers = AppDatabase.get(applicationContext).voucherDao().getAll()
        val today = LocalDate.now()

        if (settings.remindersEnabled) {
            for (v in vouchers) checkExpiry(v, settings, today)
        }
        if (settings.seasonalIdeasEnabled) {
            val monthKey = today.year * 100 + today.monthValue
            if (settings.lastIdeaMonth != monthKey) {
                Suggestions.monthlyHeadline(vouchers, today)?.let { (title, text) ->
                    Notifications.show(
                        applicationContext, IDEA_NOTIFICATION_ID, Notifications.CHANNEL_IDEAS,
                        title, text, openIdeas = true,
                    )
                }
                settings.lastIdeaMonth = monthKey
            }
        }
        return Result.success()
    }

    private fun checkExpiry(v: Voucher, settings: Settings, today: LocalDate) {
        if (v.isRedeemed) return
        val expiry = v.expiryDate ?: return
        val days = v.daysLeft(today) ?: return
        if (days < 0) return
        // The smallest reminder step that has been reached. If the phone was off on the exact
        // day, the reminder is still sent later – but every step only once.
        val step = settings.reminderDays.filter { it >= days }.minOrNull() ?: return
        val key = "${v.id}_${expiry}_$step"
        if (settings.wasNotified(key)) return

        val valueText = v.currentValue?.let { " (${Format.money(it)})" } ?: ""
        val title = when (days) {
            0L -> "⏰ Letzter Tag: ${v.title}"
            1L -> "⏰ Morgen läuft ein Gutschein ab"
            else -> "🎁 Gutschein läuft in $days Tagen ab"
        }
        val text = "${v.title} – ${v.provider}$valueText ist gültig bis ${Format.date(expiry)}. " +
            when {
                days <= 1 -> "Jetzt noch schnell einlösen!"
                days <= 7 -> "Plane jetzt, wann du ihn einlöst."
                else -> "Zeit, einen Termin zu planen."
            }
        Notifications.show(
            applicationContext, (v.id % Int.MAX_VALUE).toInt(), Notifications.CHANNEL_EXPIRY,
            title, text, voucherId = v.id,
        )
        settings.markNotified(key)
    }

    companion object {
        const val IDEA_NOTIFICATION_ID = Int.MAX_VALUE - 1
    }
}
