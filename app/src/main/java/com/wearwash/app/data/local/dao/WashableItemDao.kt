package com.wearwash.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.wearwash.app.data.local.entity.LaundryBasketEntryEntity
import com.wearwash.app.data.local.entity.UsageEventEntity
import com.wearwash.app.data.local.entity.WashEventEntity
import com.wearwash.app.data.local.entity.WashableItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WashableItemDao {
    @Query("SELECT * FROM items WHERE archivedAt IS NULL ORDER BY name")
    fun observeActiveItems(): Flow<List<WashableItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WashableItemEntity?

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<WashableItemEntity?>

    @Query(
        """
        SELECT * FROM items
        WHERE archivedAt IS NULL
        AND (
            name LIKE '%' || :query || '%'
            OR categoryName LIKE '%' || :query || '%'
        )
        ORDER BY name
        """,
    )
    fun searchByName(query: String): Flow<List<WashableItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WashableItemEntity): Long

    @Update
    suspend fun update(item: WashableItemEntity)

    @Query("UPDATE items SET archivedAt = :archivedAt, status = 'Archived', updatedAt = :archivedAt WHERE id = :itemId")
    suspend fun archiveRow(itemId: Long, archivedAt: String)

    @Insert
    suspend fun insertUsageEvent(event: UsageEventEntity): Long

    @Query("SELECT * FROM usage_events WHERE itemId = :itemId ORDER BY usedAt DESC, createdAt DESC")
    fun observeUsageEvents(itemId: Long): Flow<List<UsageEventEntity>>

    @Query("SELECT * FROM usage_events WHERE id = :eventId LIMIT 1")
    suspend fun getUsageEvent(eventId: Long): UsageEventEntity?

    @Query("DELETE FROM usage_events WHERE id = :eventId")
    suspend fun deleteUsageEventById(eventId: Long)

    @Insert
    suspend fun insertWashEvent(event: WashEventEntity): Long

    @Query("SELECT * FROM wash_events WHERE itemId = :itemId ORDER BY washedAt DESC, createdAt DESC")
    fun observeWashEvents(itemId: Long): Flow<List<WashEventEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBasketEntry(entry: LaundryBasketEntryEntity): Long

    @Query("SELECT * FROM laundry_basket_entries WHERE itemId = :itemId LIMIT 1")
    suspend fun getBasketEntry(itemId: Long): LaundryBasketEntryEntity?

    @Query("SELECT itemId FROM laundry_basket_entries")
    fun observeBasketItemIds(): Flow<List<Long>>

    @Query(
        """
        SELECT items.* FROM items
        INNER JOIN laundry_basket_entries ON laundry_basket_entries.itemId = items.id
        WHERE items.archivedAt IS NULL
        ORDER BY laundry_basket_entries.addedAt, items.name
        """,
    )
    fun observeBasketItems(): Flow<List<WashableItemEntity>>

    @Query("DELETE FROM laundry_basket_entries WHERE itemId = :itemId")
    suspend fun removeBasketEntry(itemId: Long)

    @Query("SELECT COUNT(*) FROM usage_events WHERE itemId = :itemId AND usedAt > :washedAt")
    suspend fun countUsageAfter(itemId: Long, washedAt: String): Int

    @Transaction
    suspend fun recordUsage(
        itemId: Long,
        usedAt: String,
        notes: String?,
        createdAt: String,
    ): Long? {
        val item = getById(itemId) ?: return null
        val countsTowardCurrentCycle =
            item.lastWashingDate == null || usedAt >= item.lastWashingDate
        update(
            item.copy(
                usesSinceWash = item.usesSinceWash + if (countsTowardCurrentCycle) 1 else 0,
                lifetimeUses = item.lifetimeUses + 1,
                status = if (countsTowardCurrentCycle) "Worn" else item.status,
                updatedAt = createdAt,
            ),
        )
        return insertUsageEvent(
            UsageEventEntity(
                itemId = itemId,
                usedAt = usedAt,
                notes = notes,
                createdAt = createdAt,
            ),
        )
    }

    @Transaction
    suspend fun addToBasket(entry: LaundryBasketEntryEntity) {
        insertBasketEntry(entry)
    }

    @Transaction
    suspend fun recordWashes(
        itemIds: List<Long>,
        washedAt: String,
        comment: String?,
        createdAt: String,
        outOfCycleItemIds: Set<Long>,
    ): Boolean {
        val currentItems = itemIds.map { itemId -> getById(itemId) ?: return false }
        val hasChronologyConflict = currentItems.any { item ->
            (item.lastWashingDate != null && washedAt < item.lastWashingDate) ||
                countUsageAfter(item.id, washedAt) > 0
        }
        if (hasChronologyConflict) return false

        currentItems.forEach { item ->
            update(
                item.copy(
                    usesSinceWash = 0,
                    washingCount = item.washingCount + 1,
                    lastWashingDate = washedAt,
                    status = "Clean",
                    updatedAt = createdAt,
                ),
            )
            insertWashEvent(
                WashEventEntity(
                    itemId = item.id,
                    washedAt = washedAt,
                    usesAtTimeOfWash = item.usesSinceWash,
                    comment = comment,
                    wasOutOfCycle = item.id in outOfCycleItemIds,
                    createdAt = createdAt,
                ),
            )
            removeBasketEntry(item.id)
        }
        return true
    }

    @Transaction
    suspend fun archive(itemId: Long, archivedAt: String) {
        archiveRow(itemId, archivedAt)
        removeBasketEntry(itemId)
    }

    @Transaction
    suspend fun deleteUsageEventAndUpdateItem(
        eventId: Long,
        updatedAt: String,
    ) {
        val event = getUsageEvent(eventId) ?: return
        val item = getById(event.itemId) ?: return
        val occurredAfterLatestWash = item.lastWashingDate == null || event.usedAt >= item.lastWashingDate
        update(
            item.copy(
                usesSinceWash = if (occurredAfterLatestWash) {
                    (item.usesSinceWash - 1).coerceAtLeast(0)
                } else {
                    item.usesSinceWash
                },
                lifetimeUses = (item.lifetimeUses - 1).coerceAtLeast(0),
                status = if (occurredAfterLatestWash && item.usesSinceWash <= 1) "Clean" else item.status,
                updatedAt = updatedAt,
            ),
        )
        deleteUsageEventById(eventId)
    }
}
