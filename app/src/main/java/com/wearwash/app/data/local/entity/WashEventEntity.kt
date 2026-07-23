package com.wearwash.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wash_events",
    foreignKeys = [
        ForeignKey(
            entity = WashableItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId")],
)
data class WashEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val washedAt: String,
    val usesAtTimeOfWash: Int,
    val comment: String?,
    val wasOutOfCycle: Boolean,
    val createdAt: String,
)
