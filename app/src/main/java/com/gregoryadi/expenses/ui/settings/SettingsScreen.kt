package com.gregoryadi.expenses.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gregoryadi.expenses.ui.appViewModel
import com.gregoryadi.expenses.ui.components.BackHeader
import com.gregoryadi.expenses.ui.components.FlatCard
import com.gregoryadi.expenses.ui.components.LocalSnackbarController
import com.gregoryadi.expenses.ui.components.SectionTitle
import java.time.LocalDate

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenManage: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it.settings, it.porter) }
    val snackbar = LocalSnackbarController.current
    val report: (String) -> Unit = { snackbar.show(it) }
    val stamp = LocalDate.now().toString()

    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportJson(uri, report)
    }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri, report)
    }
    val openJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importJson(uri, report)
    }

    Column(Modifier.fillMaxSize()) {
        BackHeader(title = "Settings", onBack = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FlatCard {
                    SectionTitle("Your lists")
                    SettingRow("Payment methods & categories", "Methods, spending categories and income sources", onClick = onOpenManage)
                }

                FlatCard {
                    SectionTitle("Backup")
                    SettingRow(
                        "Save backup",
                        "Everything as a JSON file you can keep in Drive or Downloads",
                        onClick = { saveJson.launch("expenses-backup-$stamp.json") },
                    )
                    SettingRow(
                        "Restore backup",
                        "Adds what's missing; importing the same file twice is safe",
                        onClick = { openJson.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    )
                    SettingRow(
                        "Export CSV",
                        "For Google Sheets or Excel",
                        onClick = { saveCsv.launch("expenses-$stamp.csv") },
                    )
                }

                Text(
                    "All data stays on this phone. Android Auto Backup also copies it to your Google account, " +
                        "and a saved backup file is your second copy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
