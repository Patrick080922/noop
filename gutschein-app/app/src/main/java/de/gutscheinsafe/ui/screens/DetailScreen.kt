package de.gutscheinsafe.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.gutscheinsafe.data.Format
import de.gutscheinsafe.data.ImageStore
import de.gutscheinsafe.data.Voucher
import de.gutscheinsafe.ui.AppViewModel
import de.gutscheinsafe.ui.ExpiryBadge
import de.gutscheinsafe.ui.openMap
import de.gutscheinsafe.ui.openWebsite
import de.gutscheinsafe.ui.planInCalendar
import de.gutscheinsafe.ui.startSafely
import de.gutscheinsafe.ui.statusColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    vm: AppViewModel,
    id: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onImage: (String) -> Unit,
) {
    val flow = remember(id) { vm.voucher(id) }
    val voucher by flow.collectAsState(initial = null)
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var editRemaining by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(voucher?.provider ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    voucher?.let { v ->
                        IconButton(onClick = { vm.toggleFavorite(v) }) {
                            Icon(
                                if (v.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorit",
                                tint = if (v.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Bearbeiten") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Löschen") }
                    }
                },
            )
        },
    ) { padding ->
        val v = voucher
        if (v == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Images(v, onImage)

            // Header
            Column {
                Text(
                    "${v.categoryEnum.emoji} ${v.categoryEnum.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(v.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(v.provider, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Validity
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Gültig bis", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            v.expiryDate?.let { Format.dateLong(it) } ?: "Unbefristet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor(v.status()),
                        )
                    }
                    ExpiryBadge(v)
                }
                if (v.isRedeemed && v.redeemedDate != null) {
                    Text(
                        "Eingelöst am ${Format.date(v.redeemedDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Value + redeem actions
            InfoCard {
                if (v.value != null) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Wert", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Format.money(v.value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        if (v.remainingValue != null && v.remainingValue != v.value) {
                            Column(Modifier.weight(1f)) {
                                Text("Restguthaben", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Format.money(v.remainingValue),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (v.isRedeemed) {
                    OutlinedButton(onClick = { vm.setRedeemed(v, false) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Wieder als offen markieren")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (v.value != null) {
                            OutlinedButton(onClick = { editRemaining = true }, modifier = Modifier.weight(1f)) {
                                Text("Teilweise")
                            }
                        }
                        Button(onClick = { vm.setRedeemed(v, true) }, modifier = Modifier.weight(1f)) {
                            Text("✓ Eingelöst")
                        }
                    }
                }
            }

            // Code + PIN
            if (v.code.isNotBlank() || v.pin.isNotBlank()) {
                InfoCard {
                    if (v.code.isNotBlank()) CopyRow(context, "Gutscheincode", v.code, secret = false)
                    if (v.pin.isNotBlank()) CopyRow(context, "PIN", v.pin, secret = true)
                }
            }

            // Details
            val rows = buildList<Triple<String, String, (() -> Unit)?>> {
                if (v.location.isNotBlank()) add(Triple("📍 Ort", v.location) { context.openMap(v.location) })
                if (v.website.isNotBlank()) add(Triple("🌐 Website", v.website) { context.openWebsite(v.website) })
                if (v.giftedBy.isNotBlank()) add(Triple("💝 Geschenk von", v.giftedBy, null))
                v.receivedDate?.let { add(Triple("📅 Erhalten am", Format.date(it), null)) }
                if (v.conditions.isNotBlank()) add(Triple("📋 Bedingungen", v.conditions, null))
                if (v.notes.isNotBlank()) add(Triple("📝 Notizen", v.notes, null))
            }
            if (rows.isNotEmpty()) {
                InfoCard {
                    rows.forEachIndexed { i, (label, value, action) ->
                        if (i > 0) Spacer(Modifier.height(10.dp))
                        Column(if (action != null) Modifier.fillMaxWidth().clickable { action() } else Modifier.fillMaxWidth()) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                value,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (action != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { context.planInCalendar(v) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Im Kalender")
                }
                FilledTonalButton(onClick = { share(context, v) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Teilen")
                }
            }
            if (v.location.isNotBlank() || v.provider.isNotBlank()) {
                OutlinedButton(
                    onClick = { context.openMap(v.location.ifBlank { v.provider }) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("In Karten öffnen")
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Gutschein löschen?") },
                text = { Text("„${v.title}“ und die Fotos werden endgültig gelöscht.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        vm.delete(v)
                        onBack()
                    }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
            )
        }
        if (editRemaining) {
            RemainingDialog(
                voucher = v,
                onDismiss = { editRemaining = false },
                onSave = {
                    vm.setRemaining(v, it)
                    editRemaining = false
                },
            )
        }
    }
}

@Composable
private fun Images(v: Voucher, onImage: (String) -> Unit) {
    val context = LocalContext.current
    val images = listOfNotNull(v.imageFront?.let { "front" to it }, v.imageBack?.let { "back" to it })
    if (images.isEmpty()) return
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            images.forEach { (side, name) ->
                AsyncImage(
                    model = ImageStore.file(context, name),
                    contentDescription = if (side == "front") "Vorderseite" else "Rückseite",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(if (side == "front") 2f else 1f)
                        .height(200.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onImage(side) },
                )
            }
        }
        Text(
            "Tippen für Vollbild – z. B. zum Vorzeigen an der Kasse",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
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
private fun CopyRow(context: Context, label: String, value: String, secret: Boolean) {
    var visible by rememberSaveable { mutableStateOf(!secret) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (visible) value else "•".repeat(value.length.coerceAtMost(8)),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (secret) {
            TextButton(onClick = { visible = !visible }) { Text(if (visible) "Verbergen" else "Zeigen") }
        }
        TextButton(onClick = {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
            Toast.makeText(context, "$label kopiert", Toast.LENGTH_SHORT).show()
        }) { Text("Kopieren") }
    }
}

@Composable
private fun RemainingDialog(voucher: Voucher, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(Format.moneyInput(voucher.currentValue)) }
    var spent by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teilweise eingelöst") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Wie viel hast du ausgegeben? Oder trag direkt das neue Restguthaben ein.")
                OutlinedTextField(
                    value = spent,
                    onValueChange = { s ->
                        spent = s
                        val amount = Format.parseMoney(s)
                        val base = voucher.currentValue
                        if (amount != null && base != null) text = Format.moneyInput((base - amount).coerceAtLeast(0.0))
                    },
                    label = { Text("Ausgegeben (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Neues Restguthaben (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { Format.parseMoney(text)?.let(onSave) },
                enabled = Format.parseMoney(text) != null,
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

private fun share(context: Context, v: Voucher) {
    val text = buildString {
        append("🎁 ${v.title} – ${v.provider}")
        v.currentValue?.let { append("\nWert: ${Format.money(it)}") }
        v.expiryDate?.let { append("\nGültig bis: ${Format.date(it)}") }
        if (v.code.isNotBlank()) append("\nCode: ${v.code}")
        if (v.location.isNotBlank()) append("\nOrt: ${v.location}")
        if (v.conditions.isNotBlank()) append("\nBedingungen: ${v.conditions}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        val image = v.imageFront
        if (image != null) {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, ImageStore.shareUri(context, image))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
        }
    }
    context.startSafely(Intent.createChooser(intent, "Gutschein teilen"))
}
