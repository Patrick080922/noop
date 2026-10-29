package de.gutscheinsafe.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Voucher photos are copied into private app storage so they survive the original being deleted. */
object ImageStore {
    fun dir(context: Context): File = File(context.filesDir, "images").apply { mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    /** Copies the image behind [uri] into app storage and returns the new file name. */
    fun importFromUri(context: Context, uri: Uri): String? = runCatching {
        val name = "${UUID.randomUUID()}.jpg"
        context.contentResolver.openInputStream(uri)?.use { input ->
            file(context, name).outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        name
    }.getOrNull()

    fun delete(context: Context, name: String?) {
        if (name != null) file(context, name).delete()
    }

    /** A fresh temporary file + content Uri for the camera app to write into. */
    fun newCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareUri(context: Context, name: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file(context, name))
}
