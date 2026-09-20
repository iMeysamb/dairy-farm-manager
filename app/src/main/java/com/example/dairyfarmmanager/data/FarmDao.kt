package com.example.dairyfarmmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    @Query("SELECT * FROM animals WHERE status != 'DELETED' ORDER BY tagNumber")
    fun observeAnimals(): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animals WHERE id = :id LIMIT 1")
    suspend fun findAnimal(id: Long): AnimalEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAnimal(animal: AnimalEntity): Long

    @Update
    suspend fun updateAnimal(animal: AnimalEntity)

    @Query("UPDATE animals SET status = 'DELETED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteAnimal(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM employees WHERE status != 'DELETED' ORDER BY name")
    fun observeEmployees(): Flow<List<EmployeeEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Query("UPDATE employees SET status = 'DELETED' WHERE id = :id")
    suspend fun softDeleteEmployee(id: Long)

    @Query("SELECT * FROM milk_records ORDER BY date DESC, id DESC")
    fun observeMilkRecords(): Flow<List<MilkRecordEntity>>

    @Insert
    suspend fun insertMilkRecord(record: MilkRecordEntity): Long

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM milk_records WHERE date = :date")
    suspend fun totalMilkForDate(date: String): Double

    @Query("SELECT * FROM feed_items ORDER BY name")
    fun observeFeedItems(): Flow<List<FeedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFeedItem(item: FeedItemEntity): Long

    @Update
    suspend fun updateFeedItem(item: FeedItemEntity)

    @Query("SELECT * FROM feed_transactions ORDER BY date DESC, id DESC")
    fun observeFeedTransactions(): Flow<List<FeedTransactionEntity>>

    @Insert
    suspend fun insertFeedTransaction(transaction: FeedTransactionEntity): Long

    @Query("SELECT * FROM sales ORDER BY date DESC, id DESC")
    fun observeSales(): Flow<List<SaleEntity>>

    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSale(id: Long)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM incomes")
    suspend fun totalIncome(): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses")
    suspend fun totalExpense(): Long
}
