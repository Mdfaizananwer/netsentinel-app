package com.netsentinel.app.ui.common

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Location is required by Android to surface SSID/BSSID/scan results — explained in-UI, requested only when needed. */
fun locationPermissions(): Array<String> = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

fun hasAnyGranted(context: android.content.Context, permissions: Array<String>): Boolean =
    permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

@Composable
fun rememberPermissionState(permissions: Array<String>): PermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasAnyGranted(context, permissions)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        granted = results.values.any { it }
    }
    return remember(granted) {
        PermissionState(granted = granted, request = { launcher.launch(permissions) })
    }
}

data class PermissionState(val granted: Boolean, val request: () -> Unit)
