package cc.skysparkle.fogwave.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cc.skysparkle.fogwave.R

@Composable
// Opens the general battery-optimization list instead of ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:
// that action requires a permission Google Play rejects for ordinary media apps.
fun BatteryOptimizationDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.battery_opt_title)) },
        text = { Text(stringResource(R.string.battery_opt_message)) },
        confirmButton = {
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (_: Exception) {
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.battery_opt_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.battery_opt_dismiss))
            }
        }
    )
}
