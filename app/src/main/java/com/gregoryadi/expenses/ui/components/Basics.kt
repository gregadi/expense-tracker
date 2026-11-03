package com.gregoryadi.expenses.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gregoryadi.expenses.data.db.ExpenseWithDetails
import com.gregoryadi.expenses.domain.Formats
import com.gregoryadi.expenses.domain.Kind
import com.gregoryadi.expenses.domain.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Shows snackbars (with an optional action) from anywhere in the UI. */
class SnackbarController(
    private val host: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    fun show(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

val LocalSnackbarController = compositionLocalOf<SnackbarController> {
    error("SnackbarController not provided")
}

@Composable
fun ColorDot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Ink card: one step lighter than the ground, no border, no shadow, soft corners. */
@Composable
fun FlatCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Pill selector (Day / Week / Month, Expense / Income...). The light indicator slides to the
 * chosen option with a small spring, and the labels cross-fade their colour.
 */
@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        val indicatorX by animateDpAsState(segment * selectedIndex, Motion.bouncy(), label = "segment")
        Box(
            Modifier
                .offset(x = indicatorX)
                .width(segment)
                .height(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface)
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "segmentText",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(CircleShape)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = textColor, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Selectable pill. Selected: filled with [selectedColor] (lime for payment methods, white for categories).
 * Colours animate on change and it springs when tapped.
 */
@Composable
fun InkChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    dot: Color? = null,
) {
    val container by animateColorAsState(if (selected) selectedColor else Color.Transparent, label = "chip")
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
        label = "chipText",
    )
    val border by animateColorAsState(
        if (selected) selectedColor else MaterialTheme.colorScheme.outlineVariant,
        label = "chipBorder",
    )
    Row(
        modifier
            .height(38.dp)
            .clip(CircleShape)
            .bouncyClick(onClick = onClick)
            .background(container)
            .border(BorderStroke(1.dp, border), CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dot != null && !selected) ColorDot(dot)
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

/** The lime action button ("Add", "Save expense"). Dims when disabled, springs when tapped. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    leading: (@Composable () -> Unit)? = null,
) {
    val container by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "pill",
    )
    val content by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pillText",
    )
    Row(
        modifier
            .height(height)
            .clip(CircleShape)
            .bouncyClick(enabled = enabled, onClick = onClick)
            .background(container)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (leading != null) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides content,
            ) { leading() }
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = content, maxLines = 1)
    }
}

/** Round 44 dp icon button on a card-coloured disc (settings, back). The icon carries the description. */
@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .bouncyClick(pressedScale = 0.9f, onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        ) { icon() }
    }
}

@Composable
fun BackHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

/** One entry in a list: a rounded tile with the category initial, title, "method · time" and the amount. */
@Composable
fun ExpenseRow(
    item: ExpenseWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    background: Color = Color.Transparent,
) {
    val e = item.expense
    val income = e.kind == Kind.INCOME
    val title = e.note.ifBlank { item.category.name }
    val subtitle = buildString {
        if (e.note.isNotBlank()) append(item.category.name).append(" · ")
        append(item.method.name).append(" · ").append(Formats.time(e.occurredAt))
    }
    val rowColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else background,
        label = "rowSelected",
    )
    val categoryColor = Color(item.category.color)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(rowColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (income) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.category.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = if (income) MaterialTheme.colorScheme.primary else categoryColor,
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = if (income) Money.incoming(e.amount) else Money.format(e.amount),
            style = MaterialTheme.typography.titleMedium,
            color = if (income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** "Received / Spent / Saved" line: label on the left, amount on the right. */
@Composable
fun AmountLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    valueColor: Color = Color.Unspecified,
) {
    Row(modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = valueColor,
        )
    }
}

/** Same as the text version, but the amount counts up and rolls when it changes. */
@Composable
fun AmountLine(
    label: String,
    amount: Long,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    valueColor: Color = Color.Unspecified,
    format: (Long) -> String = Money::format,
) {
    Row(modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        AnimatedAmount(
            amount = amount,
            style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = valueColor,
            format = format,
        )
    }
}

/** Lime for money saved, coral for overspending; fades between them. */
@Composable
fun savedColor(saved: Long): Color {
    val target = when {
        saved > 0 -> MaterialTheme.colorScheme.primary
        saved < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val color by animateColorAsState(target, label = "saved")
    return color
}
