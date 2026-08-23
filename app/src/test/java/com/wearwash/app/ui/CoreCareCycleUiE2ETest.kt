package com.wearwash.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.wearwash.app.data.ItemRepository
import com.wearwash.app.data.local.entity.LaundryBasketEntryEntity
import com.wearwash.app.data.local.entity.CategoryEntity
import com.wearwash.app.data.local.entity.FutureEventEntity
import com.wearwash.app.data.local.entity.FutureEventItemEntity
import com.wearwash.app.data.local.entity.UsageEventEntity
import com.wearwash.app.data.local.entity.WashEventEntity
import com.wearwash.app.data.local.entity.WashableItemEntity
import com.wearwash.app.ui.screens.items.ItemsScreen
import com.wearwash.app.ui.screens.items.ItemsViewModel
import com.wearwash.app.ui.screens.items.MAX_EVENT_ITEMS
import com.wearwash.app.ui.theme.WearWashTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoreCareCycleUiE2ETest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test(timeout = 60_000)
    fun `user completes register use basket and wash journey`() {
        val repository = UiTestItemRepository()
        val viewModel = ItemsViewModel(repository)
        composeRule.setContent {
            WearWashTheme {
                ItemsScreen(itemRepository = repository, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("manage-categories").performClick()
        composeRule.onNodeWithTag("category-manager").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()

        composeRule.onNodeWithTag("add-item").performClick()
        composeRule.onNodeWithTag("item-name").performTextInput("Gym shirt")
        composeRule.onNodeWithTag("save-item").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.currentItems.any { it.id == 1L }
        }
        composeRule.waitForIdle()
        assertEquals(
            0,
            composeRule.onAllNodesWithText("Used today").fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithTag("items-list").performScrollToNode(hasTestTag("item-1"))
        composeRule.onNodeWithTag("item-1").assertIsDisplayed()
        composeRule.onNodeWithTag("item-select-1").performClick()
        repeat(3) {
            composeRule.onNodeWithTag("use-selected-items").performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("${it + 1} uses since wash")
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
        composeRule.onNodeWithText("Needs washing").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("basket-selected-items").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.currentBasketIds == listOf(1L)
        }
        composeRule.onNodeWithTag("basket-tab").performClick()
        composeRule.onNodeWithTag("basket-list")
            .performScrollToNode(hasTestTag("basket-select-1"))

        composeRule.onNodeWithText("Wash all").performClick()
        composeRule.onNodeWithTag("confirm-wash").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("The basket is empty.").assertIsDisplayed()
                composeRule.onNodeWithText("No items currently need washing.").assertIsDisplayed()
            }.isSuccess
        }
    }

    @Test(timeout = 60_000)
    fun `wash selected requires at least one checked basket item`() {
        val repository = UiTestItemRepository(
            initialItems = listOf(uiTestItem(1, "Gym shirt")),
            initialBasketIds = listOf(1L),
        )
        val viewModel = ItemsViewModel(repository)
        composeRule.setContent {
            WearWashTheme {
                ItemsScreen(itemRepository = repository, viewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.basketItems.any { it.id == 1L }
        }
        composeRule.onNodeWithTag("basket-tab").performClick()

        composeRule.onNodeWithTag("wash-selected").performClick()

        composeRule.onNodeWithText("Select at least one item to wash.").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithTag("confirm-wash").fetchSemanticsNodes().size,
        )
        assertEquals(listOf(1L), repository.currentBasketIds)
    }

    @Test(timeout = 60_000)
    fun `user searches event clothes and adds them to the regular basket`() {
        val repository = UiTestItemRepository(
            initialItems = listOf(
                uiTestItem(1, "Blue shirt"),
                uiTestItem(2, "Red trousers"),
            ),
        )
        val viewModel = ItemsViewModel(repository)
        composeRule.setContent {
            WearWashTheme {
                ItemsScreen(itemRepository = repository, viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.allItems.size == 2
        }

        composeRule.onNodeWithTag("events-tab").performClick()
        composeRule.onNodeWithTag("add-event").performClick()
        composeRule.onNodeWithTag("event-name").performTextInput("Family dinner")
        composeRule.onNodeWithTag("event-item-search").performScrollTo().performTextInput("Blue")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.currentEventForm?.itemSearchQuery == "Blue"
        }
        composeRule.onNodeWithTag("event-item-1").performScrollTo().performClick()
        composeRule.onNodeWithTag("save-event").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Family dinner").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag("event-reminder").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("event-item-select-1-1").performScrollTo().performClick()
        composeRule.onNodeWithTag("add-event-items-1").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.currentBasketIds == listOf(1L)
        }
    }

    @Test(timeout = 60_000)
    fun `category filter limits the visible item state`() {
        val repository = UiTestItemRepository(
            initialItems = listOf(
                uiTestItem(1, "Blue shirt", categoryId = 1, categoryName = "Tops"),
                uiTestItem(2, "Bath towel", categoryId = 2, categoryName = "Towels"),
            ),
            initialCategories = listOf(
                uiTestCategory(1, "tops"),
                uiTestCategory(2, "towels"),
            ),
        )
        val viewModel = ItemsViewModel(repository)
        composeRule.setContent {
            WearWashTheme {
                ItemsScreen(itemRepository = repository, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("category-filter").assertIsDisplayed()
        viewModel.selectCategoryFilter(2)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.items.map { it.name } == listOf("Bath towel")
        }
    }

    @Test
    fun `event form limits item selection to six`() {
        val repository = UiTestItemRepository()
        val viewModel = ItemsViewModel(repository)
        composeRule.setContent {
            WearWashTheme {
                ItemsScreen(itemRepository = repository, viewModel = viewModel)
            }
        }
        viewModel.openNewEventEditor()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.eventForm != null
        }
        composeRule.runOnIdle {
            (1L..7L).forEach(viewModel::toggleEventItemSelection)
        }
        assertEquals(
            MAX_EVENT_ITEMS,
            viewModel.currentEventForm?.selectedItemIds?.size,
        )
    }

}

private class UiTestItemRepository(
    initialItems: List<WashableItemEntity> = emptyList(),
    initialCategories: List<CategoryEntity> = emptyList(),
    initialBasketIds: List<Long> = emptyList(),
) : ItemRepository {
    private val items = MutableStateFlow(initialItems)
    private val categories = MutableStateFlow(initialCategories)
    private val basketIds = MutableStateFlow(initialBasketIds)
    private val usageEvents = MutableStateFlow<List<UsageEventEntity>>(emptyList())
    private val washEvents = MutableStateFlow<List<WashEventEntity>>(emptyList())
    private val futureEvents = MutableStateFlow<List<FutureEventEntity>>(emptyList())
    private val futureEventItems = MutableStateFlow<List<FutureEventItemEntity>>(emptyList())
    val currentBasketIds: List<Long>
        get() = basketIds.value
    val currentItems: List<WashableItemEntity>
        get() = items.value

    override fun observeCategories(): Flow<List<CategoryEntity>> = categories
    override fun observeFutureEvents(): Flow<List<FutureEventEntity>> = futureEvents
    override fun observeFutureEventItems(): Flow<List<FutureEventItemEntity>> = futureEventItems

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

    override suspend fun saveFutureEvent(
        event: FutureEventEntity,
        itemIds: Set<Long>,
    ): Long {
        val id = event.id.takeIf { it != 0L }
            ?: ((futureEvents.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        futureEvents.value = futureEvents.value.filterNot { it.id == id } + event.copy(id = id)
        val retained = futureEventItems.value
            .filter { it.eventId == id }
            .associateBy { it.itemId }
        futureEventItems.value = futureEventItems.value.filterNot { it.eventId == id } +
            itemIds.map { itemId ->
                retained[itemId] ?: FutureEventItemEntity(
                    eventId = id,
                    itemId = itemId,
                    addedAt = event.updatedAt,
                )
            }
        return id
    }

    override suspend fun deleteFutureEvent(eventId: Long) {
        futureEvents.value = futureEvents.value.filterNot { it.id == eventId }
        futureEventItems.value = futureEventItems.value.filterNot { it.eventId == eventId }
    }

}

private fun uiTestCategory(id: Long, systemKey: String) = CategoryEntity(
    id = id,
    systemKey = systemKey,
    customName = null,
    isPredefined = true,
    washingCriteriaType = "ByUsage",
    washingUsageThreshold = 3,
    washingDayThreshold = null,
    createdAt = "2026-07-25T08:00:00-03:00",
    updatedAt = "2026-07-25T08:00:00-03:00",
)

private fun uiTestItem(
    id: Long,
    name: String,
    categoryId: Long? = null,
    categoryName: String? = null,
) = WashableItemEntity(
    id = id,
    name = name,
    categoryId = categoryId,
    categoryName = categoryName,
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
    washingCriteriaType = "ByUsage",
    washingUsageThreshold = 3,
    washingDayThreshold = null,
    status = "Clean",
    createdAt = "2026-07-25T08:00:00-03:00",
    updatedAt = "2026-07-25T08:00:00-03:00",
    archivedAt = null,
)
