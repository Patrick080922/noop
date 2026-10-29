package de.gutscheinsafe.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.gutscheinsafe.data.AppDatabase
import de.gutscheinsafe.data.Backup
import de.gutscheinsafe.data.ImageStore
import de.gutscheinsafe.data.Voucher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).voucherDao()

    val vouchers: StateFlow<List<Voucher>?> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun voucher(id: Long): Flow<Voucher?> = dao.observe(id)

    suspend fun load(id: Long): Voucher? = dao.get(id)

    /** Inserts or updates. Removes photos that were replaced. */
    fun save(voucher: Voucher, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = if (voucher.id == 0L) {
                dao.insert(voucher)
            } else {
                val old = dao.get(voucher.id)
                dao.update(voucher)
                if (old != null) {
                    if (old.imageFront != voucher.imageFront) deleteImage(old.imageFront)
                    if (old.imageBack != voucher.imageBack) deleteImage(old.imageBack)
                }
                voucher.id
            }
            onSaved(id)
        }
    }

    fun delete(voucher: Voucher) {
        viewModelScope.launch {
            dao.delete(voucher)
            deleteImage(voucher.imageFront)
            deleteImage(voucher.imageBack)
        }
    }

    fun setRedeemed(voucher: Voucher, redeemed: Boolean) {
        viewModelScope.launch {
            dao.update(
                voucher.copy(
                    isRedeemed = redeemed,
                    redeemedDate = if (redeemed) LocalDate.now().toEpochDay() else null,
                    remainingValue = if (redeemed && voucher.value != null) 0.0 else voucher.remainingValue,
                ),
            )
        }
    }

    /** Partial redemption: stores the new remaining value; at 0 the voucher counts as redeemed. */
    fun setRemaining(voucher: Voucher, remaining: Double) {
        viewModelScope.launch {
            val done = remaining <= 0.0
            dao.update(
                voucher.copy(
                    remainingValue = remaining.coerceAtLeast(0.0),
                    isRedeemed = done,
                    redeemedDate = if (done) LocalDate.now().toEpochDay() else null,
                ),
            )
        }
    }

    fun toggleFavorite(voucher: Voucher) {
        viewModelScope.launch { dao.update(voucher.copy(isFavorite = !voucher.isFavorite)) }
    }

    /** Copies a picked/taken photo into app storage. */
    suspend fun importImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        ImageStore.importFromUri(getApplication(), uri)
    }

    /** Deletes a photo that was picked in the editor but never saved. */
    fun discardImage(name: String?) {
        viewModelScope.launch { deleteImage(name) }
    }

    private suspend fun deleteImage(name: String?) = withContext(Dispatchers.IO) {
        ImageStore.delete(getApplication(), name)
    }

    suspend fun exportBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching { Backup.export(getApplication(), dao, uri) }
    }

    suspend fun importBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching { Backup.import(getApplication(), dao, uri) }
    }
}
