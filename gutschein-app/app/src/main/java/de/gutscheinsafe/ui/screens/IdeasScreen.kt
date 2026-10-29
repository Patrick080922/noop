package de.gutscheinsafe.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.gutscheinsafe.data.Format
import de.gutscheinsafe.data.Voucher
import de.gutscheinsafe.ideas.Suggestion
import de.gutscheinsafe.ideas.Suggestions
import de.gutscheinsafe.ui.AppViewModel
import de.gutscheinsafe.ui.ExpiryBadge
import de.gutscheinsafe.ui.SectionTitle
import de.gutscheinsafe.ui.VoucherThumb
import de.gutscheinsafe.ui.planInCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeasScreen(vm: AppViewModel, onOpen: (Long) -> Unit) {
    val all by vm.vouchers.collectAsStateWithLifecycle()
    val vouchers = all.orEmpty()
    val context = LocalContext.current
    val season = Suggestions.season()
    val active = vouchers.filter { it.isActive }
    val dueSoon = active.filter { (it.daysLeft() ?: Long.MAX_VALUE) <= 60 }
    val suggestions = Suggestions.forDate(vouchers)
    val expiringValue = active.filter { (it.daysLeft() ?: Long.MAX_VALUE) <= 90 }.sumOf { it.currentValue ?: 0.0 }

    Scaffold(topBar = { TopAppBar(title = { Text("Ideen & Planung", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("${season.emoji} ${season.label}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (active.isEmpty()) "Sobald du Gutscheine speicherst, schlagen wir dir hier passende Aktivitäten vor."
                            else "Was du in nächster Zeit mit deinen ${active.size} Gutscheinen machen könntest.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (expiringValue > 0) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "💸 ${Format.money(expiringValue)} verfallen in den nächsten 3 Monaten, wenn du nichts unternimmst.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            item { SectionTitle("Bald einlösen") }
            if (dueSoon.isEmpty()) {
                item {
                    Text(
                        "Nichts dringend in den nächsten 2 Monaten 👍",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(dueSoon, key = { "due_${it.id}" }) { v ->
                    DueCard(v, onOpen = { onOpen(v.id) }, onPlan = { context.planInCalendar(v, suggestedDay(v)) })
                }
            }

            item { SectionTitle("Passend zur Jahreszeit") }
            items(suggestions, key = { "idea_${it.idea.title}" }) { s -> IdeaCard(s, onOpen) }
        }
    }
}

/** Suggest the next Saturday that is at least a few days before the expiry date. */
private fun suggestedDay(v: Voucher): java.time.LocalDate {
    val today = java.time.LocalDate.now()
    val expiry = v.expiryDate?.let { java.time.LocalDate.ofEpochDay(it) } ?: today.plusDays(14)
    var day = today.plusDays(1)
    while (day.dayOfWeek != java.time.DayOfWeek.SATURDAY) day = day.plusDays(1)
    return if (day.isAfter(expiry)) expiry else day
}

@Composable
private fun DueCard(v: Voucher, onOpen: () -> Unit, onPlan: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            VoucherThumb(v, size = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(v.provider, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    v.title + (v.currentValue?.let { " · ${Format.money(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                ExpiryBadge(v)
            }
            FilledTonalButton(onClick = onPlan, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("Planen")
            }
        }
    }
}

@Composable
private fun IdeaCard(s: Suggestion, onOpen: (Long) -> Unit) {
    val hasMatch = s.matches.isNotEmpty()
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasMatch) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.idea.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(10.dp))
                Text(s.idea.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(s.idea.text, style = MaterialTheme.typography.bodyMedium)
            if (hasMatch) {
                Spacer(Modifier.height(8.dp))
                Text("Dazu passt:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                s.matches.take(3).forEach { v ->
                    Text(
                        "→ ${v.provider}: ${v.title}" + (v.expiryDate?.let { " (bis ${Format.date(it)})" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(v.id) }.padding(vertical = 4.dp),
                    )
                }
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Noch kein passender Gutschein – vielleicht eine Geschenkidee?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
