package com.example.dairyfarmmanager.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val role: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "farms")
data class FarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val logoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "animals", indices = [Index(value = ["farmId"]), Index(value = ["tagNumber"], unique = true), Index(value = ["earTag"], unique = true)])
data class AnimalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmId: Long,
    val tagNumber: String,
    val earTag: String,
    val name: String,
    val gender: String,
    val breed: String,
    val birthDate: String,
    val entryDate: String,
    val weight: Double,
    val color: String,
    val status: String,
    val motherId: Long? = null,
    val fatherId: Long? = null,
    val purchasePrice: Long = 0,
    val notes: String = "",
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "milk_records", indices = [Index(value = ["animalId", "date"])], foreignKeys = [ForeignKey(entity = AnimalEntity::class, parentColumns = ["id"], childColumns = ["animalId"], onDelete = ForeignKey.CASCADE)])
data class MilkRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val animalId: Long,
    val date: String,
    val morningAmount: Double,
    val noonAmount: Double,
    val eveningAmount: Double,
    val totalAmount: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "employees", indices = [Index(value = ["name"])])
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val position: String,
    val salary: Long,
    val startDate: String,
    val status: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "feed_items", indices = [Index(value = ["name"], unique = true)])
data class FeedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,
    val currentStock: Double,
    val minimumStock: Double,
    val purchasePrice: Long,
    val supplier: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "feed_transactions", indices = [Index(value = ["feedItemId", "date"])], foreignKeys = [ForeignKey(entity = FeedItemEntity::class, parentColumns = ["id"], childColumns = ["feedItemId"], onDelete = ForeignKey.CASCADE)])
data class FeedTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val feedItemId: Long,
    val type: String,
    val quantity: Double,
    val date: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val category: String,
    val supplier: String,
    val amount: Long,
    val description: String,
    val invoiceNumber: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val category: String,
    val customer: String,
    val amount: Long,
    val description: String,
    val invoiceNumber: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val category: String,
    val amount: Long,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val category: String,
    val amount: Long,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings", indices = [Index(value = ["key"], unique = true)])
data class SettingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String
)
