package com.wearwash.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "future_events")
data class FutureEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val eventDate: String,
    val description: String?,
    val reminderDaysBefore: Int,
    val createdAt: String,
    val updatedAt: String,
)
