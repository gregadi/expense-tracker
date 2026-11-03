package com.gregoryadi.expenses.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.gregoryadi.expenses.domain.Money
import com.gregoryadi.expenses.ui.components.PillButton
import com.gregoryadi.expenses.ui.theme.ExpensesTheme
import com.gregoryadi.expenses.ui.theme.Ink
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Widget settings: background transparency, with a live preview over a sample wallpaper.
 * Opens when the widget is added (it can be skipped) and from the widget's long-press menu.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var opacity by mutableIntStateOf(WidgetKeys.DEFAULT_OPACITY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Settings are optional: backing out still keeps the widget, with the default look.
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Start from this widget's current setting.
        lifecycleScope.launch {
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                val prefs: Preferences = getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId)
                prefs[WidgetKeys.OPACITY]?.let { opacity = it }
            }
        }

        setContent {
            ExpensesTheme {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                    ConfigScreen(
                        opacity = opacity,
                        onOpacity = { opacity = it },
                        onDone = { save(appWidgetId) },
                    )
                }
            }
        }
    }

    private fun save(appWidgetId: Int) {
        lifecycleScope.launch {
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                    prefs[WidgetKeys.OPACITY] = opacity
                    prefs[WidgetKeys.VERSION] = System.currentTimeMillis()
                }
                ExpenseWidget().update(this@WidgetConfigActivity, glanceId)
            }
            finish()
        }
    }
}

@Composable
private fun ConfigScreen(opacity: Int, onOpacity: (Int) -> Unit, onDone: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Widget", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
            Preview(opacity)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Background", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        if (opacity == 0) "Transparent" else "$opacity%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = opacity.toFloat(),
                    onValueChange = { onOpacity((it / 5f).roundToInt() * 5) },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                )
                Text(
                    "Lower it to see your wallpaper behind the widget. On bright wallpapers, keep some background " +
                        "so the numbers stay readable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            PillButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** A medium widget over a sample wallpaper, so the transparency can be judged while sliding. */
@Composable
private fun Preview(opacity: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF23415A)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFE07A5F), radius = size.height * 0.75f, center = Offset(size.width * 0.05f, size.height * 1.0f))
            drawCircle(Color(0xFFF2CC8F), radius = size.height * 0.6f, center = Offset(size.width * 0.55f, -size.height * 0.1f))
            drawCircle(Color(0xFF4F9D8C), radius = size.height * 0.65f, center = Offset(size.width * 1.0f, size.height * 0.85f))
        }
        Row(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Ink.Ground.copy(alpha = opacity / 100f))
                .padding(16.dp),
        ) {
            Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Spent today", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB9BDB0))
                    Text(Money.format(86_000), style = MaterialTheme.typography.titleLarge, color = Ink.Text)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Ink.Lime)) {
                        Box(Modifier.fillMaxWidth(0.35f).height(6.dp).clip(CircleShape).background(Ink.Text))
                    }
                    Text("Out Rp 4,32 jt · In Rp 12,5 jt", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB9BDB0))
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.width(72.dp).fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Ink.Lime),
                contentAlignment = Alignment.Center,
            ) {
                Text("+ Add", style = MaterialTheme.typography.labelLarge, color = Ink.Ground)
            }
        }
    }
}
