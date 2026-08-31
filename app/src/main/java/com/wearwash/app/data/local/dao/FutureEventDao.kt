package com.wearwash.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.wearwash.app.data.local.entity.FutureEventEntity
import com.wearwash.app.data.local.entity.FutureEventItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FutureEventDao {
    @Query("SELECT * FROM future_events ORDER BY eventDate, name")
    fun observeEvents(): Flow<List<FutureEventEntity>>

    @Query("SELECT * FROM future_event_items ORDER BY eventId, itemId")
    fun observeEventItems(): Flow<List<FutureEventItemEntity>>

    @Query("SELECT * FROM future_event_items WHERE eventId = :eventId")
    suspend fun getEventItems(eventId: Long): List<FutureEventItemEntity>

    @Query("SELECT * FROM future_events WHERE id = :eventId LIMIT 1")
    suspend fun getEvent(eventId: Long): FutureEventEntity?

    @Query(
        "UPDATE future_events SET lifecycleStatus = 'CONFIRMED', updatedAt = :updatedAt " +
            "WHERE id = :eventId AND lifecycleStatus = 'PENDING'",
    )
    suspend fun confirmPendingEvent(eventId: Long, updatedAt: String): Int

    @Query(
        "UPDATE future_events SET lifecycleStatus = 'COMPLETED', updatedAt = :updatedAt " +
            "WHERE eventDate < :today AND lifecycleStatus = 'CONFIRMED'",
    )
    suspend fun completeExpiredConfirmedEvents(today: String, updatedAt: String): Int

    @Query(
        "UPDATE future_events SET lifecycleStatus = 'NOT_CONFIRMED', updatedAt = :updatedAt " +
            "WHERE eventDate < :today AND lifecycleStatus = 'PENDING'",
    )
    suspend fun closeExpiredPendingEvents(today: String, updatedAt: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: FutureEventEntity): Long

    @Update
    suspend fun updateEvent(event: FutureEventEntity)

    @Delete
    suspend fun deleteEvent(event: FutureEventEntity)

    @Query("DELETE FROM future_events WHERE id = :eventId AND eventDate >= :today")
    suspend fun deleteOpenEvent(eventId: Long, today: String): Int

    @Query("DELETE FROM future_event_items WHERE eventId = :eventId")
    suspend fun deleteEventItems(eventId: Long)

    @Insert
    suspend fun insertEventItems(items: List<FutureEventItemEntity>)

    @Transaction
    suspend fun reconcileExpiredEvents(today: String, updatedAt: String) {
        completeExpiredConfirmedEvents(today, updatedAt)
        closeExpiredPendingEvents(today, updatedAt)
    }

    @Transaction
    suspend fun saveEventWithItems(
        event: FutureEventEntity,
        itemIds: Set<Long>,
        now: String,
    ): Long {
        val eventId = if (event.id == 0L) insertEvent(event) else {
            updateEvent(event)
            event.id
        }
        val existingItems = getEventItems(eventId).associateBy { it.itemId }
        deleteEventItems(eventId)
        if (itemIds.isNotEmpty()) {
            insertEventItems(
                itemIds.map { itemId ->
                    existingItems[itemId] ?: FutureEventItemEntity(
                        eventId = eventId,
                        itemId = itemId,
                        addedAt = now,
                    )
                },
            )
        }
        return eventId
    }
}
