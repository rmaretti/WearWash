package com.wearwash.app.ui.screens.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wearwash.app.data.ItemRepository
import com.wearwash.app.data.local.entity.LaundryBasketEntryEntity
import com.wearwash.app.data.local.entity.UsageEventEntity
import com.wearwash.app.data.local.entity.WashEventEntity
import com.wearwash.app.data.local.entity.WashableItemEntity
import com.wearwash.app.data.local.entity.CategoryEntity
import com.wearwash.app.data.local.entity.FutureEventEntity
import com.wearwash.app.data.local.entity.FutureEventItemEntity
import com.wearwash.app.domain.logic.WashingReadinessReason
import com.wearwash.app.domain.logic.WashingRule
import com.wearwash.app.domain.logic.evaluateWashingReadiness
import com.wearwash.app.domain.model.WashableItemStatus
import com.wearwash.app.domain.model.WashingCriteriaType
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainDestination { Items, Basket, Events }

enum class EventPreparationStatus { Planned, NeedsPreparation, Prepared }

data class ItemUiModel(
    val id: Long,
    val name: String,
    val category: String,
    val brand: String?,
    val usesSinceWash: Int,
    val lifetimeUses: Int,
    val washingCount: Int,
    val lifecycleStatus: WashableItemStatus,
    val needsWashing: Boolean,
    val readinessReason: WashingReadinessReason?,
    val inBasket: Boolean,
)

data class ItemFormState(
    val id: Long = 0,
    val categoryId: Long? = null,
    val name: String = "",
    val category: String = "",
    val color: String = "",
    val brand: String = "",
    val photoUri: String = "",
    val fabric: String = "",
    val season: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "",
    val description: String = "",
    val initialUsageCount: String = "0",
    val initialWashingCount: String = "0",
    val lastWashingDate: String = "",
    val washingCriteriaType: WashingCriteriaType = WashingCriteriaType.ByUsage,
    val washingUsageThreshold: String = "3",
    val washingDayThreshold: String = "",
    val showAdvancedDetails: Boolean = false,
)

data class CategoryFormState(
    val id: Long = 0,
    val name: String = "",
    val systemKey: String? = null,
    val isPredefined: Boolean = false,
    val washingCriteriaType: WashingCriteriaType = WashingCriteriaType.ByUsage,
    val washingUsageThreshold: String = "3",
    val washingDayThreshold: String = "",
)

data class CategoryManagerUiState(
    val isOpen: Boolean = false,
    val searchQuery: String = "",
    val form: CategoryFormState? = null,
    val hasSaveError: Boolean = false,
    val hasDeleteError: Boolean = false,
)

data class WashFormState(
    val itemIds: Set<Long> = emptySet(),
    val washedAt: String = LocalDate.now().toString(),
    val comment: String = "",
    val hasDateConflict: Boolean = false,
)

data class UsageHistoryItem(
    val id: Long,
    val usedAt: String,
    val notes: String?,
)

data class WashHistoryItem(
    val id: Long,
    val washedAt: String,
    val comment: String?,
    val wasOutOfCycle: Boolean,
)

data class ItemDetailUiModel(
    val item: ItemUiModel,
    val usageHistory: List<UsageHistoryItem>,
    val washHistory: List<WashHistoryItem>,
)

data class EventItemUiModel(
    val item: ItemUiModel,
    val status: EventPreparationStatus,
)

data class FutureEventUiModel(
    val id: Long,
    val name: String,
    val eventDate: LocalDate,
    val description: String?,
    val reminderDaysBefore: Int,
    val items: List<EventItemUiModel>,
    val reminderDue: Boolean,
    val isPast: Boolean,
)

data class FutureEventFormState(
    val id: Long = 0,
    val name: String = "",
    val eventDate: String = "",
    val description: String = "",
    val reminderDaysBefore: String = "1",
    val selectedItemIds: Set<Long> = emptySet(),
)

data class ItemsUiState(
    val items: List<ItemUiModel> = emptyList(),
    val allItems: List<ItemUiModel> = emptyList(),
    val basketItems: List<ItemUiModel> = emptyList(),
    val suggestedItems: List<ItemUiModel> = emptyList(),
    val searchQuery: String = "",
    val destination: MainDestination = MainDestination.Items,
    val isEditorOpen: Boolean = false,
    val form: ItemFormState = ItemFormState(),
    val detail: ItemDetailUiModel? = null,
    val washForm: WashFormState? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val categoryManager: CategoryManagerUiState = CategoryManagerUiState(),
    val selectedCategoryId: Long? = null,
    val events: List<FutureEventUiModel> = emptyList(),
    val eventForm: FutureEventFormState? = null,
)

private data class ItemsSnapshot(
    val items: List<ItemUiModel>,
    val allItems: List<ItemUiModel>,
    val basketItems: List<ItemUiModel>,
    val suggestedItems: List<ItemUiModel>,
)

private data class SurfaceState(
    val query: String,
    val editor: EditorState,
    val destination: MainDestination,
    val washForm: WashFormState?,
    val categoryManager: CategoryManagerUiState,
    val selectedCategoryId: Long?,
    val eventForm: FutureEventFormState?,
)

private data class SecondarySurfaceState(
    val categoryManager: CategoryManagerUiState,
    val selectedCategoryId: Long?,
    val eventForm: FutureEventFormState?,
)

private data class ReferenceSnapshot(
    val categories: List<CategoryEntity>,
    val events: List<FutureEventUiModel>,
)

private data class EditorState(
    val isOpen: Boolean = false,
    val form: ItemFormState = ItemFormState(),
)

private data class DetailSnapshot(
    val item: WashableItemEntity,
    val usageEvents: List<UsageEventEntity>,
    val washEvents: List<WashEventEntity>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ItemsViewModel(
    private val itemRepository: ItemRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val editorState = MutableStateFlow(EditorState())
    private val destination = MutableStateFlow(MainDestination.Items)
    private val selectedItemId = MutableStateFlow<Long?>(null)
    private val washForm = MutableStateFlow<WashFormState?>(null)
    private val today = MutableStateFlow(LocalDate.now())
    private val categoryManager = MutableStateFlow(CategoryManagerUiState())
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val eventForm = MutableStateFlow<FutureEventFormState?>(null)

    private val itemEntities = combine(searchQuery, selectedCategoryId) { query, categoryId ->
        query to categoryId
    }.flatMapLatest { (query, categoryId) ->
        itemRepository.searchItems(query).map { items ->
            if (categoryId == null) items else items.filter { it.categoryId == categoryId }
        }
    }

    private val itemsSnapshot = combine(
        itemEntities,
        itemRepository.observeActiveItems(),
        itemRepository.observeBasketItemIds(),
        itemRepository.observeBasketItems(),
        today,
    ) { entities, allEntities, basketIds, basketEntities, currentDate ->
        val basketIdSet = basketIds.toSet()
        val items = entities.map { it.toUiModel(basketIdSet, currentDate) }
        val allItems = allEntities.map { it.toUiModel(basketIdSet, currentDate) }
        val basketItems = basketEntities.map { it.toUiModel(basketIdSet, currentDate) }
        val suggestedItems = allItems.filter { it.needsWashing && !it.inBasket }
        ItemsSnapshot(items, allItems, basketItems, suggestedItems)
    }

    private val secondarySurfaceState = combine(
        categoryManager,
        selectedCategoryId,
        eventForm,
    ) { categoryState, categoryId, futureEventForm ->
        SecondarySurfaceState(categoryState, categoryId, futureEventForm)
    }

    private val surfaceState = combine(
        searchQuery,
        editorState,
        destination,
        washForm,
        secondarySurfaceState,
    ) { query, editor, target, wash, secondary ->
        SurfaceState(
            query,
            editor,
            target,
            wash,
            secondary.categoryManager,
            secondary.selectedCategoryId,
            secondary.eventForm,
        )
    }

    private val eventSnapshot = combine(
        itemRepository.observeFutureEvents(),
        itemRepository.observeFutureEventItems(),
        itemRepository.observeActiveItems(),
        itemRepository.observeBasketItemIds(),
        today,
    ) { events, assignments, items, basketIds, currentDate ->
        val itemMap = items.associateBy { it.id }
        val basketIdSet = basketIds.toSet()
        events.mapNotNull { event ->
            val date = event.eventDate.toLocalDateOrNull() ?: return@mapNotNull null
            val eventItems = assignments
                .filter { it.eventId == event.id }
                .mapNotNull { assignment ->
                    val entity = itemMap[assignment.itemId] ?: return@mapNotNull null
                    val item = entity.toUiModel(basketIdSet, currentDate)
                    EventItemUiModel(
                        item = item,
                        status = when {
                            assignment.status == EventPreparationStatus.Prepared.name ->
                                EventPreparationStatus.Prepared
                            item.needsWashing -> EventPreparationStatus.NeedsPreparation
                            else -> EventPreparationStatus.Planned
                        },
                    )
                }
            FutureEventUiModel(
                id = event.id,
                name = event.name,
                eventDate = date,
                description = event.description,
                reminderDaysBefore = event.reminderDaysBefore,
                items = eventItems,
                reminderDue = !date.isBefore(currentDate) &&
                    !currentDate.isBefore(date.minusDays(event.reminderDaysBefore.toLong())),
                isPast = date.isBefore(currentDate),
            )
        }
    }

    private val detailSnapshot: Flow<DetailSnapshot?> = selectedItemId.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            combine(
                itemRepository.observeItem(id),
                itemRepository.observeUsageEvents(id),
                itemRepository.observeWashEvents(id),
            ) { item, usageEvents, washEvents ->
                item?.let { DetailSnapshot(it, usageEvents, washEvents) }
            }
        }
    }

    private val referenceSnapshot = combine(
        itemRepository.observeCategories(),
        eventSnapshot,
    ) { categories, events -> ReferenceSnapshot(categories, events) }

    val uiState: StateFlow<ItemsUiState> = combine(
        itemsSnapshot,
        surfaceState,
        detailSnapshot,
        today,
        referenceSnapshot,
    ) { snapshot, surface, detail, currentDate, references ->
        val basketIds = snapshot.basketItems.mapTo(mutableSetOf()) { it.id }
        ItemsUiState(
            items = snapshot.items,
            allItems = snapshot.allItems,
            basketItems = snapshot.basketItems,
            suggestedItems = snapshot.suggestedItems,
            searchQuery = surface.query,
            destination = surface.destination,
            isEditorOpen = surface.editor.isOpen,
            form = surface.editor.form,
            detail = detail?.let {
                ItemDetailUiModel(
                    item = it.item.toUiModel(basketIds, currentDate),
                    usageHistory = it.usageEvents.map { event ->
                        UsageHistoryItem(event.id, event.usedAt, event.notes)
                    },
                    washHistory = it.washEvents.map { event ->
                        WashHistoryItem(
                            event.id,
                            event.washedAt,
                            event.comment,
                            event.wasOutOfCycle,
                        )
                    },
                )
            },
            washForm = surface.washForm,
            categories = references.categories,
            categoryManager = surface.categoryManager,
            selectedCategoryId = surface.selectedCategoryId,
            events = references.events,
            eventForm = surface.eventForm,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ItemsUiState())

    fun refreshDate() {
        today.value = LocalDate.now()
    }

    fun showItems() {
        destination.value = MainDestination.Items
    }

    fun showBasket() {
        destination.value = MainDestination.Basket
    }

    fun showEvents() {
        destination.value = MainDestination.Events
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectCategoryFilter(categoryId: Long?) {
        selectedCategoryId.value = categoryId
    }

    fun openNewItemEditor() {
        editorState.value = EditorState(isOpen = true, form = ItemFormState())
    }

    fun openEditItemEditor(itemId: Long) {
        viewModelScope.launch {
            val item = itemRepository.getItem(itemId) ?: return@launch
            editorState.value = EditorState(isOpen = true, form = item.toFormState())
        }
    }

    fun closeEditor() {
        editorState.value = EditorState()
    }

    fun updateForm(form: ItemFormState) {
        editorState.update { it.copy(form = form) }
    }

    fun selectCategory(category: CategoryEntity, displayName: String) {
        editorState.update { editor ->
            editor.copy(
                form = editor.form.copy(
                    categoryId = category.id,
                    category = displayName,
                    washingCriteriaType = category.washingCriteriaType.toWashingCriteriaType(),
                    washingUsageThreshold = category.washingUsageThreshold?.toString().orEmpty(),
                    washingDayThreshold = category.washingDayThreshold?.toString().orEmpty(),
                ),
            )
        }
    }

    fun openCategoryManager() {
        categoryManager.value = CategoryManagerUiState(isOpen = true)
    }

    fun closeCategoryManager() {
        categoryManager.value = CategoryManagerUiState()
    }

    fun updateCategorySearch(query: String) {
        categoryManager.update { it.copy(searchQuery = query) }
    }

    fun createCategory() {
        categoryManager.update { it.copy(form = CategoryFormState(), hasSaveError = false) }
    }

    fun editCategory(category: CategoryEntity) {
        categoryManager.update {
            it.copy(
                form = CategoryFormState(
                    id = category.id,
                    name = category.customName.orEmpty(),
                    systemKey = category.systemKey,
                    isPredefined = category.isPredefined,
                    washingCriteriaType = category.washingCriteriaType.toWashingCriteriaType(),
                    washingUsageThreshold = category.washingUsageThreshold?.toString().orEmpty(),
                    washingDayThreshold = category.washingDayThreshold?.toString().orEmpty(),
                ),
                hasSaveError = false,
                hasDeleteError = false,
            )
        }
    }

    fun updateCategoryForm(form: CategoryFormState) {
        categoryManager.update { it.copy(form = form, hasSaveError = false) }
    }

    fun cancelCategoryEdit() {
        categoryManager.update { it.copy(form = null, hasSaveError = false) }
    }

    fun saveCategory() {
        val form = categoryManager.value.form ?: return
        if ((!form.isPredefined && form.name.isBlank()) || !form.hasValidRule()) return
        viewModelScope.launch {
            val now = OffsetDateTime.now().toString()
            val saved = itemRepository.saveCategory(
                CategoryEntity(
                    id = form.id,
                    systemKey = form.systemKey,
                    customName = if (form.isPredefined) null else form.name.trim(),
                    isPredefined = form.isPredefined,
                    washingCriteriaType = form.washingCriteriaType.name,
                    washingUsageThreshold = form.washingUsageThreshold.toPositiveIntOrNull(),
                    washingDayThreshold = form.washingDayThreshold.toPositiveIntOrNull(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            categoryManager.update { it.copy(form = if (saved) null else form, hasSaveError = !saved) }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            val deleted = itemRepository.deleteCategory(categoryId)
            categoryManager.update { it.copy(hasDeleteError = !deleted) }
        }
    }

    fun saveForm() {
        val form = editorState.value.form
        if (!form.isValid(today.value)) return
        viewModelScope.launch {
            val existingItem = form.id.takeIf { it != 0L }?.let { itemRepository.getItem(it) }
            itemRepository.saveItem(form.toEntity(existingItem))
            closeEditor()
        }
    }

    fun openItemDetail(itemId: Long) {
        selectedItemId.value = itemId
    }

    fun closeItemDetail() {
        selectedItemId.value = null
    }

    fun markItemUsed(itemId: Long) {
        recordUsage(itemId, LocalDate.now().toString(), null)
    }

    fun recordUsage(itemId: Long, usedAt: String, notes: String?) {
        val date = usedAt.toLocalDateOrNull() ?: return
        if (date.isAfter(today.value)) return
        viewModelScope.launch {
            val now = OffsetDateTime.now().toString()
            itemRepository.recordUsage(
                itemId = itemId,
                usedAt = date.toString(),
                notes = notes?.trimToNull(),
                createdAt = now,
            )
        }
    }

    fun deleteUsageEvent(eventId: Long) {
        viewModelScope.launch {
            itemRepository.deleteUsageEvent(eventId, OffsetDateTime.now().toString())
        }
    }

    fun addToBasket(itemId: Long, reason: String? = null) {
        viewModelScope.launch {
            itemRepository.addToBasket(
                LaundryBasketEntryEntity(
                    itemId = itemId,
                    addedAt = OffsetDateTime.now().toString(),
                    reason = reason,
                    comment = null,
                ),
            )
        }
    }

    fun removeFromBasket(itemId: Long) {
        viewModelScope.launch { itemRepository.removeFromBasket(itemId) }
    }

    fun openWashDialog(itemIds: Set<Long>) {
        if (itemIds.isNotEmpty()) washForm.value = WashFormState(itemIds = itemIds)
    }

    fun updateWashForm(form: WashFormState) {
        washForm.value = form.copy(hasDateConflict = false)
    }

    fun closeWashDialog() {
        washForm.value = null
    }

    fun saveWash() {
        val form = washForm.value ?: return
        val washedAt = form.washedAt.toLocalDateOrNull() ?: return
        if (washedAt.isAfter(today.value)) return
        viewModelScope.launch {
            val items = form.itemIds.mapNotNull { itemRepository.getItem(it) }
            if (items.size != form.itemIds.size) return@launch
            val outOfCycleIds = items
                .filterNot { it.readiness(washedAt).needsWashing }
                .mapTo(mutableSetOf()) { it.id }
            val saved = itemRepository.recordWashes(
                itemIds = items.map { it.id },
                washedAt = washedAt.toString(),
                comment = form.comment.trimToNull(),
                createdAt = OffsetDateTime.now().toString(),
                outOfCycleItemIds = outOfCycleIds,
            )
            if (saved) {
                closeWashDialog()
            } else {
                washForm.update { current -> current?.copy(hasDateConflict = true) }
            }
        }
    }

    fun archiveItem(itemId: Long) {
        viewModelScope.launch {
            val now = OffsetDateTime.now().toString()
            itemRepository.archiveItem(itemId, now)
            closeItemDetail()
        }
    }

    fun openNewEventEditor() {
        eventForm.value = FutureEventFormState(eventDate = today.value.plusDays(1).toString())
    }

    fun openEditEventEditor(eventId: Long) {
        val event = uiState.value.events.firstOrNull { it.id == eventId } ?: return
        eventForm.value = FutureEventFormState(
            id = event.id,
            name = event.name,
            eventDate = event.eventDate.toString(),
            description = event.description.orEmpty(),
            reminderDaysBefore = event.reminderDaysBefore.toString(),
            selectedItemIds = event.items.mapTo(mutableSetOf()) { it.item.id },
        )
    }

    fun updateEventForm(form: FutureEventFormState) {
        eventForm.value = form
    }

    fun closeEventEditor() {
        eventForm.value = null
    }

    fun saveEvent() {
        val form = eventForm.value ?: return
        val eventDate = form.eventDate.toLocalDateOrNull() ?: return
        val reminderDays = form.reminderDaysBefore.toIntOrNull()?.takeIf { it >= 0 } ?: return
        if (form.name.isBlank() || eventDate.isBefore(today.value)) return
        viewModelScope.launch {
            val now = OffsetDateTime.now().toString()
            itemRepository.saveFutureEvent(
                FutureEventEntity(
                    id = form.id,
                    name = form.name.trim(),
                    eventDate = eventDate.toString(),
                    description = form.description.trimToNull(),
                    reminderDaysBefore = reminderDays,
                    createdAt = now,
                    updatedAt = now,
                ),
                form.selectedItemIds,
            )
            closeEventEditor()
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch { itemRepository.deleteFutureEvent(eventId) }
    }

    fun setEventItemPrepared(eventId: Long, itemId: Long, isPrepared: Boolean) {
        viewModelScope.launch {
            itemRepository.updateEventItemPreparation(eventId, itemId, isPrepared)
        }
    }

    companion object {
        fun factory(itemRepository: ItemRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ItemsViewModel(itemRepository) as T
            }
    }
}

private fun WashableItemEntity.toUiModel(
    basketIds: Set<Long>,
    today: LocalDate,
): ItemUiModel {
    val readiness = readiness(today)
    return ItemUiModel(
        id = id,
        name = name,
        category = categoryName.orEmpty().ifBlank { "Uncategorized" },
        brand = brand,
        usesSinceWash = usesSinceWash,
        lifetimeUses = lifetimeUses,
        washingCount = washingCount,
        lifecycleStatus = when {
            archivedAt != null -> WashableItemStatus.Archived
            usesSinceWash > 0 -> WashableItemStatus.Worn
            else -> WashableItemStatus.Clean
        },
        needsWashing = readiness.needsWashing,
        readinessReason = readiness.reason,
        inBasket = id in basketIds,
    )
}

private fun WashableItemEntity.readiness(today: LocalDate) = evaluateWashingReadiness(
    rule = WashingRule(
        type = washingCriteriaType.toWashingCriteriaType(),
        usageThreshold = washingUsageThreshold,
        dayThreshold = washingDayThreshold,
    ),
    usesSinceWash = usesSinceWash,
    lastWashingDate = lastWashingDate?.toLocalDateOrNull(),
    today = today,
)

private fun WashableItemEntity.toFormState(): ItemFormState = ItemFormState(
    id = id,
    categoryId = categoryId,
    name = name,
    category = categoryName.orEmpty(),
    color = colorName.orEmpty(),
    brand = brand.orEmpty(),
    photoUri = photoUri.orEmpty(),
    fabric = fabricName.orEmpty(),
    season = seasonName.orEmpty(),
    purchaseDate = purchaseDate.orEmpty(),
    purchasePrice = purchasePriceCents?.let { cents ->
        cents.toBigDecimal().movePointLeft(2).stripTrailingZeros().toPlainString()
    }.orEmpty(),
    description = description.orEmpty(),
    initialUsageCount = usesSinceWash.toString(),
    initialWashingCount = washingCount.toString(),
    lastWashingDate = lastWashingDate.orEmpty(),
    washingCriteriaType = washingCriteriaType.toWashingCriteriaType(),
    washingUsageThreshold = washingUsageThreshold?.toString().orEmpty(),
    washingDayThreshold = washingDayThreshold?.toString().orEmpty(),
)

private fun ItemFormState.hasValidRule(): Boolean = when (washingCriteriaType) {
    WashingCriteriaType.ByUsage -> washingUsageThreshold.toPositiveIntOrNull() != null
    WashingCriteriaType.ByDate -> washingDayThreshold.toPositiveIntOrNull() != null
    WashingCriteriaType.ByUsageOrDate ->
        washingUsageThreshold.toPositiveIntOrNull() != null &&
            washingDayThreshold.toPositiveIntOrNull() != null
    WashingCriteriaType.Manual -> true
}

internal fun ItemFormState.isValid(today: LocalDate = LocalDate.now()): Boolean =
    name.isNotBlank() &&
        hasValidRule() &&
        purchaseDate.isBlankOrValidDate(today) &&
        lastWashingDate.isBlankOrValidDate(today) &&
        purchasePrice.isBlankOrNonNegativeDecimal() &&
        initialUsageCount.isNonNegativeInt() &&
        initialWashingCount.isNonNegativeInt()

private fun CategoryFormState.hasValidRule(): Boolean = when (washingCriteriaType) {
    WashingCriteriaType.ByUsage -> washingUsageThreshold.toPositiveIntOrNull() != null
    WashingCriteriaType.ByDate -> washingDayThreshold.toPositiveIntOrNull() != null
    WashingCriteriaType.ByUsageOrDate ->
        washingUsageThreshold.toPositiveIntOrNull() != null &&
            washingDayThreshold.toPositiveIntOrNull() != null
    WashingCriteriaType.Manual -> true
}

private fun ItemFormState.toEntity(existingItem: WashableItemEntity?): WashableItemEntity {
    val now = OffsetDateTime.now().toString()
    val usesSinceWash = existingItem?.usesSinceWash ?: initialUsageCount.toNonNegativeInt()
    return WashableItemEntity(
        id = id,
        name = name.trim(),
        categoryId = categoryId,
        categoryName = category.trimToNull(),
        colorId = existingItem?.colorId,
        colorName = color.trimToNull(),
        brand = brand.trimToNull(),
        photoUri = photoUri.trimToNull(),
        fabricId = existingItem?.fabricId,
        fabricName = fabric.trimToNull(),
        seasonId = existingItem?.seasonId,
        seasonName = season.trimToNull(),
        purchaseDate = purchaseDate.trimToNull(),
        purchasePriceCents = purchasePrice.toPriceCentsOrNull(),
        description = description.trimToNull(),
        usesSinceWash = usesSinceWash,
        lifetimeUses = existingItem?.lifetimeUses ?: usesSinceWash,
        washingCount = existingItem?.washingCount ?: initialWashingCount.toNonNegativeInt(),
        lastWashingDate = existingItem?.lastWashingDate ?: lastWashingDate.trimToNull(),
        washingCriteriaType = washingCriteriaType.name,
        washingUsageThreshold = washingUsageThreshold.toPositiveIntOrNull(),
        washingDayThreshold = washingDayThreshold.toPositiveIntOrNull(),
        status = existingItem?.status
            ?: if (usesSinceWash > 0) WashableItemStatus.Worn.name else WashableItemStatus.Clean.name,
        createdAt = existingItem?.createdAt ?: now,
        updatedAt = now,
        archivedAt = existingItem?.archivedAt,
    )
}

private fun String.toWashingCriteriaType(): WashingCriteriaType =
    runCatching { WashingCriteriaType.valueOf(this) }.getOrDefault(WashingCriteriaType.ByUsage)

private fun String.toNonNegativeInt(): Int = toIntOrNull()?.coerceAtLeast(0) ?: 0
private fun String.toPositiveIntOrNull(): Int? = toIntOrNull()?.takeIf { it > 0 }
private fun String.toPriceCentsOrNull(): Long? = toBigDecimalOrNull()
    ?.setScale(2, RoundingMode.HALF_UP)
    ?.movePointRight(2)
    ?.toLong()
private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(trim()) }.getOrNull()
private fun String.trimToNull(): String? = trim().ifBlank { null }
private fun String.isBlankOrValidDate(today: LocalDate): Boolean =
    isBlank() || toLocalDateOrNull()?.let { !it.isAfter(today) } == true
private fun String.isBlankOrNonNegativeDecimal(): Boolean =
    isBlank() || toBigDecimalOrNull()?.let { it.signum() >= 0 } == true
private fun String.isNonNegativeInt(): Boolean = toIntOrNull()?.let { it >= 0 } == true
