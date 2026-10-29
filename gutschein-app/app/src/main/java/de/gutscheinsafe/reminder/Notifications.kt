package de.gutscheinsafe.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.gutscheinsafe.MainActivity
import de.gutscheinsafe.R

object Notifications {
    const val CHANNEL_EXPIRY = "expiry"
    const val CHANNEL_IDEAS = "ideas"
    const val EXTRA_VOUCHER_ID = "voucher_id"
    const val EXTRA_OPEN_IDEAS = "open_ideas"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_EXPIRY, "Ablauf-Erinnerungen", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Erinnert dich, bevor ein Gutschein abläuft"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_IDEAS, "Ideen & Vorschläge", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Saisonale Vorschläge, was du mit deinen Gutscheinen machen könntest"
            },
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(
        context: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        voucherId: Long? = null,
        openIdeas: Boolean = false,
    ) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (voucherId != null) putExtra(EXTRA_VOUCHER_ID, voucherId)
            if (openIdeas) putExtra(EXTRA_OPEN_IDEAS, true)
        }
        val pending = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF0F766E.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(
                if (channel == CHANNEL_EXPIRY) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT,
            )
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission was revoked in the meantime.
        }
    }
}
