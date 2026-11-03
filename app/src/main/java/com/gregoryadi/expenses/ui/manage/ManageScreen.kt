package com.gregoryadi.expenses.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gregoryadi.expenses.data.db.CategoryEntity
import com.gregoryadi.expenses.data.db.PaymentMethodEntity
import com.gregoryadi.expenses.domain.Kind
import com.gregoryadi.expenses.domain.MethodType
import com.gregoryadi.expenses.domain.Palette
import com.gregoryadi.expenses.ui.appViewModel
import com.gregoryadi.expenses.ui.components.BackHeader
import com.gregoryadi.expenses.ui.components.ColorDot
import com.gregoryadi.expenses.ui.components.Segmented

/** Add, rename, recolour and archive payment methods and categories. Archived ones keep their old expenses. */
@Composable
fun ManageScreen(onBack: () -> Unit) {
    val vm = appViewModel { ManageViewModel(it.lookups) }
    val methods by vm.methods.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(0) }
    var methodDraft by remember { mutableStateOf<PaymentMethodEntity?>(null) }
    var categoryDraft by remember { mutableStateOf<CategoryEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        BackHeader(
            title = "Payment methods & categories",
            onBack = onBack,
            actions = {
                FilledTonalButton(onClick = {
                    if (tab == 0) {
                        methodDraft = PaymentMethodEntity(name = "", type = MethodType.OTHER.key, color = Palette.colors[0])
                    } else {
                        categoryDraft = CategoryEntity(
                            name = "",
                            color = Palette.colors[0],
                            kind = if (tab == 2) Kind.INCOME else Kind.EXPENSE,
                        )
                    }
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("Add", modifier = Modifier.padding(start = 6.dp))
                }
            },
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Segmented(listOf("Methods", "Spending", "Income"), tab, { tab = it })
                if (tab == 0) {
                    methods.forEach { m ->
                        ManageRow(
                            name = m.name,
                            detail = MethodType.fromKey(m.type).label,
                            color = Color(m.color),
                            archived = m.archived,
                            onClick = { methodDraft = m },
                        )
                    }
                } else {
                    val kind = if (tab == 2) Kind.INCOME else Kind.EXPENSE
                    categories.filter { it.kind == kind }.forEach { c ->
                        ManageRow(
                            name = c.name,
                            detail = null,
                            color = Color(c.color),
                            archived = c.archived,
                            onClick = { categoryDraft = c },
                        )
                    }
                }
            }
        }
    }

    methodDraft?.let { draft ->
        EditDialog(
            title = if (draft.id == 0L) "New payment method" else "Edit payment method",
            initialName = draft.name,
            initialColor = draft.color,
            initialArchived = draft.archived,
            canArchive = draft.id != 0L,
            typeOptions = MethodType.entries,
            initialType = MethodType.fromKey(draft.type),
            onDismiss = { methodDraft = null },
            onSave = { name, color, archived, type ->
                vm.saveMethod(draft.copy(name = name, color = color, archived = archived, type = (type ?: MethodType.OTHER).key))
                methodDraft = null
            },
        )
    }

    categoryDraft?.let { draft ->
        EditDialog(
            title = when {
                draft.kind == Kind.INCOME && draft.id == 0L -> "New income source"
                draft.kind == Kind.INCOME -> "Edit income source"
                draft.id == 0L -> "New category"
                else -> "Edit category"
            },
            initialName = draft.name,
            initialColor = draft.color,
            initialArchived = draft.archived,
            canArchive = draft.id != 0L,
            typeOptions = emptyList(),
            initialType = null,
            onDismiss = { categoryDraft = null },
            onSave = { name, color, archived, _ ->
                vm.saveCategory(draft.copy(name = name, color = color, archived = archived))
                categoryDraft = null
            },
        )
    }
}

@Composable
private fun ManageRow(name: String, detail: String?, color: Color, archived: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(if (archived) color.copy(alpha = 0.35f) else color, size = 14.dp)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            val sub = listOfNotNull(detail, if (archived) "Archived" else null).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditDialog(
    title: String,
    initialName: String,
    initialColor: Long,
    initialArchived: Boolean,
    canArchive: Boolean,
    typeOptions: List<MethodType>,
    initialType: MethodType?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Long, archived: Boolean, type: MethodType?) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }
    var archived by remember { mutableStateOf(initialArchived) }
    var type by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (typeOptions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            typeOptions.forEach { option ->
                                FilterChip(
                                    selected = type == option,
                                    onClick = { type = option },
                                    label = { Text(option.label) },
                                )
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Colour", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Palette.colors.forEach { option ->
                            val selected = option == color
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(option))
                                    .then(
                                        if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { color = option }
                            )
                        }
                    }
                }
                if (canArchive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Archived", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Hidden from pickers; old expenses keep it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = archived, onCheckedChange = { archived = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, color, archived, type) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
