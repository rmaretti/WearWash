package com.wearwash.app.data

import com.wearwash.app.data.local.dao.WashableItemDao
import com.wearwash.app.data.local.entity.LaundryBasketEntryEntity
import com.wearwash.app.data.local.entity.UsageEventEntity
import com.wearwash.app.data.local.entity.WashEventEntity
import com.wearwash.app.data.local.entity.WashableItemEntity
import com.wearwash.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    fun observeCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
    suspend fun saveCategory(category: CategoryEntity): Boolean = false
    suspend fun deleteCategory(categoryId: Long): Boolean = false
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
) : ItemRepository {
    override fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

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
        washableItemDao.addToBasket(entry)
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
