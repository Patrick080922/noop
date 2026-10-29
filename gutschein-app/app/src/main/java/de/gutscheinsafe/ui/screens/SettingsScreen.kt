package de.gutscheinsafe.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import de.gutscheinsafe.BuildConfig
import de.gutscheinsafe.data.Settings
import de.gutscheinsafe.reminder.Notifications
import de.gutscheinsafe.reminder.ReminderScheduler
import de.gutscheinsafe.ui.AppViewModel
import de.gutscheinsafe.ui.SectionTitle
import de.gutscheinsafe.ui.startSafely
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var enabled by remember { mutableStateOf(settings.remindersEnabled) }
    var days by remember { mutableStateOf(settings.reminderDays) }
    var hour by remember { mutableIntStateOf(settings.reminderHour) }
    var ideas by remember { mutableStateOf(settings.seasonalIdeasEnabled) }
    var canNotify by remember { mutableStateOf(Notifications.canPost(context)) }

    // Re-check the permission when coming back from the system settings.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { canNotify = Notifications.canPost(context) }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
        if (!granted) openNotificationSettings(context)
    }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            vm.exportBackup(uri)
                .onSuccess { snackbar.showSnackbar("✓ $it Gutscheine gesichert") }
                .onFailure { snackbar.showSnackbar("Sicherung fehlgeschlagen: ${it.message}") }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            vm.importBackup(uri)
                .onSuccess { snackbar.showSnackbar(if (it == 0) "Keine neuen Gutscheine gefunden" else "✓ $it Gutscheine importiert") }
                .onFailure { snackbar.showSnackbar("Import fehlgeschlagen: ${it.message}") }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Einstellungen", fontWeight = FontWeight.Bold) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!canNotify) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🔕 Benachrichtigungen sind aus", fontWeight = FontWeight.Bold)
                        Text("Ohne Benachrichtigungen können wir dich nicht an ablaufende Gutscheine erinnern.")
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                openNotificationSettings(context)
                            }
                        }) { Text("Erlauben") }
                    }
                }
            }

            SectionTitle("Erinnerungen")
            SettingsCard {
                SwitchRow("Vor Ablauf erinnern", "Benachrichtigung, bevor ein Gutschein verfällt", enabled) {
                    enabled = it
                    settings.remindersEnabled = it
                }
                if (enabled) {
                    Spacer(Modifier.height(12.dp))
                    Text("Wann?", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Settings.REMINDER_DAY_OPTIONS.forEach { d ->
                            FilterChip(
                                selected = d in days,
                                onClick = {
                                    days = if (d in days) days - d else days + d
                                    settings.reminderDays = days
                                },
                                label = { Text(dayLabel(d)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Uhrzeit", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            hour = (hour + 23) % 24
                            settings.reminderHour = hour
                            ReminderScheduler.schedule(context, replace = true)
                        }) { Text("−", style = MaterialTheme.typography.titleLarge) }
                        Text("%02d:00 Uhr".format(hour), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = {
                            hour = (hour + 1) % 24
                            settings.reminderHour = hour
                            ReminderScheduler.schedule(context, replace = true)
                        }) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }

            SettingsCard {
                SwitchRow("Saisonale Ideen", "Einmal im Monat ein Vorschlag, was du mit deinen Gutscheinen machen könntest", ideas) {
                    ideas = it
                    settings.seasonalIdeasEnabled = it
                }
            }

            OutlinedButton(
                onClick = {
                    if (!Notifications.canPost(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Notifications.show(
                            context, 4242, Notifications.CHANNEL_EXPIRY,
                            "🎁 So sieht eine Erinnerung aus",
                            "REWE-Gutschein (25,00 €) läuft in 7 Tagen ab. Plane jetzt, wann du ihn einlöst.",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Test-Benachrichtigung senden") }

            SectionTitle("Sicherung")
            SettingsCard {
                Text(
                    "Alle Gutscheine liegen nur auf diesem Handy. Mach ab und zu eine Sicherung (z. B. in Google Drive), " +
                        "damit beim Handywechsel nichts verloren geht.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { exporter.launch("gutscheine-${LocalDate.now()}.zip") },
                        modifier = Modifier.weight(1f),
                    ) { Text("Sichern") }
                    OutlinedButton(
                        onClick = { importer.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Wiederherstellen") }
                }
            }

            SectionTitle("Über")
            SettingsCard {
                Text("Gutschein-Safe ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", fontWeight = FontWeight.SemiBold)
                Text(
                    "Deine Gutscheine immer dabei – mit Erinnerung, bevor sie verfallen. Keine Werbung, kein Konto, keine Cloud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun dayLabel(d: Int) = when (d) {
    0 -> "Am letzten Tag"
    1 -> "1 Tag vorher"
    else -> "$d Tage vorher"
}

private fun openNotificationSettings(context: android.content.Context) {
    context.startSafely(
        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
