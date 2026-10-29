package de.gutscheinsafe.data

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    private val german = Locale.GERMANY
    private val date = DateTimeFormatter.ofPattern("dd.MM.yyyy", german)
    private val dateLong = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", german)

    fun money(value: Double): String = NumberFormat.getCurrencyInstance(german).format(value)

    fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(date)

    fun dateLong(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateLong)

    /** "noch 12 Tage", "läuft heute ab", "seit 3 Tagen abgelaufen" ... */
    fun daysLeft(days: Long): String = when {
        days < -1 -> "seit ${-days} Tagen abgelaufen"
        days == -1L -> "seit gestern abgelaufen"
        days == 0L -> "läuft heute ab!"
        days == 1L -> "läuft morgen ab"
        days < 60 -> "noch $days Tage"
        days < 730 -> "noch ${days / 30} Monate"
        else -> "noch ${days / 365} Jahre"
    }

    /** Parses "12,50" / "12.50" / "12 €" into a number. */
    fun parseMoney(text: String): Double? {
        val t = text.replace("€", "").replace(" ", "").trim()
        if (t.isEmpty()) return null
        val normalized = if (t.contains(',')) t.replace(".", "").replace(',', '.') else t
        return normalized.toDoubleOrNull()
    }

    fun moneyInput(value: Double?): String =
        value?.let { if (it % 1.0 == 0.0) it.toLong().toString() else String.format(german, "%.2f", it) } ?: ""
}
