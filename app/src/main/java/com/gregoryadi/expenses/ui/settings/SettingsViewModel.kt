package com.gregoryadi.expenses.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregoryadi.expenses.data.port.DataPorter
import com.gregoryadi.expenses.data.prefs.SettingsStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsStore,
    private val porter: DataPorter,
) : ViewModel() {

    val dynamicColor: StateFlow<Boolean> = settings.dynamicColor

    fun setDynamicColor(enabled: Boolean) = settings.setDynamicColor(enabled)

    /** Each action reports back with a short message for a snackbar. */
    fun exportJson(uri: Uri, report: (String) -> Unit) = launchReporting(report) {
        val count = porter.exportJson(uri)
        "Backup saved ($count entries)"
    }

    fun exportCsv(uri: Uri, report: (String) -> Unit) = launchReporting(report) {
        val count = porter.exportCsv(uri)
        "CSV saved ($count entries)"
    }

    fun importJson(uri: Uri, report: (String) -> Unit) = launchReporting(report) {
        val result = porter.importJson(uri)
        "Restored ${result.added} entries, ${result.skipped} already there"
    }

    private fun launchReporting(report: (String) -> Unit, block: suspend () -> String) {
        viewModelScope.launch {
            val message = try {
                block()
            } catch (e: Exception) {
                "Failed: ${e.message ?: e.javaClass.simpleName}"
            }
            report(message)
        }
    }
}
