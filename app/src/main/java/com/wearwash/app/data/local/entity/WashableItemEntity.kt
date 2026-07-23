package com.wearwash.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class WashableItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long?,
    val categoryName: String?,
    val colorId: Long?,
    val colorName: String?,
    val brand: String?,
    val photoUri: String?,
    val fabricId: Long?,
    val fabricName: String?,
    val seasonId: Long?,
    val seasonName: String?,
    val purchaseDate: String?,
    val purchasePriceCents: Long?,
    val description: String?,
    val usesSinceWash: Int,
    val lifetimeUses: Int,
    val washingCount: Int,
    val lastWashingDate: String?,
    val washingCriteriaType: String,
    val washingUsageThreshold: Int?,
    val washingDayThreshold: Int?,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val archivedAt: String?,
)
