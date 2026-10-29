package de.gutscheinsafe.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.gutscheinsafe.data.Category
import de.gutscheinsafe.data.Format
import de.gutscheinsafe.data.ImageStore
import de.gutscheinsafe.data.Voucher
import de.gutscheinsafe.ui.AppViewModel
import de.gutscheinsafe.ui.SectionTitle
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val DAY_MS = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(vm: AppViewModel, id: Long, onDone: (Long?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loaded by rememberSaveable { mutableStateOf(id == 0L) }
    var title by rememberSaveable { mutableStateOf("") }
    var provider by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.OTHER.name) }
    var value by rememberSaveable { mutableStateOf("") }
    var remaining by rememberSaveable { mutableStateOf("") }
    var expiry by rememberSaveable { mutableStateOf<Long?>(null) }
    var received by rememberSaveable { mutableStateOf<Long?>(null) }
    var code by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var giftedBy by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var website by rememberSaveable { mutableStateOf("") }
    var conditions by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var front by rememberSaveable { mutableStateOf<String?>(null) }
    var back by rememberSaveable { mutableStateOf<String?>(null) }
    // Photos added in this editing session (deleted again if not saved), joined by "|".
    var newImages by rememberSaveable { mutableStateOf("") }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraSide by rememberSaveable { mutableStateOf("front") }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var datePickerFor by rememberSaveable { mutableStateOf<String?>(null) }

    var base by rememberSaveable { mutableStateOf<Long?>(null) } // createdAt etc. of the existing voucher
    var existing by remember { mutableStateOf<Voucher?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            val v = vm.load(id) ?: return@LaunchedEffect
            existing = v
            if (!loaded) {
                title = v.title
                provider = v.provider
                category = v.category
                value = Format.moneyInput(v.value)
                remaining = Format.moneyInput(v.remainingValue)
                expiry = v.expiryDate
                received = v.receivedDate
                code = v.code
                pin = v.pin
                giftedBy = v.giftedBy
                location = v.location
                website = v.website
                conditions = v.conditions
                notes = v.notes
                front = v.imageFront
                back = v.imageBack
                base = v.createdAt
                loaded = true
            }
        }
    }

    fun addImage(name: String, side: String) {
        newImages = if (newImages.isEmpty()) name else "$newImages|$name"
        if (side == "front") front = name else back = name
    }

    fun handlePicked(uri: Uri?, side: String) {
        if (uri == null) return
        scope.launch { vm.importImage(uri)?.let { addImage(it, side) } }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri?.let(Uri::parse)
        if (ok && uri != null) handlePicked(uri, cameraSide)
    }
    var gallerySide by rememberSaveable { mutableStateOf("front") }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        handlePicked(uri, gallerySide)
    }

    fun takePhoto(side: String) {
        val uri = ImageStore.newCameraUri(context)
        cameraUri = uri.toString()
        cameraSide = side
        camera.launch(uri)
    }

    fun pickPhoto(side: String) {
        gallerySide = side
        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun cancel() {
        newImages.split('|').filter { it.isNotBlank() }.forEach { vm.discardImage(it) }
        onDone(null)
    }

    fun save() {
        if (id != 0L && existing == null) return // still loading
        if (provider.isBlank() && title.isBlank()) {
            showErrors = true
            return
        }
        val parsedValue = Format.parseMoney(value)
        val parsedRemaining = Format.parseMoney(remaining)?.takeIf { it != parsedValue }
        val old = existing
        val v = (old ?: Voucher(title = "", provider = "")).copy(
            title = title.trim().ifBlank { "Gutschein" },
            provider = provider.trim().ifBlank { title.trim() },
            category = category,
            value = parsedValue,
            remainingValue = parsedRemaining,
            expiryDate = expiry,
            receivedDate = received,
            code = code.trim(),
            pin = pin.trim(),
            giftedBy = giftedBy.trim(),
            location = location.trim(),
            website = website.trim(),
            conditions = conditions.trim(),
            notes = notes.trim(),
            imageFront = front,
            imageBack = back,
            createdAt = base ?: old?.createdAt ?: System.currentTimeMillis(),
        ).let {
            // A remaining value of 0 means fully used.
            if (parsedRemaining != null && parsedRemaining <= 0.0 && !it.isRedeemed) {
                it.copy(isRedeemed = true, redeemedDate = LocalDate.now().toEpochDay())
            } else {
                it
            }
        }
        newImages.split('|').filter { it.isNotBlank() && it != front && it != back }.forEach { vm.discardImage(it) }
        vm.save(v) { savedId -> onDone(savedId) }
    }

    BackHandler { cancel() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "Neuer Gutschein" else "Bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = { cancel() }) { Icon(Icons.Filled.Close, contentDescription = "Abbrechen") }
                },
                actions = {
                    TextButton(onClick = { save() }) { Text("Speichern", fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { padding ->
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Photos
            SectionTitle("Fotos")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoSlot(
                    label = "Vorderseite",
                    image = front,
                    onCamera = { takePhoto("front") },
                    onGallery = { pickPhoto("front") },
                    onRemove = { front = null },
                    modifier = Modifier.weight(1f),
                )
                PhotoSlot(
                    label = "Rückseite",
                    image = back,
                    onCamera = { takePhoto("back") },
                    onGallery = { pickPhoto("back") },
                    onRemove = { back = null },
                    modifier = Modifier.weight(1f),
                )
            }

            SectionTitle("Was & wo?")
            OutlinedTextField(
                value = provider,
                onValueChange = { provider = it },
                label = { Text("Wo einlösbar? *") },
                placeholder = { Text("z. B. REWE, Hotel Sonnenhof, Therme Erding") },
                isError = showErrors && provider.isBlank() && title.isBlank(),
                supportingText = {
                    if (showErrors && provider.isBlank() && title.isBlank()) Text("Bitte angeben, wo der Gutschein gilt")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Wofür?") },
                placeholder = { Text("z. B. Wellness-Wochenende für 2, Einkaufsgutschein") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Kategorie", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { c ->
                    FilterChip(
                        selected = category == c.name,
                        onClick = { category = c.name },
                        label = { Text("${c.emoji} ${c.label}") },
                    )
                }
            }

            SectionTitle("Wert")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Wert in €") },
                    placeholder = { Text("optional") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                if (id != 0L) {
                    OutlinedTextField(
                        value = remaining,
                        onValueChange = { remaining = it },
                        label = { Text("Restguthaben €") },
                        placeholder = { Text("falls teilweise") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SectionTitle("Gültigkeit")
            DateField(
                label = "Gültig bis",
                value = expiry,
                emptyText = "Unbefristet / unbekannt",
                onClick = { datePickerFor = "expiry" },
                onClear = { expiry = null },
            )
            val baseYear = received?.let { LocalDate.ofEpochDay(it).year } ?: LocalDate.now().year
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { expiry = LocalDate.of(baseYear + 3, 12, 31).toEpochDay() },
                    label = { Text("3-Jahres-Frist (31.12.${baseYear + 3})") },
                )
                AssistChip(
                    onClick = { expiry = LocalDate.now().plusYears(1).toEpochDay() },
                    label = { Text("+1 Jahr") },
                )
                AssistChip(
                    onClick = { expiry = LocalDate.now().plusYears(2).toEpochDay() },
                    label = { Text("+2 Jahre") },
                )
            }
            Text(
                "Tipp: Ohne aufgedrucktes Datum gilt in Deutschland meist die 3-Jahres-Frist – ab Ende des Jahres, in dem du den Gutschein bekommen hast.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DateField(
                label = "Erhalten am",
                value = received,
                emptyText = "optional",
                onClick = { datePickerFor = "received" },
                onClear = { received = null },
            )

            SectionTitle("Code")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Gutscheincode / Nr.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionTitle("Zusatzinfos")
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Ort / Adresse") },
                placeholder = { Text("z. B. Hauptstraße 1, München – oder „alle Filialen“") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text("Website / Online-Shop") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = giftedBy,
                onValueChange = { giftedBy = it },
                label = { Text("Geschenk von") },
                placeholder = { Text("z. B. Oma zum Geburtstag") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = conditions,
                onValueChange = { conditions = it },
                label = { Text("Bedingungen") },
                placeholder = { Text("z. B. nur Mo–Do, Reservierung nötig, nicht online") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notizen") },
                placeholder = { Text("z. B. mit Anna einlösen, Telefonnummer fürs Buchen …") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Gutschein speichern", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    datePickerFor?.let { which ->
        DatePickerModal(
            initial = if (which == "expiry") expiry else received,
            onPick = { day ->
                if (which == "expiry") expiry = day else received = day
                datePickerFor = null
            },
            onDismiss = { datePickerFor = null },
        )
    }
}

@Composable
private fun PhotoSlot(
    label: String,
    image: String?,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    OutlinedCard(
        modifier = modifier.height(170.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (image != null) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageStore.file(context, image),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clickable(onClick = onCamera),
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Foto entfernen", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(
                    label,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCamera) { Text("📷 Foto") }
                TextButton(onClick = onGallery) { Text("🖼️ Galerie") }
            }
        }
    }
}

@Composable
private fun DateField(label: String, value: Long?, emptyText: String, onClick: () -> Unit, onClear: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value?.let { Format.dateLong(it) } ?: emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (value != null) {
                IconButton(onClick = onClear) { Icon(Icons.Filled.Close, contentDescription = "Entfernen") }
            } else {
                Text("📅", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(initial: Long?, onPick: (Long?) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial?.let { it * DAY_MS })
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(state.selectedDateMillis?.let { Math.floorDiv(it, DAY_MS) }) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    ) {
        DatePicker(state = state)
    }
}
