package de.gutscheinsafe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gutscheinsafe.data.ExpiryStatus
import de.gutscheinsafe.data.Format
import de.gutscheinsafe.data.Voucher
import de.gutscheinsafe.ui.AppViewModel
import de.gutscheinsafe.ui.EmptyState
import de.gutscheinsafe.ui.VoucherCard

private enum class Filter(val label: String) {
    ACTIVE("Aktiv"),
    SOON("Bald fällig"),
    FAVORITES("Favoriten"),
    ARCHIVE("Archiv"),
}

private fun Voucher.matches(filter: Filter): Boolean = when (filter) {
    Filter.ACTIVE -> isActive
    Filter.SOON -> isActive && (daysLeft() ?: Long.MAX_VALUE) <= 30
    Filter.FAVORITES -> isFavorite && isActive
    Filter.ARCHIVE -> !isActive
}

private fun Voucher.matches(query: String): Boolean {
    if (query.isBlank()) return true
    return listOf(title, provider, notes, code, location, giftedBy, categoryEnum.label)
        .any { it.contains(query.trim(), ignoreCase = true) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, onOpen: (Long) -> Unit, onAdd: () -> Unit) {
    val all by vm.vouchers.collectAsStateWithLifecycle()
    var filterName by rememberSaveable { mutableStateOf(Filter.ACTIVE.name) }
    val filter = Filter.valueOf(filterName)
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meine Gutscheine", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        searching = !searching
                        if (!searching) query = ""
                    }) {
                        Icon(if (searching) Icons.Filled.Clear else Icons.Filled.Search, contentDescription = "Suchen")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Gutschein") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        val vouchers = all
        if (vouchers == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val shown = vouchers.filter {
            it.matches(filter) && it.matches(query) && (categoryName == null || it.category == categoryName)
        }.let { list -> if (filter == Filter.ARCHIVE) list.sortedByDescending { it.expiryDate ?: 0 } else list }
        val usedCategories = vouchers.filter { it.matches(filter) }.map { it.categoryEnum }.distinct()
            .sortedBy { it.ordinal }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (searching) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Suchen nach Name, Ort, Code …") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (vouchers.isEmpty()) {
                item {
                    EmptyState(
                        "🎁",
                        "Noch keine Gutscheine",
                        "Fotografiere deinen ersten Gutschein und trage ein, bis wann er gilt. " +
                            "Wir erinnern dich rechtzeitig, bevor er abläuft.",
                    )
                }
                return@LazyColumn
            }

            if (!searching) {
                item { SummaryCard(vouchers) }
                val urgent = vouchers.count { it.status() == ExpiryStatus.URGENT }
                if (urgent > 0 && filter != Filter.SOON) {
                    item { UrgentBanner(urgent) { filterName = Filter.SOON.name } }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Filter.entries.toList()) { f ->
                        val count = vouchers.count { it.matches(f) }
                        FilterChip(
                            selected = filter == f,
                            onClick = {
                                filterName = f.name
                                categoryName = null
                            },
                            label = { Text("${f.label} ($count)") },
                        )
                    }
                }
            }
            if (usedCategories.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(usedCategories) { c ->
                            FilterChip(
                                selected = categoryName == c.name,
                                onClick = { categoryName = if (categoryName == c.name) null else c.name },
                                label = { Text("${c.emoji} ${c.label}") },
                            )
                        }
                    }
                }
            }

            if (shown.isEmpty()) {
                item {
                    EmptyState(
                        when (filter) {
                            Filter.SOON -> "😌"
                            Filter.ARCHIVE -> "🗂️"
                            Filter.FAVORITES -> "🤍"
                            Filter.ACTIVE -> "🔍"
                        },
                        when (filter) {
                            Filter.SOON -> "Nichts läuft bald ab"
                            Filter.ARCHIVE -> "Archiv ist leer"
                            Filter.FAVORITES -> "Keine Favoriten"
                            Filter.ACTIVE -> "Nichts gefunden"
                        },
                        when (filter) {
                            Filter.SOON -> "In den nächsten 30 Tagen verfällt keiner deiner Gutscheine."
                            Filter.ARCHIVE -> "Eingelöste und abgelaufene Gutscheine landen hier."
                            Filter.FAVORITES -> "Markiere Gutscheine mit dem Herz, um sie hier zu sammeln."
                            Filter.ACTIVE -> "Keine aktiven Gutscheine passen zu deiner Auswahl."
                        },
                    )
                }
            } else {
                items(shown, key = { it.id }) { v ->
                    VoucherCard(v, onClick = { onOpen(v.id) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(vouchers: List<Voucher>) {
    val active = vouchers.filter { it.isActive }
    val total = active.sumOf { it.currentValue ?: 0.0 }
    val soon = active.count { (it.daysLeft() ?: Long.MAX_VALUE) <= 30 }
    val withoutValue = active.count { it.currentValue == null }

    Surface(shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF14B8A6))),
                    RoundedCornerShape(24.dp),
                )
                .padding(20.dp),
        ) {
            Text("Verfügbares Guthaben", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            Text(
                Format.money(total),
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            if (withoutValue > 0) {
                Text(
                    "+ $withoutValue ${if (withoutValue == 1) "Gutschein" else "Gutscheine"} ohne Betrag (z. B. Erlebnisse)",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row {
                Stat("${active.size}", if (active.size == 1) "aktiver Gutschein" else "aktive Gutscheine", Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Stat("$soon", "laufen in 30 Tagen ab", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun UrgentBanner(count: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⏰", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (count == 1) "1 Gutschein läuft diese Woche ab" else "$count Gutscheine laufen diese Woche ab",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    "Tippen zum Anzeigen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
