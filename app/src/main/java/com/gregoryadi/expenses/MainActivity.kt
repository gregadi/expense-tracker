package com.gregoryadi.expenses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import com.gregoryadi.expenses.ui.ExpensesRoot
import com.gregoryadi.expenses.ui.LocalAppContainer
import com.gregoryadi.expenses.ui.theme.ExpensesTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ink is always dark, so the status and navigation bar icons are always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        val container = (application as ExpensesApplication).container

        setContent {
            // Recomputed whenever the window changes size, e.g. when the Fold is opened or closed.
            val sizeClass = calculateWindowSizeClass(this)

            CompositionLocalProvider(LocalAppContainer provides container) {
                ExpensesTheme {
                    ExpensesRoot(widthClass = sizeClass.widthSizeClass)
                }
            }
        }
    }
}
