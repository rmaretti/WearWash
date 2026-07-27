package com.wearwash.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "future_event_items",
    primaryKeys = ["eventId", "itemId"],
    foreignKeys = [
        ForeignKey(
            entity = FutureEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WashableItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("eventId"), Index("itemId")],
)
data class FutureEventItemEntity(
    val eventId: Long,
    val itemId: Long,
    val addedAt: String,
)
