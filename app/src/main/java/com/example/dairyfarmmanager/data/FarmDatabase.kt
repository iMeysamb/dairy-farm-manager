package com.example.dairyfarmmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class, FarmEntity::class, AnimalEntity::class, MilkRecordEntity::class,
        EmployeeEntity::class, FeedItemEntity::class, FeedTransactionEntity::class,
        PurchaseEntity::class, SaleEntity::class, ExpenseEntity::class, IncomeEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FarmDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao

    companion object {
        @Volatile
        private var instance: FarmDatabase? = null

        fun get(context: Context): FarmDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                FarmDatabase::class.java,
                "dairy_farm_manager.db"
            ).build().also { instance = it }
        }
    }
}
