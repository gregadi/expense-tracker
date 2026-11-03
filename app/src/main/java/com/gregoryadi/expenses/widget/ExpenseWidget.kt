package com.gregoryadi.expenses.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gregoryadi.expenses.MainActivity
import com.gregoryadi.expenses.R
import com.gregoryadi.expenses.domain.Money
import com.gregoryadi.expenses.domain.Savings
import com.gregoryadi.expenses.ui.theme.Ink

/**
 * The home screen widget. One widget, three layouts picked by its size:
 * small (2x2): today + Add; medium (4x2): adds the month; large (4x3): adds saved + quick-add buttons.
 * The background opacity is set per widget in [WidgetConfigActivity].
 */
class ExpenseWidget : GlanceAppWidget() {

    companion object {
        val SMALL = DpSize(100.dp, 100.dp)
        val MEDIUM = DpSize(220.dp, 100.dp)
        val LARGE = DpSize(220.dp, 180.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        provideContent {
            val prefs = currentState<Preferences>()
            val opacity = prefs[WidgetKeys.OPACITY] ?: WidgetKeys.DEFAULT_OPACITY
            val version = prefs[WidgetKeys.VERSION] ?: 0L
            // Reload the numbers whenever the app bumps the version (after a save, delete or import).
            val data by produceState<WidgetData?>(initialValue = null, version) {
                value = WidgetData.load(appContext)
            }
            WidgetContent(appContext, data, opacity)
        }
    }
}

// ---- Colours ----

private val Lime = ColorProvider(Ink.Lime)
private val OnLime = ColorProvider(Ink.Ground)
private val TextColor = ColorProvider(Ink.Text)
private val MutedColor = ColorProvider(Color(0xFFB9BDB0))
private val Coral = ColorProvider(Ink.Coral)

private fun text(size: Int, color: ColorProvider = TextColor, bold: Boolean = false) =
    TextStyle(color = color, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

// ---- Actions ----

/** Opens the app on Home. */
private fun openApp(context: Context): Action =
    actionStartActivity(Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

/**
 * Opens the app straight into the Add sheet, optionally with a payment method picked.
 * Each method gets its own data URI so Android keeps the buttons' intents apart.
 */
private fun addAction(context: Context, methodId: Long? = null): Action =
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_ADD)
            .setData(Uri.parse("expenses://add/${methodId ?: 0}"))
            .putExtra(MainActivity.EXTRA_METHOD_ID, methodId ?: 0L)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )

// ---- Layouts ----

@Composable
private fun WidgetContent(context: Context, data: WidgetData?, opacity: Int) {
    val background = ColorProvider(Ink.Ground.copy(alpha = opacity.coerceIn(0, 100) / 100f))
    Box(
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(background)
            .clickable(openApp(context))
            .padding(16.dp),
    ) {
        if (data == null) {
            Text("Expenses", style = text(14, MutedColor))
        } else {
            when (LocalSize.current) {
                ExpenseWidget.LARGE -> Large(context, data)
                ExpenseWidget.MEDIUM -> Medium(context, data)
                else -> Small(context, data)
            }
        }
    }
}

/** Today's amount: full rupiah when it fits, compact ("Rp 1,2 jt") when it would not. */
private fun todayText(amount: Long, room: Int): String {
    val full = Money.format(amount)
    return if (full.length <= room) full else Money.compact(amount)
}

@Composable
private fun SpentToday(data: WidgetData, size: Int, room: Int) {
    Column {
        Text("Spent today", style = text(12, MutedColor))
        Spacer(GlanceModifier.height(4.dp))
        Text(todayText(data.today, room), style = text(size, bold = true), maxLines = 1)
    }
}

@Composable
private fun Small(context: Context, data: WidgetData) {
    Column(GlanceModifier.fillMaxSize()) {
        SpentToday(data, size = 22, room = 10)
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            val count = if (data.todayCount == 1) "1 entry" else "${data.todayCount} entries"
            Text(count, style = text(12, MutedColor), modifier = GlanceModifier.defaultWeight(), maxLines = 1)
            AddCircle(context, size = 48)
        }
    }
}

@Composable
private fun Medium(context: Context, data: WidgetData) {
    Row(GlanceModifier.fillMaxSize()) {
        Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
            SpentToday(data, size = 26, room = 13)
            Spacer(GlanceModifier.defaultWeight())
            MonthBlock(data)
        }
        Spacer(GlanceModifier.width(12.dp))
        Box(
            GlanceModifier
                .width(84.dp)
                .fillMaxHeight()
                .cornerRadius(22.dp)
                .background(Lime)
                .clickable(addAction(context)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlusIcon(26)
                Spacer(GlanceModifier.height(4.dp))
                Text("Add", style = text(14, OnLime, bold = true))
            }
        }
    }
}

@Composable
private fun Large(context: Context, data: WidgetData) {
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth()) {
            Box(GlanceModifier.defaultWeight()) { SpentToday(data, size = 26, room = 13) }
            Column(horizontalAlignment = Alignment.End) {
                Text("Saved in ${data.monthShort}", style = text(12, MutedColor))
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    Money.compact(data.monthSaved),
                    style = text(17, if (data.monthSaved < 0) Coral else Lime, bold = true),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        MonthBlock(data)
        Spacer(GlanceModifier.defaultWeight())
        Text("Quick add with", style = text(12, MutedColor))
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            data.quickMethods.forEach { method ->
                Box(
                    GlanceModifier
                        .defaultWeight()
                        .height(48.dp)
                        .cornerRadius(16.dp)
                        .background(ColorProvider(Ink.Text.copy(alpha = 0.10f)))
                        .clickable(addAction(context, method.id)),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.padding(horizontal = 8.dp)) {
                        Box(
                            GlanceModifier.size(8.dp).cornerRadius(4.dp).background(ColorProvider(Color(method.color))),
                        ) {}
                        Spacer(GlanceModifier.width(6.dp))
                        Text(method.name, style = text(13, bold = true), maxLines = 1)
                    }
                }
                Spacer(GlanceModifier.width(6.dp))
            }
            Box(
                GlanceModifier
                    .size(48.dp)
                    .cornerRadius(16.dp)
                    .background(Lime)
                    .clickable(addAction(context)),
                contentAlignment = Alignment.Center,
            ) { PlusIcon(24) }
        }
    }
}

/** Month name with "You kept 65%", the in/out bar, then Out and In. */
@Composable
private fun MonthBlock(data: WidgetData) {
    val income = data.monthIncome
    val spent = data.monthSpent
    val share = when {
        income > 0 -> (spent.toFloat() / income).coerceIn(0f, 1f)
        spent > 0 -> 1f
        else -> 0f
    }
    val over = income in 1 until spent
    Column(GlanceModifier.fillMaxWidth()) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text(data.monthName, style = text(12, MutedColor), modifier = GlanceModifier.defaultWeight())
            Savings.shortNote(income, spent)?.let { note ->
                Text(note, style = text(12, if (over) Coral else Lime, bold = true), maxLines = 1)
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        LinearProgressIndicator(
            progress = share,
            modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
            color = if (over) Coral else TextColor,
            backgroundColor = if (income > 0) Lime else ColorProvider(Ink.Raised),
        )
        Spacer(GlanceModifier.height(6.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Text("Out ${Money.compact(spent)}", style = text(12, MutedColor), modifier = GlanceModifier.defaultWeight(), maxLines = 1)
            Text("In ${Money.compact(income)}", style = text(12, MutedColor), maxLines = 1)
        }
    }
}

@Composable
private fun AddCircle(context: Context, size: Int) {
    Box(
        GlanceModifier
            .size(size.dp)
            .cornerRadius((size / 2).dp)
            .background(Lime)
            .clickable(addAction(context)),
        contentAlignment = Alignment.Center,
    ) { PlusIcon(22) }
}

@Composable
private fun PlusIcon(size: Int) {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_add),
        contentDescription = "Add expense",
        modifier = GlanceModifier.size(size.dp),
        colorFilter = ColorFilter.tint(OnLime),
    )
}
