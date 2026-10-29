package de.gutscheinsafe.ideas

import de.gutscheinsafe.data.Category
import de.gutscheinsafe.data.Voucher
import java.time.LocalDate
import java.time.Month

enum class Season(val label: String, val emoji: String) {
    WINTER("Winter", "❄️"),
    SPRING("Frühling", "🌷"),
    SUMMER("Sommer", "☀️"),
    AUTUMN("Herbst", "🍂");

    companion object {
        fun of(date: LocalDate): Season = when (date.month) {
            Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> WINTER
            Month.MARCH, Month.APRIL, Month.MAY -> SPRING
            Month.JUNE, Month.JULY, Month.AUGUST -> SUMMER
            else -> AUTUMN
        }
    }
}

data class Idea(
    val title: String,
    val text: String,
    val emoji: String,
    val categories: Set<Category>,
)

/** An idea together with the user's own vouchers that fit it. */
data class Suggestion(val idea: Idea, val matches: List<Voucher>)

object Suggestions {

    private val seasonal: Map<Season, List<Idea>> = mapOf(
        Season.WINTER to listOf(
            Idea("Sauna-Tag gegen die Kälte", "Draußen grau und kalt – perfekt für Therme, Sauna oder ein Wellness-Wochenende.", "🧖", setOf(Category.WELLNESS, Category.TRAVEL)),
            Idea("Kultur-Abend", "Lange Abende sind ideal für Kino, Theater oder ein Konzert.", "🎭", setOf(Category.CULTURE)),
            Idea("Gemütlich essen gehen", "Fondue, Raclette oder Lieblingsrestaurant – gönn dir einen warmen Abend.", "🫕", setOf(Category.RESTAURANT)),
            Idea("Lesestoff für die Couch", "Bücher-Gutscheine jetzt einlösen und eingekuschelt lesen.", "📚", setOf(Category.BOOKS)),
            Idea("Indoor-Aktivität", "Bouldern, Bowling, Escape-Room oder Trampolinhalle – Spaß ohne Frieren.", "🧗", setOf(Category.EXPERIENCE, Category.SPORT)),
        ),
        Season.SPRING to listOf(
            Idea("Frühjahrs-Garderobe", "Neue Saison, neue Kollektion – ein guter Moment für Mode-Gutscheine.", "🛍️", setOf(Category.FASHION, Category.BEAUTY)),
            Idea("Balkon & Garten starten", "Pflanzen, Erde, Gartenmöbel: Jetzt beginnt die Saison.", "🪴", setOf(Category.HOME)),
            Idea("Rauf aufs Rad", "Laufschuhe, Fahrrad-Check oder Fitness-Kurs für den Frühling.", "🚴", setOf(Category.SPORT)),
            Idea("Saisonstart Freizeitparks", "Viele Parks, Zoos und Ausflugsziele öffnen wieder.", "🎢", setOf(Category.EXPERIENCE)),
            Idea("Spargelzeit & Biergarten", "Erste warme Tage – raus zum Essen!", "🥗", setOf(Category.RESTAURANT)),
        ),
        Season.SUMMER to listOf(
            Idea("Ab ins Abenteuer", "Kletterpark, Kanu, Freibad oder Freizeitpark – der Sommer ruft.", "🛶", setOf(Category.EXPERIENCE, Category.SPORT)),
            Idea("Kurztrip planen", "Hotel-Gutscheine für ein langes Wochenende nutzen.", "🏨", setOf(Category.TRAVEL, Category.WELLNESS)),
            Idea("Eis & Terrasse", "Draußen sitzen und genießen – Restaurant- und Café-Gutscheine einlösen.", "🍦", setOf(Category.RESTAURANT)),
            Idea("Open-Air-Kultur", "Open-Air-Kino, Festivals und Sommerkonzerte.", "🎶", setOf(Category.CULTURE)),
            Idea("Sommer-Sale", "Viele Läden reduzieren jetzt – Mode-Gutscheine gehen doppelt weit.", "🏷️", setOf(Category.FASHION)),
        ),
        Season.AUTUMN to listOf(
            Idea("Wellness im Herbst", "Wenn's draußen ungemütlich wird: Spa, Massage oder Wellness-Hotel.", "🧖", setOf(Category.WELLNESS, Category.TRAVEL, Category.BEAUTY)),
            Idea("Kino-Abend", "Herbstzeit ist Kinozeit – such dir einen Film aus.", "🍿", setOf(Category.CULTURE)),
            Idea("Kürbis- & Wildwochen", "Viele Restaurants haben jetzt saisonale Herbstkarten.", "🎃", setOf(Category.RESTAURANT)),
            Idea("Herbstjacke & Stiefel", "Zeit für warme Sachen.", "🧥", setOf(Category.FASHION)),
            Idea("Neue Bücher & Spiele", "Für lange Abende zu Hause.", "📚", setOf(Category.BOOKS, Category.ELECTRONICS)),
        ),
    )

    /** Special occasions in certain months. */
    private fun occasions(month: Month): List<Idea> = when (month) {
        Month.FEBRUARY -> listOf(Idea("Valentinstag (14.2.)", "Romantisches Dinner oder ein Wellness-Tag zu zweit?", "💝", setOf(Category.RESTAURANT, Category.WELLNESS)))
        Month.MAY -> listOf(Idea("Muttertag", "Ein Wellness- oder Beauty-Gutschein ist ein schönes Erlebnis zum Verschenken oder gemeinsam Einlösen.", "💐", setOf(Category.WELLNESS, Category.BEAUTY, Category.RESTAURANT)))
        Month.NOVEMBER -> listOf(Idea("Black Friday & Weihnachtseinkäufe", "Technik- und Mode-Gutscheine mit Rabattaktionen kombinieren.", "🎁", setOf(Category.ELECTRONICS, Category.FASHION, Category.BOOKS)))
        Month.DECEMBER -> listOf(
            Idea("Weihnachtsgeschenke", "Gutscheine für Bücher, Mode oder Technik für Geschenke nutzen.", "🎄", setOf(Category.BOOKS, Category.FASHION, Category.ELECTRONICS, Category.HOME)),
            Idea("Jahresende: Fristen prüfen!", "Viele Gutscheine verfallen am 31.12. (gesetzliche 3-Jahres-Frist). Schau, was noch offen ist.", "⏰", Category.entries.toSet()),
        )
        Month.AUGUST, Month.SEPTEMBER -> listOf(Idea("Zurück in den Alltag", "Neue Sportroutine nach dem Urlaub? Fitness-Gutscheine einlösen.", "💪", setOf(Category.SPORT)))
        else -> emptyList()
    }

    private val always = listOf(
        Idea("Beim nächsten Wocheneinkauf", "Supermarkt-Gutscheine einfach beim nächsten Einkauf an der Kasse zeigen.", "🛒", setOf(Category.SUPERMARKET)),
    )

    fun season(today: LocalDate = LocalDate.now()) = Season.of(today)

    /**
     * Ideas for right now. Ideas that match one of the user's active vouchers come first, so the
     * list doubles as "what could I do with my vouchers next?".
     */
    fun forDate(vouchers: List<Voucher>, today: LocalDate = LocalDate.now()): List<Suggestion> {
        val active = vouchers.filter { it.isActive }
        val ideas = occasions(today.month) + seasonal.getValue(Season.of(today)) + always
        return ideas
            .map { idea ->
                Suggestion(
                    idea,
                    active.filter { it.categoryEnum in idea.categories }
                        .sortedBy { it.expiryDate ?: Long.MAX_VALUE },
                )
            }
            .filter { it.matches.isNotEmpty() || it.idea.categories.size < Category.entries.size }
            .sortedByDescending { it.matches.isNotEmpty() }
    }

    /** One short line for the monthly notification, or null if nothing fits. */
    fun monthlyHeadline(vouchers: List<Voucher>, today: LocalDate = LocalDate.now()): Pair<String, String>? {
        val best = forDate(vouchers, today).firstOrNull { it.matches.isNotEmpty() } ?: return null
        val v = best.matches.first()
        val title = "${best.idea.emoji} ${best.idea.title}"
        val text = "${best.idea.text} Du hast z. B.: ${v.title} – ${v.provider}."
        return title to text
    }
}
