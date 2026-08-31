package com.wearwash.app.data

import com.wearwash.app.data.local.dao.WashableItemDao
import com.wearwash.app.data.local.entity.LaundryBasketEntryEntity
import com.wearwash.app.data.local.entity.UsageEventEntity
import com.wearwash.app.data.local.entity.WashEventEntity
import com.wearwash.app.data.local.entity.WashableItemEntity
import com.wearwash.app.data.local.entity.CategoryEntity
import com.wearwash.app.data.local.entity.FutureEventEntity
import com.wearwash.app.data.local.entity.FutureEventItemEntity
import com.wearwash.app.data.local.entity.FutureEventStatus
import com.wearwash.app.domain.logic.WashingRule
import com.wearwash.app.domain.logic.evaluateWashingReadiness
import com.wearwash.app.domain.model.WashingCriteriaType
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    fun observeCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
    fun observeFutureEvents(): Flow<List<FutureEventEntity>> = flowOf(emptyList())
    fun observeFutureEventItems(): Flow<List<FutureEventItemEntity>> = flowOf(emptyList())
    suspend fun saveCategory(category: CategoryEntity): Boolean = false
    suspend fun deleteCategory(categoryId: Long): Boolean = false
    suspend fun saveFutureEvent(event: FutureEventEntity, itemIds: Set<Long>): Long = 0
    suspend fun deleteFutureEvent(eventId: Long) = Unit
    suspend fun confirmFutureEvent(eventId: Long, today: LocalDate, updatedAt: String): Boolean = false
    suspend fun reconcileFutureEventStatuses(today: LocalDate, updatedAt: String) = Unit
    fun observeActiveItems(): Flow<List<WashableItemEntity>>
    fun searchItems(query: String): Flow<List<WashableItemEntity>>
    fun observeItem(id: Long): Flow<WashableItemEntity?>
    fun observeUsageEvents(itemId: Long): Flow<List<UsageEventEntity>>
    fun observeWashEvents(itemId: Long): Flow<List<WashEventEntity>>
    fun observeBasketItemIds(): Flow<List<Long>>
    fun observeBasketItems(): Flow<List<WashableItemEntity>>
    suspend fun getItem(id: Long): WashableItemEntity?
    suspend fun saveItem(item: WashableItemEntity): Long
    suspend fun updateItem(item: WashableItemEntity)
    suspend fun recordUsage(itemId: Long, usedAt: String, notes: String?, createdAt: String)
    suspend fun deleteUsageEvent(eventId: Long, updatedAt: String)
    suspend fun addToBasket(entry: LaundryBasketEntryEntity)
    suspend fun removeFromBasket(itemId: Long)
    suspend fun recordWashes(
        itemIds: List<Long>,
        washedAt: String,
        comment: String?,
        createdAt: String,
        outOfCycleItemIds: Set<Long>,
    ): Boolean
    suspend fun archiveItem(itemId: Long, archivedAt: String)
}

class RoomItemRepository(
    private val washableItemDao: WashableItemDao,
    private val categoryDao: com.wearwash.app.data.local.dao.CategoryDao,
    private val futureEventDao: com.wearwash.app.data.local.dao.FutureEventDao,
) : ItemRepository {
    override fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    override fun observeFutureEvents(): Flow<List<FutureEventEntity>> =
        futureEventDao.observeEvents()

    override fun observeFutureEventItems(): Flow<List<FutureEventItemEntity>> =
        futureEventDao.observeEventItems()

    override suspend fun saveCategory(category: CategoryEntity): Boolean {
        val name = category.customName?.trim()
        if (
            !category.isPredefined &&
            (name.isNullOrBlank() || categoryDao.countCustomName(name, category.id) > 0)
        ) return false
        return runCatching {
            if (category.id == 0L) categoryDao.insert(category.copy(customName = name))
            else {
                val existing = categoryDao.getById(category.id) ?: return false
                categoryDao.update(
                    category.copy(
                        systemKey = existing.systemKey,
                        customName = if (existing.isPredefined) null else name,
                        isPredefined = existing.isPredefined,
                        createdAt = existing.createdAt,
                    ),
                )
            }
        }.isSuccess
    }

    override suspend fun deleteCategory(categoryId: Long): Boolean {
        val category = categoryDao.getById(categoryId) ?: return false
        if (category.isPredefined || categoryDao.itemCount(categoryId) > 0) return false
        categoryDao.deleteCustom(categoryId)
        return true
    }

    override suspend fun saveFutureEvent(
        event: FutureEventEntity,
        itemIds: Set<Long>,
    ): Long {
        val now = java.time.OffsetDateTime.now().toString()
        val existing = if (event.id == 0L) null else futureEventDao.getEvent(event.id)
        return futureEventDao.saveEventWithItems(
            event.copy(
                lifecycleStatus = existing?.lifecycleStatus ?: FutureEventStatus.PENDING.name,
                createdAt = existing?.createdAt ?: event.createdAt,
                updatedAt = now,
            ),
            itemIds,
            now,
        )
    }

    override suspend fun deleteFutureEvent(eventId: Long) {
        futureEventDao.getEvent(eventId)?.let { futureEventDao.deleteEvent(it) }
    }

    override suspend fun confirmFutureEvent(
        eventId: Long,
        today: LocalDate,
        updatedAt: String,
    ): Boolean {
        val event = futureEventDao.getEvent(eventId) ?: return false
        val eventDate = runCatching { LocalDate.parse(event.eventDate) }.getOrNull() ?: return false
        val daysUntilEvent = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate)
        if (daysUntilEvent !in 0L..3L) return false
        return futureEventDao.confirmPendingEvent(eventId, updatedAt) == 1
    }

    override suspend fun reconcileFutureEventStatuses(today: LocalDate, updatedAt: String) {
        futureEventDao.reconcileExpiredEvents(today.toString(), updatedAt)
    }

    override fun observeActiveItems(): Flow<List<WashableItemEntity>> =
        washableItemDao.observeActiveItems()

    override fun searchItems(query: String): Flow<List<WashableItemEntity>> =
        if (query.isBlank()) {
            washableItemDao.observeActiveItems()
        } else {
            washableItemDao.searchByName(query.trim())
        }

    override fun observeItem(id: Long): Flow<WashableItemEntity?> =
        washableItemDao.observeById(id)

    override fun observeUsageEvents(itemId: Long): Flow<List<UsageEventEntity>> =
        washableItemDao.observeUsageEvents(itemId)

    override fun observeWashEvents(itemId: Long): Flow<List<WashEventEntity>> =
        washableItemDao.observeWashEvents(itemId)

    override fun observeBasketItemIds(): Flow<List<Long>> =
        washableItemDao.observeBasketItemIds()

    override fun observeBasketItems(): Flow<List<WashableItemEntity>> =
        washableItemDao.observeBasketItems()

    override suspend fun getItem(id: Long): WashableItemEntity? =
        washableItemDao.getById(id)

    override suspend fun saveItem(item: WashableItemEntity): Long =
        washableItemDao.upsert(item)

    override suspend fun updateItem(item: WashableItemEntity) {
        washableItemDao.update(item)
    }

    override suspend fun recordUsage(
        itemId: Long,
        usedAt: String,
        notes: String?,
        createdAt: String,
    ) {
        washableItemDao.recordUsage(itemId, usedAt, notes, createdAt)
    }

    override suspend fun deleteUsageEvent(eventId: Long, updatedAt: String) {
        washableItemDao.deleteUsageEventAndUpdateItem(eventId, updatedAt)
    }

    override suspend fun addToBasket(entry: LaundryBasketEntryEntity) {
        val item = washableItemDao.getById(entry.itemId) ?: return
        val ruleType = runCatching {
            WashingCriteriaType.valueOf(item.washingCriteriaType)
        }.getOrDefault(WashingCriteriaType.Manual)
        val lastWashingDate = item.lastWashingDate?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
        val readiness = evaluateWashingReadiness(
            rule = WashingRule(
                type = ruleType,
                usageThreshold = item.washingUsageThreshold,
                dayThreshold = item.washingDayThreshold,
            ),
            usesSinceWash = item.usesSinceWash,
            lastWashingDate = lastWashingDate,
        )
        if (readiness.needsWashing) washableItemDao.addToBasket(entry)
    }

    override suspend fun removeFromBasket(itemId: Long) {
        washableItemDao.removeBasketEntry(itemId)
    }

    override suspend fun recordWashes(
        itemIds: List<Long>,
        washedAt: String,
        comment: String?,
        createdAt: String,
        outOfCycleItemIds: Set<Long>,
    ): Boolean = washableItemDao.recordWashes(
        itemIds,
        washedAt,
        comment,
        createdAt,
        outOfCycleItemIds,
    )

    override suspend fun archiveItem(itemId: Long, archivedAt: String) {
        washableItemDao.archive(itemId, archivedAt)
    }
}
