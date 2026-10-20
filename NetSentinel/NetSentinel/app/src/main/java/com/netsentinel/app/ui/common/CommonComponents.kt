package com.netsentinel.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netsentinel.app.ui.theme.ErrorRed
import com.netsentinel.app.ui.theme.NeonCyan
import com.netsentinel.app.ui.theme.NeonGreen
import com.netsentinel.app.ui.theme.WarnAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A neon-bordered card used as the base tile shape across the dashboard and detail screens. */
@Composable
fun NeonCard(
    accentColor: Color = NeonCyan,
    modifier: Modifier = Modifier,
    content: ColumnScopeAlias,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

// Typealias so NeonCard's content lambda has Compose's ColumnScope without importing it at call sites.
typealias ColumnScopeAlias = @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit

enum class StatusLevel { OK, WARNING, ERROR }

@Composable
fun StatusBadge(label: String, level: StatusLevel, modifier: Modifier = Modifier) {
    val (icon, color) = when (level) {
        StatusLevel.OK -> Icons.Filled.CheckCircle to NeonGreen
        StatusLevel.WARNING -> Icons.Filled.Warning to WarnAmber
        StatusLevel.ERROR -> Icons.Filled.Error to ErrorRed
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = color)
        Text(label, color = color, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LabelValueRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = valueColor, style = MaterialTheme.typography.labelSmall)
    }
}

fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))

fun formatTimestampFull(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
