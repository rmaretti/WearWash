package com.wearwash.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "future_event_items",
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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val itemId: Long,
    val status: String,
    val addedAt: String,
    val preparedAt: String?,
    val preparationComment: String?,
    val preparationWashed: Boolean,
)
