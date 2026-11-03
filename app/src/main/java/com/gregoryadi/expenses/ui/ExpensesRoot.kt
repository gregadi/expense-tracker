package com.gregoryadi.expenses.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gregoryadi.expenses.ui.add.AddExpenseSheet
import com.gregoryadi.expenses.ui.add.AddExpenseViewModel
import com.gregoryadi.expenses.ui.components.LocalSnackbarController
import com.gregoryadi.expenses.ui.components.Motion
import com.gregoryadi.expenses.ui.components.PillButton
import com.gregoryadi.expenses.ui.components.SnackbarController
import com.gregoryadi.expenses.ui.components.bouncyClick
import com.gregoryadi.expenses.ui.history.HistoryScreen
import com.gregoryadi.expenses.ui.home.HomeScreen
import com.gregoryadi.expenses.ui.insights.InsightsScreen
import com.gregoryadi.expenses.ui.manage.ManageScreen
import com.gregoryadi.expenses.ui.settings.SettingsScreen
import kotlin.math.sign

private enum class Tab(val label: String) {
    HOME("Home"),
    HISTORY("History"),
    INSIGHTS("Insights"),
}

/** Screens stacked on top of the tabs; a higher ordinal is "deeper". */
private enum class Overlay { NONE, SETTINGS, MANAGE }

private data class Screen(val tab: Tab, val overlay: Overlay)

/**
 * App shell. Content fills the screen; a floating pill at the bottom holds the three tabs and the Add button,
 * on both the cover screen and the inner screen (the inner screen's screens switch to two panes on their own).
 *
 * Motion: tabs cross-fade with a short slide in the direction you moved; Settings and Manage slide in from
 * the right and back out; the nav pill slides away while they are open.
 */
@Composable
fun ExpensesRoot(widthClass: WindowWidthSizeClass) {
    // The Fold's inner screen is nearly square (~600-840 dp wide), so "not compact" is the unfolded layout.
    val isWide = widthClass != WindowWidthSizeClass.Compact

    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarController(snackbarHost, scope) }

    val addVm = appViewModel { AddExpenseViewModel(it.expenses, it.lookups, it.settings) }
    val addState by addVm.state.collectAsStateWithLifecycle()

    // Widget taps: close Settings if it's open and show the Add sheet.
    val container = LocalAppContainer.current
    LaunchedEffect(container) {
        for (request in container.addRequests) {
            overlay = Overlay.NONE
            addVm.startNew(request.methodId)
        }
    }

    BackHandler(enabled = overlay != Overlay.NONE) {
        overlay = if (overlay == Overlay.MANAGE) Overlay.SETTINGS else Overlay.NONE
    }
    BackHandler(enabled = overlay == Overlay.NONE && tab != Tab.HOME) { tab = Tab.HOME }

    // Text and icons default to the light "on background" colour. (Material only sets this inside a
    // Surface or Scaffold; without it, everything not given a colour falls back to black.)
    CompositionLocalProvider(
        LocalSnackbarController provides snackbar,
        LocalContentColor provides MaterialTheme.colorScheme.onBackground,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
        ) {
            AnimatedContent(
                targetState = Screen(tab, overlay),
                transitionSpec = { screenTransition(initialState, targetState) },
                label = "screen",
                modifier = Modifier.fillMaxSize(),
            ) { screen ->
                when (screen.overlay) {
                    Overlay.SETTINGS -> SettingsScreen(
                        onBack = { overlay = Overlay.NONE },
                        onOpenManage = { overlay = Overlay.MANAGE },
                    )
                    Overlay.MANAGE -> ManageScreen(onBack = { overlay = Overlay.SETTINGS })
                    Overlay.NONE -> when (screen.tab) {
                        Tab.HOME -> HomeScreen(
                            isWide = isWide,
                            onEdit = addVm::startEdit,
                            onOpenSettings = { overlay = Overlay.SETTINGS },
                            onSeeAll = { tab = Tab.HISTORY },
                        )
                        Tab.HISTORY -> HistoryScreen(isWide = isWide, onEdit = addVm::startEdit)
                        Tab.INSIGHTS -> InsightsScreen(isWide = isWide)
                    }
                }
            }

            AnimatedVisibility(
                visible = overlay == Overlay.NONE,
                enter = slideInVertically(tween(Motion.ENTER_MS + 80, easing = FastOutSlowInEasing)) { it * 2 } + fadeIn(),
                exit = slideOutVertically(tween(Motion.ENTER_MS, easing = FastOutSlowInEasing)) { it * 2 } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                FloatingNav(selected = tab, onSelect = { tab = it }, onAdd = { addVm.startNew() })
            }

            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (overlay == Overlay.NONE) 100.dp else 16.dp)
                    .widthIn(max = 480.dp),
            )
        }

        if (addState.visible) {
            AddExpenseSheet(vm = addVm)
        }
    }
}

private fun screenTransition(from: Screen, to: Screen): ContentTransform {
    val ease = FastOutSlowInEasing
    return when {
        // Opening Settings / Manage: slide in from the right over a slightly receding screen.
        to.overlay.ordinal > from.overlay.ordinal ->
            (slideInHorizontally(tween(Motion.ENTER_MS + 60, easing = ease)) { it / 3 } + fadeIn(tween(Motion.ENTER_MS))) togetherWith
                (slideOutHorizontally(tween(Motion.ENTER_MS + 60, easing = ease)) { -it / 10 } + fadeOut(tween(Motion.ENTER_MS / 2)))
        // Going back: the reverse.
        to.overlay.ordinal < from.overlay.ordinal ->
            (slideInHorizontally(tween(Motion.ENTER_MS + 60, easing = ease)) { -it / 10 } + fadeIn(tween(Motion.ENTER_MS))) togetherWith
                (slideOutHorizontally(tween(Motion.ENTER_MS + 60, easing = ease)) { it / 3 } + fadeOut(tween(Motion.ENTER_MS / 2)))
        // Switching tabs: a short slide in the direction of travel, with a cross-fade.
        else -> {
            val direction = (to.tab.ordinal - from.tab.ordinal).sign
            (fadeIn(tween(Motion.ENTER_MS, delayMillis = 60)) +
                slideInHorizontally(tween(Motion.ENTER_MS + 60, easing = ease)) { direction * it / 12 } +
                scaleIn(tween(Motion.ENTER_MS + 60, easing = ease), initialScale = 0.98f)) togetherWith
                (fadeOut(tween(120)) + slideOutHorizontally(tween(Motion.ENTER_MS, easing = ease)) { -direction * it / 16 })
        }
    }
}

/** The floating pill: three round tab buttons and the lime Add button. */
@Composable
private fun FloatingNav(selected: Tab, onSelect: (Tab) -> Unit, onAdd: () -> Unit) {
    val shape = RoundedCornerShape(36.dp)
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        // Even spacing across the whole pill: three tabs and the Add button share the width.
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Tab.entries.forEach { entry ->
            NavButton(entry, selected = selected == entry, onClick = { onSelect(entry) })
        }
        PillButton(
            text = "Add",
            onClick = onAdd,
            height = 48.dp,
            leading = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
        )
    }
}

@Composable
private fun NavButton(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
        tween(Motion.ENTER_MS),
        label = "navBg",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(Motion.ENTER_MS),
        label = "navFg",
    )
    Column(
        Modifier
            .height(56.dp)
            .widthIn(min = 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .bouncyClick(pressedScale = 0.9f, onClick = onClick)
            .background(container)
            .padding(horizontal = 10.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            when (tab) {
                Tab.HOME -> Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(22.dp))
                Tab.HISTORY -> Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null, modifier = Modifier.size(22.dp))
                Tab.INSIGHTS -> BarsIcon()
            }
            Text(tab.label, style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1)
        }
    }
}

/** Three little bars; the core icon set has no bar chart, and this avoids the huge extended set. */
@Composable
private fun BarsIcon() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(22.dp)) {
        val barWidth = size.width * 0.16f
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val heights = listOf(0.45f, 0.85f, 0.6f)
        heights.forEachIndexed { i, h ->
            val x = size.width * (0.17f + i * 0.28f)
            val barHeight = size.height * h
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height * 0.92f - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}
