package de.gutscheinsafe.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "vouchers")
data class Voucher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** What the voucher is for, e.g. "Wellness-Wochenende" or "Einkaufsgutschein". */
    val title: String,
    /** Where it can be redeemed, e.g. "Hotel Sonnenhof" or "REWE". */
    val provider: String,
    val category: String = Category.OTHER.name,
    /** Original value in euro (optional, e.g. "1 Massage" vouchers have none). */
    val value: Double? = null,
    /** What is left after partial redemption. Null = full value still available. */
    val remainingValue: Double? = null,
    val code: String = "",
    val pin: String = "",
    /** Valid until (inclusive), stored as LocalDate.toEpochDay(). Null = no expiry. */
    val expiryDate: Long? = null,
    val receivedDate: Long? = null,
    val giftedBy: String = "",
    val location: String = "",
    val website: String = "",
    val conditions: String = "",
    val notes: String = "",
    /** File names inside filesDir/images. */
    val imageFront: String? = null,
    val imageBack: String? = null,
    val isRedeemed: Boolean = false,
    val redeemedDate: Long? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val categoryEnum: Category get() = Category.fromName(category)

    /** Value still available: remaining value if set, otherwise the original value. */
    val currentValue: Double? get() = remainingValue ?: value

    fun daysLeft(today: LocalDate = LocalDate.now()): Long? =
        expiryDate?.let { it - today.toEpochDay() }

    fun status(today: LocalDate = LocalDate.now()): ExpiryStatus {
        if (isRedeemed) return ExpiryStatus.REDEEMED
        val d = daysLeft(today) ?: return ExpiryStatus.NO_EXPIRY
        return when {
            d < 0 -> ExpiryStatus.EXPIRED
            d <= 7 -> ExpiryStatus.URGENT
            d <= 30 -> ExpiryStatus.SOON
            d <= 90 -> ExpiryStatus.UPCOMING
            else -> ExpiryStatus.OK
        }
    }

    val isActive: Boolean get() = status() != ExpiryStatus.REDEEMED && status() != ExpiryStatus.EXPIRED
}

enum class ExpiryStatus { URGENT, SOON, UPCOMING, OK, NO_EXPIRY, EXPIRED, REDEEMED }

enum class Category(val label: String, val emoji: String) {
    WELLNESS("Wellness & Spa", "🧖"),
    SUPERMARKET("Supermarkt", "🛒"),
    RESTAURANT("Restaurant & Café", "🍽️"),
    TRAVEL("Hotel & Reise", "🏨"),
    EXPERIENCE("Erlebnis & Freizeit", "🎢"),
    CULTURE("Kino, Theater & Konzert", "🎭"),
    FASHION("Mode & Shopping", "🛍️"),
    BEAUTY("Beauty & Friseur", "💇"),
    SPORT("Sport & Fitness", "🏃"),
    ELECTRONICS("Technik", "📱"),
    BOOKS("Bücher & Medien", "📚"),
    HOME("Haus & Garten", "🪴"),
    OTHER("Sonstiges", "🎁");

    companion object {
        fun fromName(name: String): Category = entries.firstOrNull { it.name == name } ?: OTHER
    }
}
