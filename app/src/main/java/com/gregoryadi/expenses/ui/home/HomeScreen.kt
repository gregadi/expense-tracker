package com.gregoryadi.expenses.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gregoryadi.expenses.data.db.ExpenseWithDetails
import com.gregoryadi.expenses.domain.Money
import com.gregoryadi.expenses.domain.Savings
import com.gregoryadi.expenses.ui.appViewModel
import com.gregoryadi.expenses.ui.components.AnimatedAmount
import com.gregoryadi.expenses.ui.components.CircleIconButton
import com.gregoryadi.expenses.ui.components.ColorDot
import com.gregoryadi.expenses.ui.components.ExpenseRow
import com.gregoryadi.expenses.ui.components.FlatCard
import com.gregoryadi.expenses.ui.components.SectionTitle
import com.gregoryadi.expenses.ui.components.SegmentBar
import com.gregoryadi.expenses.ui.components.animatedFraction
import com.gregoryadi.expenses.ui.components.savedColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Bottom padding that keeps the last item clear of the floating nav. */
private val NavClearance = 128.dp

/**
 * Home, design "Ink". Folded: one column. Unfolded: today, the month and payment methods on the left,
 * recent entries on the right.
 */
@Composable
fun HomeScreen(
    isWide: Boolean,
    onEdit: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onSeeAll: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it.expenses) }
    val state by vm.state.collectAsStateWithLifecycle()

    if (isWide) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(
                Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = NavClearance),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                TopBar(onOpenSettings)
                Hero(state)
                MonthCard(state)
                PaidWith(state)
            }
            Column(
                Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 76.dp, bottom = NavClearance),
            ) {
                Recent(state.recent, onEdit, onSeeAll)
            }
        }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = NavClearance),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TopBar(onOpenSettings)
            Hero(state)
            MonthCard(state)
            PaidWith(state)
            Recent(state.recent, onEdit, onSeeAll)
        }
    }
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit) {
    val date = remember { DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(LocalDate.now()) }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            date,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        CircleIconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun Hero(state: HomeState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle("Spent today")
        AnimatedAmount(amount = state.today, style = MaterialTheme.typography.displayLarge)
    }
}

/** This month: a bar of money in vs out, then In / Out / Saved. */
@Composable
private fun MonthCard(state: HomeState) {
    val month = remember { LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    val income = state.monthIncome
    val spent = state.monthSpent
    // How much of the bar the spending covers: share of income, or the whole bar once over budget.
    val spentShare = when {
        income > 0 -> (spent.toFloat() / income).coerceIn(0f, 1f)
        spent > 0 -> 1f
        else -> 0f
    }
    val over = spent > income && income > 0
    val grownSpent = animatedFraction(spentShare)

    FlatCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(month, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Savings.shortNote(income, spent)?.let { note ->
                Text(note, style = MaterialTheme.typography.labelLarge, color = savedColor(state.monthSaved))
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(if (income > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(grownSpent.coerceAtLeast(0.001f))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("In", income, Modifier.weight(1f))
            Stat("Out", spent, Modifier.weight(1f))
            Stat("Saved", state.monthSaved, Modifier.weight(1f), color = savedColor(state.monthSaved))
        }
    }
}

@Composable
private fun Stat(label: String, amount: Long, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AnimatedAmount(
            amount = amount,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            format = Money::compact,
        )
    }
}

@Composable
private fun PaidWith(state: HomeState) {
    val total = state.monthSpent
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Paid with this month")
        SegmentBar(state.methodsThisMonth)
        if (state.methodsThisMonth.isEmpty()) {
            Text(
                "Nothing spent yet this month.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.methodsThisMonth.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        pair.forEach { g ->
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                ColorDot(Color(g.color))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    g.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${if (total > 0) (g.total * 100.0 / total).roundToInt() else 0}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Recent(recent: List<ExpenseWithDetails>, onEdit: (Long) -> Unit, onSeeAll: () -> Unit) {
    // animateContentSize: the list grows smoothly when a new entry is saved.
    Column(Modifier.animateContentSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Recent", Modifier.weight(1f))
            TextButton(onClick = onSeeAll) { Text("See all") }
        }
        if (recent.isEmpty()) {
            Text(
                "Nothing logged yet. Tap Add to record an expense or income.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            recent.forEach { item ->
                androidx.compose.runtime.key(item.expense.id) {
                    ExpenseRow(item = item, onClick = { onEdit(item.expense.id) })
                }
            }
        }
    }
}
