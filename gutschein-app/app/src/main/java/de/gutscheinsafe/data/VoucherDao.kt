package de.gutscheinsafe.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate, createdAt DESC")
    fun observeAll(): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE id = :id")
    fun observe(id: Long): Flow<Voucher?>

    @Query("SELECT * FROM vouchers WHERE id = :id")
    suspend fun get(id: Long): Voucher?

    @Query("SELECT * FROM vouchers")
    suspend fun getAll(): List<Voucher>

    @Insert
    suspend fun insert(voucher: Voucher): Long

    @Update
    suspend fun update(voucher: Voucher)

    @Delete
    suspend fun delete(voucher: Voucher)
}
