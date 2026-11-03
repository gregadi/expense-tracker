package com.gregoryadi.expenses.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.gregoryadi.expenses.ui.components.AnimatedAmount
import com.gregoryadi.expenses.ui.components.InkChip
import com.gregoryadi.expenses.ui.components.PillButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gregoryadi.expenses.data.db.ExpenseWithDetails
import com.gregoryadi.expenses.domain.Formats
import com.gregoryadi.expenses.domain.Kind
import com.gregoryadi.expenses.domain.Money
import com.gregoryadi.expenses.ui.appViewModel
import com.gregoryadi.expenses.ui.components.ColorDot
import com.gregoryadi.expenses.ui.components.ExpenseRow
import com.gregoryadi.expenses.ui.components.FlatCard
import com.gregoryadi.expenses.ui.components.LocalSnackbarController
import com.gregoryadi.expenses.ui.components.SectionTitle
import com.gregoryadi.expenses.ui.components.Segmented
import java.time.LocalDate

/**
 * History: every expense grouped by day, with search and filters; swipe left to delete (with undo).
 * Unfolded (wide): the list sits on the left and the selected expense's details on the right.
 */
@Composable
fun HistoryScreen(isWide: Boolean, onEdit: (Long) -> Unit) {
    val vm = appViewModel { HistoryViewModel(it.expenses, it.lookups) }
    val items by vm.items.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val methods by vm.methods.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarController.current

    val onDelete: (ExpenseWithDetails) -> Unit = { item ->
        vm.delete(item)
        snackbar.show("Expense deleted", "Undo") { vm.restore(item) }
    }

    val list: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            Text(
                "History",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            SearchField(filter.query, vm::setQuery, Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            Segmented(
                options = listOf("All", "Expenses", "Income"),
                selectedIndex = when (filter.kind) {
                    Kind.EXPENSE -> 1
                    Kind.INCOME -> 2
                    else -> 0
                },
                onSelect = { index ->
                    vm.setKind(
                        when (index) {
                            1 -> Kind.EXPENSE
                            2 -> Kind.INCOME
                            else -> null
                        }
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(methods, key = { it.id }) { method ->
                    InkChip(
                        label = method.name,
                        selected = filter.methodId == method.id,
                        onClick = { vm.toggleMethod(method.id) },
                        dot = Color(method.color),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories.filter { filter.kind == null || it.kind == filter.kind }, key = { it.id }) { category ->
                    InkChip(
                        label = category.name,
                        selected = filter.categoryId == category.id,
                        onClick = { vm.toggleCategory(category.id) },
                        selectedColor = MaterialTheme.colorScheme.onSurface,
                        dot = Color(category.color),
                    )
                }
            }
            ExpenseList(
                items = items,
                selectedId = if (isWide) selected?.expense?.id else null,
                onClick = { item -> if (isWide) vm.select(item.expense.id) else onEdit(item.expense.id) },
                onDelete = onDelete,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (isWide) {
        Row(Modifier.fillMaxSize()) {
            list(Modifier.weight(1f).fillMaxSize())
            Box(Modifier.weight(1f).fillMaxSize().padding(24.dp)) {
                DetailPane(
                    item = selected,
                    onEdit = { id -> onEdit(id) },
                    onDelete = { item ->
                        onDelete(item)
                        vm.select(null)
                    },
                )
            }
        }
    } else {
        list(Modifier.fillMaxSize())
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Search notes, categories, amounts") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseList(
    items: List<ExpenseWithDetails>,
    selectedId: Long?,
    onClick: (ExpenseWithDetails) -> Unit,
    onDelete: (ExpenseWithDetails) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    // The query already sorts newest first, and groupBy keeps that order.
    val grouped = remember(items) { items.groupBy { Formats.localDate(it.expense.occurredAt) }.toList() }

    if (items.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                "Nothing matches.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 128.dp),
    ) {
        grouped.forEach { (date, dayItems) ->
            item(key = "day-$date") {
                Row(
                    Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle(Formats.dayLabel(date, today), Modifier.weight(1f))
                    Text(
                        dayTotals(dayItems),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(dayItems, key = { it.expense.id }) { item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDelete(item)
                            true
                        } else {
                            false
                        }
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    // New entries fade in, deleted ones collapse, and the rest slide into place.
                    modifier = Modifier.animateItem(),
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(end = 24.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                ) {
                    ExpenseRow(
                        item = item,
                        onClick = { onClick(item) },
                        selected = item.expense.id == selectedId,
                        background = MaterialTheme.colorScheme.background,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailPane(
    item: ExpenseWithDetails?,
    onEdit: (Long) -> Unit,
    onDelete: (ExpenseWithDetails) -> Unit,
) {
    if (item == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Select an expense to see its details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val income = item.expense.kind == Kind.INCOME
    FlatCard {
        SectionTitle(if (income) "Income" else "Expense")
        AnimatedAmount(
            amount = item.expense.amount,
            style = MaterialTheme.typography.displaySmall,
            color = if (income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            format = if (income) Money::incoming else Money::format,
        )
        Spacer(Modifier.height(20.dp))
        DetailLine(if (income) "Source" else "Category", item.category.name, Color(item.category.color))
        DetailLine(if (income) "Received in" else "Paid with", item.method.name, Color(item.method.color))
        DetailLine("Date", Formats.dateTime(item.expense.occurredAt))
        if (item.expense.note.isNotBlank()) DetailLine("Note", item.expense.note)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(text = "Edit", onClick = { onEdit(item.expense.id) }, height = 48.dp)
            TextButton(onClick = { onDelete(item) }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, dot: Color? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Row(Modifier.weight(0.6f), verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                ColorDot(dot)
                Spacer(Modifier.padding(start = 8.dp))
            }
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Day header text: "+Rp 5.000.000 · Rp 120.000" (money in, then money out), skipping a side that is zero. */
private fun dayTotals(items: List<ExpenseWithDetails>): String {
    val received = items.filter { it.expense.kind == Kind.INCOME }.sumOf { it.expense.amount }
    val spent = items.filter { it.expense.kind != Kind.INCOME }.sumOf { it.expense.amount }
    return listOfNotNull(
        if (received > 0) Money.incoming(received) else null,
        if (spent > 0 || received == 0L) Money.format(spent) else null,
    ).joinToString(" · ")
}
