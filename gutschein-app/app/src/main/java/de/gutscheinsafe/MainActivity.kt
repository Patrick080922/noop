package de.gutscheinsafe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import de.gutscheinsafe.reminder.Notifications
import de.gutscheinsafe.ui.AppRoot
import de.gutscheinsafe.ui.DeepLink
import de.gutscheinsafe.ui.theme.GutscheinTheme

class MainActivity : ComponentActivity() {

    private val deepLink = mutableStateOf<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) deepLink.value = parse(intent)
        setContent {
            GutscheinTheme {
                AppRoot(
                    deepLink = deepLink.value,
                    onDeepLinkHandled = { deepLink.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLink.value = parse(intent)
    }

    private fun parse(intent: Intent?): DeepLink? {
        if (intent == null) return null
        val id = intent.getLongExtra(Notifications.EXTRA_VOUCHER_ID, -1L)
        return when {
            id > 0 -> DeepLink.Voucher(id)
            intent.getBooleanExtra(Notifications.EXTRA_OPEN_IDEAS, false) -> DeepLink.Ideas
            else -> null
        }
    }
}
