package com.gregoryadi.expenses.ui.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.gregoryadi.expenses.ui.components.AnimatedAmount
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gregoryadi.expenses.data.db.GroupTotal
import com.gregoryadi.expenses.domain.Money
import com.gregoryadi.expenses.domain.PeriodType
import com.gregoryadi.expenses.domain.Savings
import com.gregoryadi.expenses.ui.appViewModel
import com.gregoryadi.expenses.ui.components.AmountLine
import com.gregoryadi.expenses.ui.components.BarChart
import com.gregoryadi.expenses.ui.components.BreakdownList
import com.gregoryadi.expenses.ui.components.DonutChart
import com.gregoryadi.expenses.ui.components.DonutSlice
import com.gregoryadi.expenses.ui.components.FlatCard
import com.gregoryadi.expenses.ui.components.SectionTitle
import com.gregoryadi.expenses.ui.components.Segmented
import com.gregoryadi.expenses.ui.components.savedColor
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Insights: spending by day / week / month / year, a trend chart, and the payment method and category breakdowns.
 * Unfolded (wide): trend on the left, breakdowns on the right.
 */
@Composable
fun InsightsScreen(isWide: Boolean) {
    val vm = appViewModel { InsightsViewModel(it.expenses) }
    val s by vm.state.collectAsStateWithLifecycle()

    val controls: @Composable () -> Unit = {
        Text("Insights", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 20.dp))
        Segmented(
            options = PeriodType.entries.map { it.label },
            selectedIndex = s.period.type.ordinal,
            onSelect = { vm.setType(PeriodType.entries[it]) },
        )
        PeriodNavigator(s, vm)
    }

    if (isWide) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                controls()
                TotalBlock(s)
                InOutCard(s)
                TrendCard(s)
            }
            Column(
                Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BreakdownCard("Spending by payment method", s.byMethod, s.total)
                BreakdownCard("Spending by category", s.byCategory, s.total)
                if (s.incomeBySource.isNotEmpty()) {
                    BreakdownCard("Income by source", s.incomeBySource, s.income)
                }
            }
        }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            controls()
            TotalBlock(s)
            InOutCard(s)
            TrendCard(s)
            BreakdownCard("Spending by payment method", s.byMethod, s.total)
            BreakdownCard("Spending by category", s.byCategory, s.total)
            if (s.incomeBySource.isNotEmpty()) {
                BreakdownCard("Income by source", s.incomeBySource, s.income)
            }
        }
    }
}

@Composable
private fun PeriodNavigator(s: InsightsState, vm: InsightsViewModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = vm::previous) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous ${s.period.unitName}")
        }
        // The label slides left when going back in time and right when going forward.
        AnimatedContent(
            targetState = s.period,
            transitionSpec = {
                val forward = targetState.start >= initialState.start
                val dir = if (forward) 1 else -1
                (slideInHorizontally(tween(240)) { dir * it / 3 } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(200)) { -dir * it / 3 } + fadeOut(tween(160)))
            },
            label = "period",
            modifier = Modifier.weight(1f).clickable(onClick = vm::goToToday).padding(vertical = 8.dp),
        ) { period ->
            Text(
                text = period.label(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        IconButton(onClick = vm::next, enabled = s.canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next ${s.period.unitName}")
        }
    }
}

@Composable
private fun TotalBlock(s: InsightsState) {
    Column {
        SectionTitle("Total spent")
        AnimatedAmount(amount = s.total, style = MaterialTheme.typography.displayLarge)
        val comparison = comparisonText(s)
        Text(
            text = comparison,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A percentage is only fair once the period is over; before that, just show the previous total. */
private fun comparisonText(s: InsightsState): String {
    val unit = s.period.unitName
    if (s.previousTotal <= 0L) return "Nothing spent in the previous $unit"
    if (!s.isComplete) return "Previous $unit: ${Money.format(s.previousTotal)}"
    val change = ((s.total - s.previousTotal) * 100.0 / s.previousTotal).roundToInt()
    val direction = when {
        change > 0 -> "up ${abs(change)}%"
        change < 0 -> "down ${abs(change)}%"
        else -> "unchanged"
    }
    return "$direction vs previous $unit (${Money.format(s.previousTotal)})"
}

/** Received, spent and saved for the selected period. */
@Composable
private fun InOutCard(s: InsightsState) {
    FlatCard {
        SectionTitle("Money in and out")
        Spacer(Modifier.height(4.dp))
        AmountLine("Received", s.income, format = Money::incoming)
        AmountLine("Spent", s.total)
        AmountLine(
            label = "Saved",
            amount = s.saved,
            emphasized = true,
            valueColor = savedColor(s.saved),
        )
        Savings.note(s.income, s.total)?.let { note ->
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrendCard(s: InsightsState) {
    FlatCard {
        SectionTitle(
            when (s.period.type) {
                PeriodType.DAY -> "Spending by hour"
                PeriodType.WEEK, PeriodType.MONTH -> "Spending by day"
                PeriodType.YEAR -> "Spending by month"
            }
        )
        Spacer(Modifier.height(16.dp))
        BarChart(s.buckets)
    }
}

@Composable
private fun BreakdownCard(title: String, groups: List<GroupTotal>, total: Long) {
    FlatCard {
        SectionTitle(title)
        Spacer(Modifier.height(12.dp))
        if (groups.isEmpty()) {
            Text(
                "No expenses in this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            DonutChart(
                slices = groups.map { DonutSlice(Color(it.color), it.total) },
                modifier = Modifier.widthIn(max = 180.dp).align(Alignment.CenterHorizontally),
            ) {
                Text(
                    Money.compact(total),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            BreakdownList(groups, total)
        }
    }
}
