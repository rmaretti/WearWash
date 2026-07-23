package com.wearwash.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "laundry_basket_entries",
    foreignKeys = [
        ForeignKey(
            entity = WashableItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["itemId"], unique = true)],
)
data class LaundryBasketEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val addedAt: String,
    val reason: String?,
    val comment: String?,
)
