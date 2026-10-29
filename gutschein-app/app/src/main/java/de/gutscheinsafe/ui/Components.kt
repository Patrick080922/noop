package de.gutscheinsafe.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.gutscheinsafe.data.ExpiryStatus
import de.gutscheinsafe.data.Format
import de.gutscheinsafe.data.ImageStore
import de.gutscheinsafe.data.Voucher
import de.gutscheinsafe.ui.theme.StatusColors
import java.time.LocalDate
import java.time.ZoneId

fun statusColor(status: ExpiryStatus): Color = when (status) {
    ExpiryStatus.URGENT, ExpiryStatus.EXPIRED -> StatusColors.urgent
    ExpiryStatus.SOON -> StatusColors.soon
    ExpiryStatus.UPCOMING -> StatusColors.upcoming
    ExpiryStatus.OK, ExpiryStatus.NO_EXPIRY -> StatusColors.ok
    ExpiryStatus.REDEEMED -> StatusColors.neutral
}

fun statusText(v: Voucher): String = when (val s = v.status()) {
    ExpiryStatus.REDEEMED -> "Eingelöst"
    ExpiryStatus.NO_EXPIRY -> "Unbefristet"
    ExpiryStatus.EXPIRED -> "Abgelaufen"
    else -> v.daysLeft()?.let { Format.daysLeft(it) } ?: s.name
}

@Composable
fun ExpiryBadge(voucher: Voucher, modifier: Modifier = Modifier) {
    val color = statusColor(voucher.status())
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
    ) {
        Text(
            statusText(voucher),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun VoucherThumb(voucher: Voucher, size: Dp = 64.dp) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(14.dp)
    if (voucher.imageFront != null) {
        AsyncImage(
            model = ImageStore.file(context, voucher.imageFront),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(shape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(voucher.categoryEnum.emoji, fontSize = (size.value * 0.45f).sp)
        }
    }
}

@Composable
fun VoucherCard(voucher: Voucher, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimmed = !voucher.isActive
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoucherThumb(voucher)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        voucher.provider.ifBlank { voucher.title },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    if (voucher.isFavorite) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Favorite, contentDescription = "Favorit",
                            tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp),
                        )
                    }
                }
                if (voucher.title.isNotBlank() && voucher.title != voucher.provider) {
                    Text(
                        voucher.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.size(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpiryBadge(voucher)
                    voucher.expiryDate?.let {
                        Text(
                            "bis ${Format.date(it)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            voucher.currentValue?.let {
                Spacer(Modifier.width(8.dp))
                Text(
                    Format.money(it),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun EmptyState(emoji: String, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(PaddingValues(horizontal = 32.dp, vertical = 48.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 56.sp)
        Spacer(Modifier.size(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "Keine passende App gefunden", Toast.LENGTH_SHORT).show()
    }
}

/** Opens the calendar app with a prefilled all-day event to plan redeeming the voucher. */
fun Context.planInCalendar(v: Voucher, onDay: LocalDate? = null) {
    val day = onDay ?: v.expiryDate?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now().plusDays(7)
    val start = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.Events.TITLE, "🎁 Gutschein einlösen: ${v.provider.ifBlank { v.title }}")
        putExtra(
            CalendarContract.Events.DESCRIPTION,
            buildString {
                append(v.title)
                v.currentValue?.let { append(" – ${Format.money(it)}") }
                v.expiryDate?.let { append("\nGültig bis ${Format.date(it)}") }
                if (v.code.isNotBlank()) append("\nCode: ${v.code}")
            },
        )
        if (v.location.isNotBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, v.location)
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 24 * 60 * 60 * 1000L)
    }
    startSafely(intent)
}

fun Context.openMap(query: String) =
    startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))))

fun Context.openWebsite(url: String) {
    val full = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
    startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(full)))
}
