package com.wearwash.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["systemKey"], unique = true),
        Index(value = ["customName"], unique = true),
    ],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemKey: String?,
    val customName: String?,
    val isPredefined: Boolean,
    val washingCriteriaType: String,
    val washingUsageThreshold: Int?,
    val washingDayThreshold: Int?,
    val createdAt: String,
    val updatedAt: String,
)
