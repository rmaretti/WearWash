package com.wearwash.app.ui.screens.items

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wearwash.app.R
import com.wearwash.app.data.ItemRepository
import com.wearwash.app.domain.logic.WashingReadinessReason
import com.wearwash.app.domain.model.WashableItemStatus
import com.wearwash.app.domain.model.WashingCriteriaType
import java.time.LocalDate

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

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header(onAdd = viewModel::openNewItemEditor)
            DestinationSwitcher(uiState.destination, viewModel::showItems, viewModel::showBasket)
            when (uiState.destination) {
                MainDestination.Items -> ItemsContent(uiState, viewModel)
                MainDestination.Basket -> BasketContent(uiState, viewModel)
            }
        }
    }

    if (uiState.isEditorOpen) {
        ItemEditorDialog(
            form = uiState.form,
            onFormChange = viewModel::updateForm,
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
}

@Composable
private fun Header(onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onAdd, modifier = Modifier.testTag("add-item")) {
            Text(stringResource(R.string.add_item))
        }
    }
}

@Composable
private fun DestinationSwitcher(
    destination: MainDestination,
    onItems: () -> Unit,
    onBasket: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = destination == MainDestination.Items,
            onClick = onItems,
            label = { Text(stringResource(R.string.items_title)) },
        )
        FilterChip(
            selected = destination == MainDestination.Basket,
            onClick = onBasket,
            modifier = Modifier.testTag("basket-tab"),
            label = { Text(stringResource(R.string.laundry_basket_title)) },
        )
    }
}

@Composable
private fun ItemsContent(uiState: ItemsUiState, viewModel: ItemsViewModel) {
    OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = viewModel::updateSearchQuery,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(R.string.search_items)) },
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
    var selectedIds by remember(uiState.basketItems) { mutableStateOf(emptySet<Long>()) }
    val allIds = uiState.basketItems.mapTo(mutableSetOf()) { it.id }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { viewModel.openWashDialog(selectedIds) },
            enabled = selectedIds.isNotEmpty(),
            modifier = Modifier.testTag("wash-selected"),
        ) {
            Text(stringResource(R.string.wash_selected))
        }
        OutlinedButton(
            onClick = { viewModel.openWashDialog(allIds) },
            enabled = allIds.isNotEmpty(),
        ) {
            Text(stringResource(R.string.wash_all))
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(stringResource(R.string.in_basket_section), style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.basketItems.isEmpty()) {
            item { Text(stringResource(R.string.basket_empty)) }
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
        item {
            Text(
                stringResource(R.string.suggested_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (uiState.suggestedItems.isEmpty()) {
            item { Text(stringResource(R.string.no_suggestions)) }
        }
        items(uiState.suggestedItems, key = { "suggested-${it.id}" }) { item ->
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                        Text(readinessText(item.readinessReason))
                    }
                    Button(onClick = { viewModel.addToBasket(item.id, "automatic_readiness") }) {
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
    Card(modifier = Modifier.testTag("item-${item.id}")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(item.brand?.let { "${item.category} · $it" } ?: item.category)
                    Text(stringResource(R.string.uses_since_wash, item.usesSinceWash))
                }
                StatusBadges(item)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onUsed, label = { Text(stringResource(R.string.used_today)) })
                AssistChip(
                    onClick = onBasket,
                    label = {
                        Text(
                            stringResource(
                                if (item.inBasket) R.string.remove_from_basket
                                else R.string.add_to_basket,
                            ),
                        )
                    },
                )
                TextButton(onClick = onOpen) { Text(stringResource(R.string.details)) }
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
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = onSelected,
                modifier = Modifier.testTag("basket-select-${item.id}"),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.uses_since_wash, item.usesSinceWash))
            }
            TextButton(onClick = onOpen) { Text(stringResource(R.string.details)) }
            TextButton(onClick = onRemove) { Text(stringResource(R.string.remove)) }
        }
    }
}

@Composable
private fun StatusBadges(item: ItemUiModel) {
    Column(horizontalAlignment = Alignment.End) {
        Text(stringResource(item.lifecycleStatus.labelRes), style = MaterialTheme.typography.labelMedium)
        if (item.needsWashing) {
            Text(
                stringResource(R.string.needs_washing),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (item.inBasket) Text(stringResource(R.string.in_laundry_basket))
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

@Composable
private fun ItemEditorDialog(
    form: ItemFormState,
    onFormChange: (ItemFormState) -> Unit,
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
                item { FormTextField(form.category, { onFormChange(form.copy(category = it)) }, stringResource(R.string.category)) }
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
