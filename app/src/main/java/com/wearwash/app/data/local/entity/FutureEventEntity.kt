package com.wearwash.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FutureEventStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    NOT_CONFIRMED;

    companion object {
        fun fromStorage(value: String): FutureEventStatus =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}

@Entity(tableName = "future_events")
data class FutureEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val eventDate: String,
    val description: String?,
    val reminderDaysBefore: Int,
    val lifecycleStatus: String = FutureEventStatus.PENDING.name,
    val createdAt: String,
    val updatedAt: String,
)
