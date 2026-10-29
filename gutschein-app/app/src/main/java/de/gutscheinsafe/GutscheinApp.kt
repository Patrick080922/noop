package de.gutscheinsafe

import android.app.Application
import de.gutscheinsafe.reminder.Notifications
import de.gutscheinsafe.reminder.ReminderScheduler

class GutscheinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        ReminderScheduler.schedule(this)
    }
}
