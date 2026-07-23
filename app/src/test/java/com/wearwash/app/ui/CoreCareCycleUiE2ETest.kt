package com.wearwash.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.wearwash.app.data.ItemRepository
import com.wearwash.app.data.local.entity.LaundryBasketEntryEntity
import com.wearwash.app.data.local.entity.UsageEventEntity
import com.wearwash.app.data.local.entity.WashEventEntity
import com.wearwash.app.data.local.entity.WashableItemEntity
import com.wearwash.app.ui.screens.items.ItemsScreen
import com.wearwash.app.ui.screens.items.ItemsViewModel
import com.wearwash.app.ui.theme.WearWashTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoreCareCycleUiE2ETest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `user completes register use basket and wash journey`() {
        val repository = UiTestItemRepository()
        val viewModel = ItemsViewModel(repository)
        composeRule.setContent {
            WearWashTheme {
                ItemsScreen(itemRepository = repository, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("add-item").performClick()
        composeRule.onNodeWithTag("item-name").performTextInput("Gym shirt")
        composeRule.onNodeWithTag("save-item").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("item-1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Gym shirt").assertIsDisplayed()

        repeat(3) {
            composeRule.onNodeWithText("Used today").performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithText("Needs washing").assertIsDisplayed()

        composeRule.onNodeWithTag("basket-tab").performClick()
        composeRule.onNodeWithText("Add to basket").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("basket-select-1").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("basket-select-1").performClick()
        composeRule.onNodeWithTag("wash-selected").performClick()
        composeRule.onNodeWithTag("confirm-wash").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("The basket is empty.").assertIsDisplayed()
                composeRule.onNodeWithText("No items currently need washing.").assertIsDisplayed()
            }.isSuccess
        }
    }
}

private class UiTestItemRepository : ItemRepository {
    private val items = MutableStateFlow<List<WashableItemEntity>>(emptyList())
    private val basketIds = MutableStateFlow<List<Long>>(emptyList())
    private val usageEvents = MutableStateFlow<List<UsageEventEntity>>(emptyList())
    private val washEvents = MutableStateFlow<List<WashEventEntity>>(emptyList())

    override fun observeActiveItems(): Flow<List<WashableItemEntity>> = items

    override fun searchItems(query: String): Flow<List<WashableItemEntity>> =
        items.map { all ->
            if (query.isBlank()) all
            else all.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.categoryName.orEmpty().contains(query, ignoreCase = true)
            }
        }

    override fun observeItem(id: Long): Flow<WashableItemEntity?> =
        items.map { all -> all.firstOrNull { it.id == id } }

    override fun observeUsageEvents(itemId: Long): Flow<List<UsageEventEntity>> =
        usageEvents.map { events -> events.filter { it.itemId == itemId } }

    override fun observeWashEvents(itemId: Long): Flow<List<WashEventEntity>> =
        washEvents.map { events -> events.filter { it.itemId == itemId } }

    override fun observeBasketItemIds(): Flow<List<Long>> = basketIds

    override fun observeBasketItems(): Flow<List<WashableItemEntity>> =
        combine(items, basketIds) { all, ids -> all.filter { it.id in ids } }

    override suspend fun getItem(id: Long): WashableItemEntity? =
        items.value.firstOrNull { it.id == id }

    override suspend fun saveItem(item: WashableItemEntity): Long {
        val id = item.id.takeIf { it != 0L } ?: ((items.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        val saved = item.copy(id = id)
        items.value = items.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun updateItem(item: WashableItemEntity) {
        items.value = items.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun recordUsage(
        itemId: Long,
        usedAt: String,
        notes: String?,
        createdAt: String,
    ) {
        val item = getItem(itemId) ?: return
        updateItem(
            item.copy(
                usesSinceWash = item.usesSinceWash + 1,
                lifetimeUses = item.lifetimeUses + 1,
                status = "Worn",
                updatedAt = createdAt,
            ),
        )
        usageEvents.value += UsageEventEntity(
            id = (usageEvents.value.size + 1).toLong(),
            itemId = itemId,
            usedAt = usedAt,
            notes = notes,
            createdAt = createdAt,
        )
    }

    override suspend fun deleteUsageEvent(eventId: Long, updatedAt: String) {
        usageEvents.value = usageEvents.value.filterNot { it.id == eventId }
    }

    override suspend fun addToBasket(entry: LaundryBasketEntryEntity) {
        if (entry.itemId !in basketIds.value) basketIds.value += entry.itemId
    }

    override suspend fun removeFromBasket(itemId: Long) {
        basketIds.value -= itemId
    }

    override suspend fun recordWashes(
        itemIds: List<Long>,
        washedAt: String,
        comment: String?,
        createdAt: String,
        outOfCycleItemIds: Set<Long>,
    ): Boolean {
        val selected = itemIds.map { getItem(it) ?: return false }
        selected.forEach { item ->
            updateItem(
                item.copy(
                    usesSinceWash = 0,
                    washingCount = item.washingCount + 1,
                    lastWashingDate = washedAt,
                    status = "Clean",
                    updatedAt = createdAt,
                ),
            )
            washEvents.value += WashEventEntity(
                id = (washEvents.value.size + 1).toLong(),
                itemId = item.id,
                washedAt = washedAt,
                usesAtTimeOfWash = item.usesSinceWash,
                comment = comment,
                wasOutOfCycle = item.id in outOfCycleItemIds,
                createdAt = createdAt,
            )
            removeFromBasket(item.id)
        }
        return true
    }

    override suspend fun archiveItem(itemId: Long, archivedAt: String) {
        val item = getItem(itemId) ?: return
        updateItem(item.copy(status = "Archived", archivedAt = archivedAt, updatedAt = archivedAt))
    }
}
