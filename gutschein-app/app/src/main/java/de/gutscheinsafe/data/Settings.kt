package de.gutscheinsafe.data

import android.content.Context

/** Small key/value settings, stored in SharedPreferences. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var remindersEnabled: Boolean
        get() = prefs.getBoolean("reminders_enabled", true)
        set(value) = prefs.edit().putBoolean("reminders_enabled", value).apply()

    /** Days before expiry on which a reminder is sent (0 = on the last day). */
    var reminderDays: Set<Int>
        get() = prefs.getStringSet("reminder_days", null)?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?: DEFAULT_REMINDER_DAYS
        set(value) = prefs.edit().putStringSet("reminder_days", value.map { it.toString() }.toSet()).apply()

    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 9)
        set(value) = prefs.edit().putInt("reminder_hour", value).apply()

    var seasonalIdeasEnabled: Boolean
        get() = prefs.getBoolean("seasonal_ideas", true)
        set(value) = prefs.edit().putBoolean("seasonal_ideas", value).apply()

    var notificationPermissionAsked: Boolean
        get() = prefs.getBoolean("notif_asked", false)
        set(value) = prefs.edit().putBoolean("notif_asked", value).apply()

    /** Remembers which reminders were already shown so nothing is sent twice. */
    fun wasNotified(key: String): Boolean = prefs.getBoolean("sent_$key", false)
    fun markNotified(key: String) = prefs.edit().putBoolean("sent_$key", true).apply()

    var lastIdeaMonth: Int
        get() = prefs.getInt("last_idea_month", 0)
        set(value) = prefs.edit().putInt("last_idea_month", value).apply()

    companion object {
        val DEFAULT_REMINDER_DAYS = setOf(30, 7, 1, 0)
        val REMINDER_DAY_OPTIONS = listOf(90, 60, 30, 14, 7, 3, 1, 0)
    }
}
