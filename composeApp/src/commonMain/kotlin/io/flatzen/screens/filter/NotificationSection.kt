package io.flatzen.screens.filter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun NotificationSection(
    enabled: Boolean,
    interval: Int?,
    hasNotificationFilter: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onApplyFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Enable notifications checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enable Notifications",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        // Show additional options when enabled
        if (enabled) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Send notifications every:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Interval selection
            IntervalSelector(
                selectedInterval = interval,
                onIntervalSelected = onIntervalChange,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Apply button
            Button(
                onClick = onApplyFilter,
                modifier = Modifier.fillMaxWidth(),
                enabled = interval != null
            ) {
                Text("Apply selected filters for notifications")
            }
        }
    }
}

@Composable
private fun IntervalSelector(
    selectedInterval: Int?,
    onIntervalSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.selectableGroup()
    ) {
        val intervals = listOf(
            15 to "15 minutes",
            30 to "30 minutes", 
            60 to "1 hour"
        )
        
        intervals.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedInterval == value,
                        onClick = { onIntervalSelected(value) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedInterval == value,
                    onClick = null // handled by selectable modifier
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}