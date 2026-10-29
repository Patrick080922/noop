package de.gutscheinsafe.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup as a single ZIP file (vouchers.json + photos). Lets the user move all vouchers
 * to a new phone or keep a copy in the cloud.
 */
object Backup {
    private const val JSON_NAME = "vouchers.json"
    private const val IMAGE_PREFIX = "images/"

    suspend fun export(context: Context, dao: VoucherDao, target: Uri): Int {
        val vouchers = dao.getAll()
        context.contentResolver.openOutputStream(target)?.use { out ->
            ZipOutputStream(out).use { zip ->
                val array = JSONArray()
                vouchers.forEach { array.put(toJson(it)) }
                zip.putNextEntry(ZipEntry(JSON_NAME))
                zip.write(JSONObject().put("version", 1).put("vouchers", array).toString(2).toByteArray())
                zip.closeEntry()
                vouchers.flatMap { listOfNotNull(it.imageFront, it.imageBack) }.distinct().forEach { name ->
                    val file = ImageStore.file(context, name)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry(IMAGE_PREFIX + name))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        } ?: error("Datei konnte nicht geschrieben werden")
        return vouchers.size
    }

    /** Imports a backup. Vouchers are added; existing ones are kept. Returns the number imported. */
    suspend fun import(context: Context, dao: VoucherDao, source: Uri): Int {
        var json: String? = null
        context.contentResolver.openInputStream(source)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == JSON_NAME -> json = zip.readBytes().toString(Charsets.UTF_8)
                        name.startsWith(IMAGE_PREFIX) && !name.contains("..") -> {
                            val fileName = name.removePrefix(IMAGE_PREFIX)
                            if (fileName.isNotBlank() && !fileName.contains('/')) {
                                ImageStore.file(context, fileName).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("Datei konnte nicht gelesen werden")

        val root = JSONObject(json ?: error("Keine gültige Gutschein-Sicherung"))
        val array = root.getJSONArray("vouchers")
        val existing = dao.getAll().map { it.title to it.createdAt }.toSet()
        var count = 0
        for (i in 0 until array.length()) {
            val v = fromJson(array.getJSONObject(i))
            if ((v.title to v.createdAt) in existing) continue
            dao.insert(v.copy(id = 0))
            count++
        }
        return count
    }

    private fun toJson(v: Voucher) = JSONObject().apply {
        put("title", v.title)
        put("provider", v.provider)
        put("category", v.category)
        putOpt("value", v.value)
        putOpt("remainingValue", v.remainingValue)
        put("code", v.code)
        put("pin", v.pin)
        putOpt("expiryDate", v.expiryDate)
        putOpt("receivedDate", v.receivedDate)
        put("giftedBy", v.giftedBy)
        put("location", v.location)
        put("website", v.website)
        put("conditions", v.conditions)
        put("notes", v.notes)
        putOpt("imageFront", v.imageFront)
        putOpt("imageBack", v.imageBack)
        put("isRedeemed", v.isRedeemed)
        putOpt("redeemedDate", v.redeemedDate)
        put("isFavorite", v.isFavorite)
        put("createdAt", v.createdAt)
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? = if (has(key) && !isNull(key)) getDouble(key) else null
    private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
    private fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null

    private fun fromJson(o: JSONObject) = Voucher(
        title = o.optString("title"),
        provider = o.optString("provider"),
        category = o.optString("category", Category.OTHER.name),
        value = o.optDoubleOrNull("value"),
        remainingValue = o.optDoubleOrNull("remainingValue"),
        code = o.optString("code"),
        pin = o.optString("pin"),
        expiryDate = o.optLongOrNull("expiryDate"),
        receivedDate = o.optLongOrNull("receivedDate"),
        giftedBy = o.optString("giftedBy"),
        location = o.optString("location"),
        website = o.optString("website"),
        conditions = o.optString("conditions"),
        notes = o.optString("notes"),
        imageFront = o.optStringOrNull("imageFront"),
        imageBack = o.optStringOrNull("imageBack"),
        isRedeemed = o.optBoolean("isRedeemed"),
        redeemedDate = o.optLongOrNull("redeemedDate"),
        isFavorite = o.optBoolean("isFavorite"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
    )
}
