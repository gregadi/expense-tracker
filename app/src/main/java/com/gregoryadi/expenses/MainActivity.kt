package com.gregoryadi.expenses

import android.content.Intent
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

    companion object {
        /** Sent by the home screen widget: open straight into the Add sheet. */
        const val ACTION_ADD = "com.gregoryadi.expenses.action.ADD"

        /** Optional payment method to preselect (0 = the usual default). */
        const val EXTRA_METHOD_ID = "method_id"
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ink is always dark, so the status and navigation bar icons are always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        val container = (application as ExpensesApplication).container
        // Only a fresh launch; after the system recreates the activity the request was already handled.
        if (savedInstanceState == null) handleWidgetIntent(intent)

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

    /** The app is already open (singleTask) and the widget was tapped again. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent?.action != ACTION_ADD) return
        val methodId = intent.getLongExtra(EXTRA_METHOD_ID, 0L).takeIf { it != 0L }
        (application as ExpensesApplication).container.addRequests.trySend(AddRequest(methodId))
        // Consumed: don't reopen the sheet if this intent is looked at again.
        intent.action = Intent.ACTION_MAIN
    }
}
