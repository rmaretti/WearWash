package com.wearwash.app.ui.screens.items

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wearwash.app.R
import com.wearwash.app.data.ItemRepository
import com.wearwash.app.data.local.entity.CategoryEntity
import com.wearwash.app.domain.logic.WashingReadinessReason
import com.wearwash.app.domain.model.WashableItemStatus
import com.wearwash.app.domain.model.WashingCriteriaType
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    itemRepository: ItemRepository,
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.factory(itemRepository)),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDate()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.wearwash_logo),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(R.string.tagline),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            AppNavigationBar(
                destination = uiState.destination,
                basketCount = uiState.basketItems.size,
                onItems = viewModel::showItems,
                onBasket = viewModel::showBasket,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (uiState.destination) {
                MainDestination.Items -> ItemsContent(uiState, viewModel)
                MainDestination.Basket -> BasketContent(uiState, viewModel)
            }
        }
    }

    if (uiState.isEditorOpen) {
        ItemEditorDialog(
            form = uiState.form,
            categories = uiState.categories,
            onFormChange = viewModel::updateForm,
            onCategorySelected = viewModel::selectCategory,
            onManageCategories = viewModel::openCategoryManager,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::saveForm,
        )
    }
    uiState.detail?.let { detail ->
        ItemDetailDialog(
            detail = detail,
            onDismiss = viewModel::closeItemDetail,
            onUsedToday = { viewModel.markItemUsed(detail.item.id) },
            onRecordUsage = { date, notes -> viewModel.recordUsage(detail.item.id, date, notes) },
            onDeleteUsage = viewModel::deleteUsageEvent,
            onAddToBasket = { viewModel.addToBasket(detail.item.id, "manual") },
            onRemoveFromBasket = { viewModel.removeFromBasket(detail.item.id) },
            onMarkWashed = { viewModel.openWashDialog(setOf(detail.item.id)) },
            onEdit = { viewModel.openEditItemEditor(detail.item.id) },
            onArchive = { viewModel.archiveItem(detail.item.id) },
        )
    }
    uiState.washForm?.let { form ->
        WashDialog(
            form = form,
            onChange = viewModel::updateWashForm,
            onDismiss = viewModel::closeWashDialog,
            onSave = viewModel::saveWash,
        )
    }
    if (uiState.categoryManager.isOpen) {
        CategoryManagerDialog(
            categories = uiState.categories,
            state = uiState.categoryManager,
            onSearchChange = viewModel::updateCategorySearch,
            onNew = viewModel::createCategory,
            onEdit = viewModel::editCategory,
            onDelete = viewModel::deleteCategory,
            onFormChange = viewModel::updateCategoryForm,
            onSave = viewModel::saveCategory,
            onCancelEdit = viewModel::cancelCategoryEdit,
            onDismiss = viewModel::closeCategoryManager,
        )
    }
}

@Composable
private fun AppNavigationBar(
    destination: MainDestination,
    basketCount: Int,
    onItems: () -> Unit,
    onBasket: () -> Unit,
) {
    val navigationColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = Color.White,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = Color.White.copy(alpha = 0.78f),
        unselectedTextColor = Color.White.copy(alpha = 0.78f),
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.secondary) {
        NavigationBarItem(
            selected = destination == MainDestination.Items,
            onClick = onItems,
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text(stringResource(R.string.items_title)) },
            colors = navigationColors,
        )
        NavigationBarItem(
            selected = destination == MainDestination.Basket,
            onClick = onBasket,
            modifier = Modifier.testTag("basket-tab"),
            icon = {
                BadgedBox(
                    badge = {
                        if (basketCount > 0) Badge { Text(basketCount.toString()) }
                    },
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                }
            },
            label = { Text(stringResource(R.string.laundry_basket_title)) },
            colors = navigationColors,
        )
    }
}

@Composable
private fun ItemsContent(uiState: ItemsUiState, viewModel: ItemsViewModel) {
    val needsWashCount = uiState.items.count { it.needsWashing }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.items_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            "${pluralStringResource(R.plurals.wardrobe_item_count, uiState.items.size, uiState.items.size)} · " +
                pluralStringResource(R.plurals.needs_washing_count, needsWashCount, needsWashCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = viewModel::openNewItemEditor,
            modifier = Modifier
                .weight(1f)
                .testTag("add-item"),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_item))
        }
        Button(
            onClick = viewModel::openCategoryManager,
            modifier = Modifier
                .weight(1f)
                .testTag("manage-categories"),
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.categories_title))
        }
    }
    OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = viewModel::updateSearchQuery,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_items)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
    if (uiState.items.isEmpty()) {
        EmptyMessage(R.string.no_items_title, R.string.no_items_body)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.items, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    onOpen = { viewModel.openItemDetail(item.id) },
                    onUsed = { viewModel.markItemUsed(item.id) },
                    onBasket = {
                        if (item.inBasket) viewModel.removeFromBasket(item.id)
                        else viewModel.addToBasket(item.id, "manual")
                    },
                )
            }
        }
    }
}

@Composable
private fun BasketContent(uiState: ItemsUiState, viewModel: ItemsViewModel) {
    val allIds = uiState.basketItems.mapTo(mutableSetOf()) { it.id }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    LaunchedEffect(allIds) {
        selectedIds = selectedIds.intersect(allIds)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.laundry_basket_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            pluralStringResource(
                R.plurals.basket_item_count,
                uiState.basketItems.size,
                uiState.basketItems.size,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (uiState.basketItems.isNotEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.basket_ready_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.basket_ready_body),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.openWashDialog(selectedIds.ifEmpty { allIds }) },
                        enabled = allIds.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wash-selected"),
                    ) {
                        Text(stringResource(R.string.wash_selected))
                    }
                    OutlinedButton(
                        onClick = { viewModel.openWashDialog(allIds) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.wash_all))
                    }
                }
            }
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.basketItems.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.in_basket_section),
                    count = uiState.basketItems.size,
                )
            }
        }
        items(uiState.basketItems, key = { "basket-${it.id}" }) { item ->
            BasketItemCard(
                item = item,
                selected = item.id in selectedIds,
                onSelected = { checked ->
                    selectedIds = if (checked) selectedIds + item.id else selectedIds - item.id
                },
                onOpen = { viewModel.openItemDetail(item.id) },
                onRemove = { viewModel.removeFromBasket(item.id) },
            )
        }
        if (uiState.suggestedItems.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(top = if (uiState.basketItems.isEmpty()) 0.dp else 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SectionHeader(
                        title = stringResource(R.string.suggested_section),
                        count = uiState.suggestedItems.size,
                    )
                    Text(
                        stringResource(R.string.basket_suggestions_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (uiState.basketItems.isEmpty() && uiState.suggestedItems.isEmpty()) {
            item { EmptyBasketMessage() }
        }
        items(uiState.suggestedItems, key = { "suggested-${it.id}" }) { item ->
            ElevatedCard(onClick = { viewModel.openItemDetail(item.id) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ItemMonogram(item.name)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            readinessText(item.readinessReason),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = { viewModel.addToBasket(item.id, "automatic_readiness") },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_to_basket))
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: ItemUiModel,
    onOpen: () -> Unit,
    onUsed: () -> Unit,
    onBasket: () -> Unit,
) {
    val detailDescription = stringResource(R.string.open_item_details, item.name)
    ElevatedCard(
        modifier = Modifier
            .testTag("item-${item.id}")
            .semantics { contentDescription = detailDescription },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ItemMonogram(item.name)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(item.brand?.let { "${item.category} · $it" } ?: item.category)
                    Text(stringResource(R.string.uses_since_wash, item.usesSinceWash))
                }
                StatusBadges(item)
                IconButton(onClick = onOpen) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = detailDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUsed, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.used_today))
                }
                OutlinedButton(
                    onClick = onBasket,
                    modifier = Modifier.weight(1f),
                ) {
                        Text(
                            stringResource(
                                if (item.inBasket) R.string.remove_from_basket
                                else R.string.add_to_basket,
                            ),
                        )
                }
            }
        }
    }
}

@Composable
private fun BasketItemCard(
    item: ItemUiModel,
    selected: Boolean,
    onSelected: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = onSelected,
                modifier = Modifier.testTag("basket-select-${item.id}"),
            )
            ItemMonogram(item.name)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.uses_since_wash, item.usesSinceWash),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpen) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.open_item_details, item.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove))
            }
        }
    }
}

@Composable
private fun StatusBadges(item: ItemUiModel) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatusPill(text = stringResource(item.lifecycleStatus.labelRes), emphasized = false)
        if (item.needsWashing) {
            StatusPill(text = stringResource(R.string.needs_washing), emphasized = true)
        }
        if (item.inBasket) {
            StatusPill(text = stringResource(R.string.in_laundry_basket), emphasized = false)
        }
    }
}

@Composable
private fun StatusPill(text: String, emphasized: Boolean) {
    val background = if (emphasized) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val foreground = if (emphasized) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = foreground,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun ItemMonogram(name: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "•",
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
        ) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun EmptyBasketMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(stringResource(R.string.basket_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.basket_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.basket_empty_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.no_suggestions),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemDetailDialog(
    detail: ItemDetailUiModel,
    onDismiss: () -> Unit,
    onUsedToday: () -> Unit,
    onRecordUsage: (String, String?) -> Unit,
    onDeleteUsage: (Long) -> Unit,
    onAddToBasket: () -> Unit,
    onRemoveFromBasket: () -> Unit,
    onMarkWashed: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    var usageDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var usageNotes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(detail.item.name) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 540.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { StatusBadges(detail.item) }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onUsedToday, label = { Text(stringResource(R.string.used_today)) })
                        AssistChip(
                            onClick = if (detail.item.inBasket) onRemoveFromBasket else onAddToBasket,
                            label = {
                                Text(
                                    stringResource(
                                        if (detail.item.inBasket) R.string.remove_from_basket
                                        else R.string.add_to_basket,
                                    ),
                                )
                            },
                        )
                        AssistChip(onClick = onMarkWashed, label = { Text(stringResource(R.string.mark_washed)) })
                    }
                }
                item { Text(stringResource(R.string.add_custom_usage), style = MaterialTheme.typography.titleSmall) }
                item {
                    FormTextField(
                        usageDate,
                        { usageDate = it },
                        stringResource(R.string.usage_date),
                    )
                }
                item {
                    FormTextField(
                        usageNotes,
                        { usageNotes = it },
                        stringResource(R.string.notes),
                        singleLine = false,
                    )
                }
                item {
                    Button(
                        onClick = {
                            onRecordUsage(usageDate, usageNotes)
                            usageNotes = ""
                        },
                        enabled = runCatching { LocalDate.parse(usageDate) }.isSuccess,
                    ) { Text(stringResource(R.string.record_usage)) }
                }
                item { Text(stringResource(R.string.usage_history), style = MaterialTheme.typography.titleSmall) }
                if (detail.usageHistory.isEmpty()) item { Text(stringResource(R.string.no_usage_history)) }
                items(detail.usageHistory, key = { "usage-${it.id}" }) { event ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(event.notes?.let { "${event.usedAt} · $it" } ?: event.usedAt)
                        TextButton(onClick = { onDeleteUsage(event.id) }) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
                item { Text(stringResource(R.string.wash_history), style = MaterialTheme.typography.titleSmall) }
                if (detail.washHistory.isEmpty()) item { Text(stringResource(R.string.no_wash_history)) }
                items(detail.washHistory, key = { "wash-${it.id}" }) { event ->
                    val outOfCycleLabel = stringResource(R.string.out_of_cycle)
                    Text(
                        buildString {
                            append(event.washedAt)
                            if (event.wasOutOfCycle) append(" · $outOfCycleLabel")
                            event.comment?.let { append(" · $it") }
                        },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.edit_item)) }
                TextButton(onClick = onArchive) { Text(stringResource(R.string.archive)) }
            }
        },
    )
}

@Composable
private fun WashDialog(
    form: WashFormState,
    onChange: (WashFormState) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val validDate = runCatching { LocalDate.parse(form.washedAt) }.isSuccess
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mark_washed)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.items_selected, form.itemIds.size))
                FormTextField(
                    form.washedAt,
                    { onChange(form.copy(washedAt = it)) },
                    stringResource(R.string.wash_date),
                )
                FormTextField(
                    form.comment,
                    { onChange(form.copy(comment = it)) },
                    stringResource(R.string.wash_comment),
                    singleLine = false,
                )
                if (form.hasDateConflict) {
                    Text(
                        stringResource(R.string.wash_date_conflict),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = validDate, modifier = Modifier.testTag("confirm-wash")) {
                Text(stringResource(R.string.mark_washed))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun EmptyMessage(@StringRes title: Int, @StringRes body: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(body), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun readinessText(reason: WashingReadinessReason?): String = stringResource(
    when (reason) {
        WashingReadinessReason.Usage -> R.string.readiness_usage
        WashingReadinessReason.Date -> R.string.readiness_date
        WashingReadinessReason.UsageAndDate -> R.string.readiness_usage_and_date
        null -> R.string.needs_washing
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    selectedCategoryId: Long?,
    selectedName: String,
    categories: List<CategoryEntity>,
    onSelected: (CategoryEntity, String) -> Unit,
    onManageCategories: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(selectedCategoryId, selectedName) { mutableStateOf(selectedName) }
    val namedCategories = categories.map { category -> category to categoryDisplayName(category) }
    val filtered = namedCategories.filter { (_, name) ->
        query.isBlank() || name.contains(query, ignoreCase = true) || name == selectedName
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    expanded = true
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text(stringResource(R.string.category)) },
                placeholder = { Text(stringResource(R.string.select_category)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filtered.forEach { (category, name) ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(name)
                                Text(
                                    categoryRuleText(category),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            query = name
                            expanded = false
                            onSelected(category, name)
                        },
                    )
                }
                if (filtered.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.no_categories_found)) },
                        onClick = {},
                        enabled = false,
                    )
                }
            }
        }
        TextButton(onClick = onManageCategories) {
            Text(stringResource(R.string.manage_categories))
        }
    }
}

@Composable
private fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    state: CategoryManagerUiState,
    onSearchChange: (String) -> Unit,
    onNew: () -> Unit,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onFormChange: (CategoryFormState) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag("category-manager"),
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (state.form == null) R.string.categories_title else R.string.edit_category,
                ),
            )
        },
        text = {
            state.form?.let { form ->
                CategoryForm(
                    form = form,
                    hasSaveError = state.hasSaveError,
                    onChange = onFormChange,
                )
            } ?: run {
                val namedCategories = categories.map { category -> category to categoryDisplayName(category) }
                val filtered = namedCategories.filter { (_, name) ->
                    name.contains(state.searchQuery, ignoreCase = true)
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.search_categories)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                    )
                    Button(onClick = onNew) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.new_category))
                    }
                    if (state.hasDeleteError) {
                        Text(
                            stringResource(R.string.category_delete_error),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.first.id }) { (category, name) ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            categoryRuleText(category),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        if (category.isPredefined) {
                                            Text(
                                                stringResource(R.string.predefined_category),
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onEdit(category) }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_category))
                                    }
                                    if (!category.isPredefined) {
                                        IconButton(onClick = { onDelete(category.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_category))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (state.form == null) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            } else {
                TextButton(onClick = onSave) { Text(stringResource(R.string.save)) }
            }
        },
        dismissButton = {
            if (state.form != null) {
                TextButton(onClick = onCancelEdit) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun CategoryForm(
    form: CategoryFormState,
    hasSaveError: Boolean,
    onChange: (CategoryFormState) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = if (form.isPredefined) categorySystemName(form.systemKey) else form.name,
            onValueChange = { onChange(form.copy(name = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.category_name)) },
            readOnly = form.isPredefined,
            singleLine = true,
        )
        Text(stringResource(R.string.category_default_rule), style = MaterialTheme.typography.labelLarge)
        WashingCriteriaChips(form.washingCriteriaType) {
            onChange(form.copy(washingCriteriaType = it))
        }
        CategoryThresholdFields(form, onChange)
        Text(
            stringResource(R.string.category_rule_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasSaveError) {
            Text(stringResource(R.string.category_duplicate_error), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun CategoryThresholdFields(
    form: CategoryFormState,
    onChange: (CategoryFormState) -> Unit,
) {
    when (form.washingCriteriaType) {
        WashingCriteriaType.ByUsage -> FormTextField(
            form.washingUsageThreshold,
            { onChange(form.copy(washingUsageThreshold = it)) },
            stringResource(R.string.usage_threshold),
            keyboardType = KeyboardType.Number,
        )
        WashingCriteriaType.ByDate -> FormTextField(
            form.washingDayThreshold,
            { onChange(form.copy(washingDayThreshold = it)) },
            stringResource(R.string.day_threshold),
            keyboardType = KeyboardType.Number,
        )
        WashingCriteriaType.ByUsageOrDate -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormTextField(
                form.washingUsageThreshold,
                { onChange(form.copy(washingUsageThreshold = it)) },
                stringResource(R.string.usage_threshold),
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
            )
            FormTextField(
                form.washingDayThreshold,
                { onChange(form.copy(washingDayThreshold = it)) },
                stringResource(R.string.day_threshold),
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
            )
        }
        WashingCriteriaType.Manual -> Text(stringResource(R.string.manual_wash_hint))
    }
}

@Composable
private fun categoryRuleText(category: CategoryEntity): String {
    val type = runCatching { WashingCriteriaType.valueOf(category.washingCriteriaType) }
        .getOrDefault(WashingCriteriaType.Manual)
    return when (type) {
        WashingCriteriaType.ByUsage ->
            "${stringResource(R.string.criteria_by_usage)} · ${category.washingUsageThreshold ?: 0}"
        WashingCriteriaType.ByDate ->
            "${stringResource(R.string.criteria_by_date)} · ${category.washingDayThreshold ?: 0}"
        WashingCriteriaType.ByUsageOrDate ->
            "${stringResource(R.string.criteria_by_usage_or_date)} · " +
                "${category.washingUsageThreshold ?: 0}/${category.washingDayThreshold ?: 0}"
        WashingCriteriaType.Manual -> stringResource(R.string.criteria_manual)
    }
}

@Composable
private fun categoryDisplayName(category: CategoryEntity): String =
    category.customName ?: categorySystemName(category.systemKey)

@Composable
private fun categorySystemName(systemKey: String?): String = stringResource(
    when (systemKey) {
        "tops" -> R.string.category_tops
        "bottoms" -> R.string.category_bottoms
        "underwear" -> R.string.category_underwear
        "socks" -> R.string.category_socks
        "activewear" -> R.string.category_activewear
        "sleepwear" -> R.string.category_sleepwear
        "dresses" -> R.string.category_dresses
        "outerwear" -> R.string.category_outerwear
        "bedding" -> R.string.category_bedding
        "towels" -> R.string.category_towels
        "curtains" -> R.string.category_curtains
        else -> R.string.category_other
    },
)

@Composable
private fun ItemEditorDialog(
    form: ItemFormState,
    categories: List<CategoryEntity>,
    onFormChange: (ItemFormState) -> Unit,
    onCategorySelected: (CategoryEntity, String) -> Unit,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val validRule = when (form.washingCriteriaType) {
        WashingCriteriaType.ByUsage -> form.washingUsageThreshold.toIntOrNull()?.let { it > 0 } == true
        WashingCriteriaType.ByDate -> form.washingDayThreshold.toIntOrNull()?.let { it > 0 } == true
        WashingCriteriaType.ByUsageOrDate ->
            form.washingUsageThreshold.toIntOrNull()?.let { it > 0 } == true &&
                form.washingDayThreshold.toIntOrNull()?.let { it > 0 } == true
        WashingCriteriaType.Manual -> true
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (form.id == 0L) R.string.add_item else R.string.edit_item)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    FormTextField(
                        form.name,
                        { onFormChange(form.copy(name = it)) },
                        stringResource(R.string.item_name),
                        testTag = "item-name",
                    )
                }
                item {
                    CategorySelector(
                        selectedCategoryId = form.categoryId,
                        selectedName = form.category,
                        categories = categories,
                        onSelected = onCategorySelected,
                        onManageCategories = onManageCategories,
                    )
                }
                if (form.id == 0L) {
                    item {
                        FormTextField(
                            form.initialUsageCount,
                            { onFormChange(form.copy(initialUsageCount = it)) },
                            stringResource(R.string.initial_usage_count),
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }
                item { Text(stringResource(R.string.washing_criteria), style = MaterialTheme.typography.labelLarge) }
                item {
                    WashingCriteriaChips(form.washingCriteriaType) {
                        onFormChange(form.copy(washingCriteriaType = it))
                    }
                }
                item { WashingThresholdFields(form, onFormChange) }
                item {
                    TextButton(onClick = { onFormChange(form.copy(showAdvancedDetails = !form.showAdvancedDetails)) }) {
                        Text(stringResource(if (form.showAdvancedDetails) R.string.less_details else R.string.more_details))
                    }
                }
                if (form.showAdvancedDetails) item { AdvancedItemFields(form, onFormChange) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = form.name.isNotBlank() && validRule,
                modifier = Modifier.testTag("save-item"),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun WashingThresholdFields(form: ItemFormState, onFormChange: (ItemFormState) -> Unit) {
    when (form.washingCriteriaType) {
        WashingCriteriaType.ByUsage -> FormTextField(
            form.washingUsageThreshold,
            { onFormChange(form.copy(washingUsageThreshold = it)) },
            stringResource(R.string.usage_threshold),
            keyboardType = KeyboardType.Number,
            testTag = "usage-threshold",
        )
        WashingCriteriaType.ByDate -> FormTextField(
            form.washingDayThreshold,
            { onFormChange(form.copy(washingDayThreshold = it)) },
            stringResource(R.string.day_threshold),
            keyboardType = KeyboardType.Number,
        )
        WashingCriteriaType.ByUsageOrDate -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormTextField(
                form.washingUsageThreshold,
                { onFormChange(form.copy(washingUsageThreshold = it)) },
                stringResource(R.string.usage_threshold),
                Modifier.weight(1f),
                KeyboardType.Number,
            )
            FormTextField(
                form.washingDayThreshold,
                { onFormChange(form.copy(washingDayThreshold = it)) },
                stringResource(R.string.day_threshold),
                Modifier.weight(1f),
                KeyboardType.Number,
            )
        }
        WashingCriteriaType.Manual -> Text(stringResource(R.string.manual_wash_hint))
    }
}

@Composable
private fun AdvancedItemFields(form: ItemFormState, onFormChange: (ItemFormState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormTextField(form.color, { onFormChange(form.copy(color = it)) }, stringResource(R.string.color))
        FormTextField(form.brand, { onFormChange(form.copy(brand = it)) }, stringResource(R.string.brand))
        FormTextField(form.fabric, { onFormChange(form.copy(fabric = it)) }, stringResource(R.string.fabric))
        FormTextField(form.season, { onFormChange(form.copy(season = it)) }, stringResource(R.string.season))
        FormTextField(form.photoUri, { onFormChange(form.copy(photoUri = it)) }, stringResource(R.string.photo_uri))
        FormTextField(form.purchaseDate, { onFormChange(form.copy(purchaseDate = it)) }, stringResource(R.string.purchase_date))
        FormTextField(
            form.purchasePrice,
            { onFormChange(form.copy(purchasePrice = it)) },
            stringResource(R.string.purchase_price),
            keyboardType = KeyboardType.Decimal,
        )
        FormTextField(
            form.description,
            { onFormChange(form.copy(description = it)) },
            stringResource(R.string.description),
            singleLine = false,
        )
        if (form.id == 0L) {
            FormTextField(
                form.initialWashingCount,
                { onFormChange(form.copy(initialWashingCount = it)) },
                stringResource(R.string.initial_washing_count),
                keyboardType = KeyboardType.Number,
            )
            FormTextField(
                form.lastWashingDate,
                { onFormChange(form.copy(lastWashingDate = it)) },
                stringResource(R.string.last_washing_date),
            )
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    testTag: String? = null,
) {
    val taggedModifier = if (testTag == null) modifier else modifier.testTag(testTag)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = taggedModifier.fillMaxWidth(),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WashingCriteriaChips(
    selected: WashingCriteriaType,
    onSelected: (WashingCriteriaType) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WashingCriteriaType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text(stringResource(type.labelRes)) },
            )
        }
    }
}

private val WashableItemStatus.labelRes: Int
    @StringRes get() = when (this) {
        WashableItemStatus.Clean -> R.string.clean
        WashableItemStatus.Worn -> R.string.worn
        WashableItemStatus.Archived -> R.string.archived
    }

private val WashingCriteriaType.labelRes: Int
    @StringRes get() = when (this) {
        WashingCriteriaType.ByUsage -> R.string.criteria_by_usage
        WashingCriteriaType.ByDate -> R.string.criteria_by_date
        WashingCriteriaType.ByUsageOrDate -> R.string.criteria_by_usage_or_date
        WashingCriteriaType.Manual -> R.string.criteria_manual
    }
