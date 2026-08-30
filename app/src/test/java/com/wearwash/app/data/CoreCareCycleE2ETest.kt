package com.wearwash.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wearwash.app.data.local.WearWashDatabase
import com.wearwash.app.data.local.entity.LaundryBasketEntryEntity
import com.wearwash.app.data.local.entity.CategoryEntity
import com.wearwash.app.data.local.entity.FutureEventEntity
import com.wearwash.app.data.local.entity.WashableItemEntity
import com.wearwash.app.domain.model.WashableItemStatus
import com.wearwash.app.domain.model.WashingCriteriaType
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoreCareCycleE2ETest {
    private lateinit var database: WearWashDatabase
    private lateinit var repository: ItemRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WearWashDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomItemRepository(
            database.washableItemDao(),
            database.categoryDao(),
            database.futureEventDao(),
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `register use basket and wash completes the core care cycle`() = runTest {
        val createdAt = "2026-07-23T08:00:00-03:00"
        val itemId = repository.saveItem(testItem(createdAt))

        val initialItem = repository.getItem(itemId)!!
        assertEquals(WashableItemStatus.Clean.name, initialItem.status)
        assertEquals(0, initialItem.usesSinceWash)

        repeat(2) { index ->
            repository.recordUsage(
                itemId = itemId,
                usedAt = "2026-07-${21 + index}",
                notes = if (index == 1) "Warm day" else null,
                createdAt = "2026-07-23T0${9 + index}:00:00-03:00",
            )
        }

        assertEquals(2, repository.observeUsageEvents(itemId).first().size)
        assertEquals(2, repository.getItem(itemId)!!.usesSinceWash)

        val basketEntry = LaundryBasketEntryEntity(
            itemId = itemId,
            addedAt = "2026-07-23T12:00:00-03:00",
            reason = "automatic_readiness",
            comment = null,
        )
        repository.addToBasket(basketEntry)
        repository.addToBasket(basketEntry)

        assertEquals(listOf(itemId), repository.observeBasketItemIds().first())
        assertEquals(1, repository.observeBasketItems().first().size)

        val washedAt = "2026-07-23T13:00:00-03:00"
        val washSaved = repository.recordWashes(
            itemIds = listOf(itemId),
            washedAt = "2026-07-23",
            comment = null,
            createdAt = washedAt,
            outOfCycleItemIds = emptySet(),
        )
        assertTrue(washSaved)

        val washedItem = repository.getItem(itemId)
        assertNotNull(washedItem)
        assertEquals(0, washedItem!!.usesSinceWash)
        assertEquals(2, washedItem.lifetimeUses)
        assertEquals(1, washedItem.washingCount)
        assertEquals("2026-07-23", washedItem.lastWashingDate)
        assertEquals(WashableItemStatus.Clean.name, washedItem.status)
        assertTrue(repository.observeBasketItems().first().isEmpty())
        assertEquals(1, repository.observeWashEvents(itemId).first().size)
    }

    @Test
    fun `deleting a usage corrects counters without removing history before latest wash`() = runTest {
        val itemId = repository.saveItem(
            testItem("2026-07-23T08:00:00-03:00").copy(
                usesSinceWash = 1,
                lifetimeUses = 4,
                lastWashingDate = "2026-07-20",
                status = WashableItemStatus.Worn.name,
            ),
        )
        repository.recordUsage(
            itemId = itemId,
            usedAt = "2026-07-10",
            notes = null,
            createdAt = "2026-07-10T10:00:00-03:00",
        )
        val oldEvent = repository.observeUsageEvents(itemId).first().single()
        repository.deleteUsageEvent(oldEvent.id, "2026-07-23T12:00:00-03:00")

        val corrected = repository.getItem(itemId)!!
        assertEquals(1, corrected.usesSinceWash)
        assertEquals(4, corrected.lifetimeUses)
        assertFalse(repository.observeUsageEvents(itemId).first().isNotEmpty())
    }

    @Test
    fun `backdated wash is rejected without changing item basket or history`() = runTest {
        val itemId = repository.saveItem(
            testItem("2026-07-20T08:00:00-03:00").copy(washingUsageThreshold = 1),
        )
        repository.recordUsage(
            itemId = itemId,
            usedAt = "2026-07-23",
            notes = null,
            createdAt = "2026-07-23T10:00:00-03:00",
        )
        repository.addToBasket(
            LaundryBasketEntryEntity(
                itemId = itemId,
                addedAt = "2026-07-23T11:00:00-03:00",
                reason = "manual",
                comment = null,
            ),
        )

        val saved = repository.recordWashes(
            itemIds = listOf(itemId),
            washedAt = "2026-07-22",
            comment = null,
            createdAt = "2026-07-23T12:00:00-03:00",
            outOfCycleItemIds = emptySet(),
        )

        assertFalse(saved)
        assertEquals(1, repository.getItem(itemId)!!.usesSinceWash)
        assertEquals(listOf(itemId), repository.observeBasketItemIds().first())
        assertTrue(repository.observeWashEvents(itemId).first().isEmpty())
    }

    @Test
    fun `bulk wash updates all selected items and clears their basket entries`() = runTest {
        val firstId = repository.saveItem(testItem("2026-07-20T08:00:00-03:00"))
        val secondId = repository.saveItem(
            testItem("2026-07-20T08:05:00-03:00").copy(name = "Towel"),
        )
        listOf(firstId, secondId).forEach { itemId ->
            repository.recordUsage(
                itemId,
                "2026-07-22",
                null,
                "2026-07-22T10:00:00-03:00",
            )
            repository.addToBasket(
                LaundryBasketEntryEntity(
                    itemId = itemId,
                    addedAt = "2026-07-22T11:00:00-03:00",
                    reason = "manual",
                    comment = null,
                ),
            )
        }

        val saved = repository.recordWashes(
            itemIds = listOf(firstId, secondId),
            washedAt = "2026-07-23",
            comment = "Weekly load",
            createdAt = "2026-07-23T10:00:00-03:00",
            outOfCycleItemIds = setOf(secondId),
        )

        assertTrue(saved)
        assertTrue(repository.observeBasketItems().first().isEmpty())
        assertEquals(0, repository.getItem(firstId)!!.usesSinceWash)
        assertEquals(0, repository.getItem(secondId)!!.usesSinceWash)
        assertEquals(1, repository.observeWashEvents(firstId).first().size)
        assertTrue(repository.observeWashEvents(secondId).first().single().wasOutOfCycle)
    }

    @Test
    fun `archiving removes basket membership and duplicate adds stay unique`() = runTest {
        val itemId = repository.saveItem(
            testItem("2026-07-20T08:00:00-03:00").copy(
                usesSinceWash = 2,
                lifetimeUses = 2,
                status = WashableItemStatus.Worn.name,
            ),
        )
        val entry = LaundryBasketEntryEntity(
            itemId = itemId,
            addedAt = "2026-07-22T11:00:00-03:00",
            reason = "manual",
            comment = null,
        )
        repository.addToBasket(entry)
        repository.addToBasket(entry)
        assertEquals(listOf(itemId), repository.observeBasketItemIds().first())

        repository.archiveItem(itemId, "2026-07-23T10:00:00-03:00")

        assertTrue(repository.observeBasketItemIds().first().isEmpty())
        assertNotNull(repository.getItem(itemId)!!.archivedAt)
    }

    @Test
    fun `custom category supports create update search and safe delete`() = runTest {
        val now = "2026-07-25T10:00:00-03:00"
        assertTrue(
            repository.saveCategory(
                CategoryEntity(
                    customName = "Delicates",
                    systemKey = null,
                    isPredefined = false,
                    washingCriteriaType = WashingCriteriaType.ByUsage.name,
                    washingUsageThreshold = 2,
                    washingDayThreshold = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
        )
        val category = repository.observeCategories().first().single()
        assertEquals("Delicates", category.customName)
        assertFalse(
            repository.saveCategory(
                category.copy(id = 0, customName = "delicates"),
            ),
        )

        assertTrue(
            repository.saveCategory(
                category.copy(
                    washingCriteriaType = WashingCriteriaType.ByUsageOrDate.name,
                    washingDayThreshold = 7,
                ),
            ),
        )
        val updated = repository.observeCategories().first().single()
        assertEquals(7, updated.washingDayThreshold)

        repository.saveItem(testItem(now).copy(categoryId = updated.id, categoryName = "Delicates"))
        assertFalse(repository.deleteCategory(updated.id))
    }

    @Test
    fun `future event keeps item assignments when edited and cascades when deleted`() = runTest {
        val now = "2026-07-25T10:00:00-03:00"
        val shirtId = repository.saveItem(testItem(now))
        val towelId = repository.saveItem(testItem(now).copy(name = "Towel"))
        val eventId = repository.saveFutureEvent(
            FutureEventEntity(
                name = "Beach weekend",
                eventDate = "2026-07-27",
                description = "Pack light",
                reminderDaysBefore = 2,
                createdAt = now,
                updatedAt = now,
            ),
            setOf(shirtId, towelId),
        )

        assertEquals(1, repository.observeFutureEvents().first().size)
        assertEquals(2, repository.observeFutureEventItems().first().size)

        val savedEvent = repository.observeFutureEvents().first().single()
        repository.saveFutureEvent(
            savedEvent.copy(name = "Long beach weekend"),
            setOf(shirtId),
        )

        assertEquals("Long beach weekend", repository.observeFutureEvents().first().single().name)
        val retained = repository.observeFutureEventItems().first().single()
        assertEquals(shirtId, retained.itemId)

        repository.deleteFutureEvent(eventId)
        assertTrue(repository.observeFutureEvents().first().isEmpty())
        assertTrue(repository.observeFutureEventItems().first().isEmpty())
    }

    private fun testItem(createdAt: String) = WashableItemEntity(
        name = "Gym shirt",
        categoryId = null,
        categoryName = "Clothing",
        colorId = null,
        colorName = null,
        brand = null,
        photoUri = null,
        fabricId = null,
        fabricName = null,
        seasonId = null,
        seasonName = null,
        purchaseDate = null,
        purchasePriceCents = null,
        description = null,
        usesSinceWash = 0,
        lifetimeUses = 0,
        washingCount = 0,
        lastWashingDate = null,
        washingCriteriaType = WashingCriteriaType.ByUsage.name,
        washingUsageThreshold = 2,
        washingDayThreshold = null,
        status = WashableItemStatus.Clean.name,
        createdAt = createdAt,
        updatedAt = createdAt,
        archivedAt = null,
    )
}
