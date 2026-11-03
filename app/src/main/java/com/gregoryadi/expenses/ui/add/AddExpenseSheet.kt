package com.gregoryadi.expenses.ui.add

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import com.gregoryadi.expenses.ui.components.InkChip
import com.gregoryadi.expenses.ui.components.PillButton
import com.gregoryadi.expenses.ui.components.bouncyClick
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gregoryadi.expenses.data.db.CategoryEntity
import com.gregoryadi.expenses.data.db.PaymentMethodEntity
import com.gregoryadi.expenses.domain.Formats
import com.gregoryadi.expenses.domain.Kind
import com.gregoryadi.expenses.domain.Money
import com.gregoryadi.expenses.ui.components.LocalSnackbarController
import com.gregoryadi.expenses.ui.components.SectionTitle
import com.gregoryadi.expenses.ui.components.Segmented
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Bottom sheet for adding or editing an expense or an income.
 *
 * The amount always sits directly above the keypad, so it stays visible while typing:
 * - Portrait: the details (method, category, date, note) scroll at the top; amount, keypad and Save stay pinned below.
 * - Landscape (either screen of the Fold): details on the left, amount + keypad + Save on the right.
 *
 * The draft lives in [AddExpenseViewModel], so it survives folding, unfolding and rotating.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(vm: AddExpenseViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val allMethods by vm.methods.collectAsStateWithLifecycle()
    val allCategories by vm.categories.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarController.current
    val haptic = LocalHapticFeedback.current
    var showDatePicker by remember { mutableStateOf(false) }

    val config = LocalConfiguration.current
    val landscape = config.screenWidthDp > config.screenHeightDp

    val methods = allMethods.filter { !it.archived || it.id == state.methodId }
    val categories = allCategories.filter { it.kind == state.kind && (!it.archived || it.id == state.categoryId) }
    val noun = if (state.isIncome) "Income" else "Expense"

    val onSave: () -> Unit = {
        vm.save {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            snackbar.show(if (state.editingId != null) "Changes saved" else "$noun saved")
        }
    }
    val onDelete: () -> Unit = {
        vm.deleteEditing { deleted ->
            snackbar.show("$noun deleted", "Undo") { vm.restore(deleted) }
        }
    }

    val header: @Composable () -> Unit = {
        SheetHeader(state = state, onKind = vm::setKind, onDelete = onDelete)
    }
    val details: @Composable () -> Unit = {
        Details(
            state = state,
            methods = methods,
            categories = categories,
            onMethod = vm::setMethod,
            onCategory = vm::setCategory,
            onPickDate = { showDatePicker = true },
            onNote = vm::setNote,
        )
    }

    // The drag handle costs ~40 dp of height, which landscape on the cover screen can't spare.
    val dragHandle: (@Composable () -> Unit)? = if (landscape) null else StandardDragHandle

    ModalBottomSheet(
        onDismissRequest = vm::dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetMaxWidth = if (landscape) 880.dp else BottomSheetDefaults.SheetMaxWidth,
        dragHandle = dragHandle,
    ) {
        if (landscape) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    header()
                    details()
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AmountDisplay(state)
                    Keypad(keyHeight = 44.dp, onKey = vm::key, onBackspace = vm::backspace)
                    SaveButton(state, onSave)
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                header()
                // Only this part scrolls; the amount, keypad and Save button below it never leave the screen.
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    details()
                }
                AmountDisplay(state)
                Keypad(keyHeight = 52.dp, onKey = vm::key, onBackspace = vm::backspace)
                SaveButton(state, onSave)
            }
        }
    }

    if (showDatePicker) {
        // The picker works in UTC midnights, so convert the local date to and from UTC.
        val initial = Formats.localDate(state.occurredAt).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        vm.setDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun SheetHeader(state: AddState, onKind: (String) -> Unit, onDelete: () -> Unit) {
    val editing = state.editingId != null
    val noun = if (state.isIncome) "income" else "expense"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (editing) "Edit $noun" else "New $noun",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (editing) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete $noun",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Segmented(
            options = listOf("Expense", "Income"),
            selectedIndex = if (state.isIncome) 1 else 0,
            onSelect = { onKind(if (it == 1) Kind.INCOME else Kind.EXPENSE) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Details(
    state: AddState,
    methods: List<PaymentMethodEntity>,
    categories: List<CategoryEntity>,
    onMethod: (Long) -> Unit,
    onCategory: (Long) -> Unit,
    onPickDate: () -> Unit,
    onNote: (String) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(if (state.isIncome) "Received in" else "Paid with")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                methods.forEach { method ->
                    InkChip(
                        label = method.name,
                        selected = state.methodId == method.id,
                        onClick = { onMethod(method.id) },
                        dot = Color(method.color),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(if (state.isIncome) "Source" else "Category")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    InkChip(
                        label = category.name,
                        selected = state.categoryId == category.id,
                        onClick = { onCategory(category.id) },
                        selectedColor = MaterialTheme.colorScheme.onSurface,
                        dot = Color(category.color),
                    )
                }
            }
            if (categories.isEmpty()) {
                Text(
                    "No ${if (state.isIncome) "income sources" else "categories"} yet. Add one in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            Modifier
                .height(44.dp)
                .clip(CircleShape)
                .bouncyClick(onClick = onPickDate)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(Formats.chipLabel(state.occurredAt, LocalDate.now(zone)), style = MaterialTheme.typography.labelLarge)
        }

        TextField(
            value = state.note,
            onValueChange = onNote,
            placeholder = { Text("Add a note") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            shape = RoundedCornerShape(22.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The amount above the keypad. Each key press rolls the number: new digits slide up into place,
 * deleting slides them back down. Income shows in lime with a "+".
 */
@Composable
private fun AmountDisplay(state: AddState) {
    val color by animateColorAsState(
        when {
            state.amount == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
            state.isIncome -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "amountColor",
    )
    AnimatedContent(
        targetState = state.digits,
        transitionSpec = {
            val growing = targetState.length >= initialState.length
            val inFrom = if (growing) 1 else -1
            (slideInVertically(tween(180)) { inFrom * it / 2 } + fadeIn(tween(180))) togetherWith
                (slideOutVertically(tween(150)) { -inFrom * it / 2 } + fadeOut(tween(120))) using
                SizeTransform(clip = false)
        },
        label = "amount",
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) { digits ->
        val amount = digits.toLongOrNull() ?: 0L
        Text(
            text = if (state.isIncome) Money.incoming(amount) else Money.format(amount),
            style = MaterialTheme.typography.displaySmall,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SaveButton(state: AddState, onSave: () -> Unit) {
    PillButton(
        text = when {
            state.editingId != null -> "Save changes"
            state.isIncome -> "Save income"
            else -> "Save expense"
        },
        onClick = onSave,
        enabled = state.canSave,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
private val StandardDragHandle: @Composable () -> Unit = { BottomSheetDefaults.DragHandle() }

private val keypadRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("000", "0", "⌫"),
)

/** Number pad. Keys spring under the finger; one tap enters one key. */
@Composable
private fun Keypad(keyHeight: Dp, onKey: (String) -> Unit, onBackspace: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keypadRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .bouncyClick(pressedScale = 0.9f) { if (key == "⌫") onBackspace() else onKey(key) }
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = key,
                            style = if (key.length > 1) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            modifier = if (key == "⌫") Modifier.semantics { contentDescription = "Delete digit" } else Modifier,
                        )
                    }
                }
            }
        }
    }
}
