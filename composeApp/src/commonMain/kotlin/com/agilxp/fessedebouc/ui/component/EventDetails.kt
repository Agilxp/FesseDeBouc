package com.agilxp.fessedebouc.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.agilxp.fessedebouc.ui.viewmodel.EventViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MailQuestion

@Composable
fun EventDetails(eventViewModel: EventViewModel) {
    val eventUiState by eventViewModel.uiState.collectAsState()
    val selectedEvent = eventUiState.selectedEvent!!
    Column(modifier = Modifier.fillMaxSize()) {
        Text(selectedEvent.name, style = MaterialTheme.typography.titleLarge)
        Text(
            selectedEvent.description,
            style = MaterialTheme.typography.bodyMedium
        )
        Text("${selectedEvent.start} - ${selectedEvent.end}", style = MaterialTheme.typography.bodySmall)
        Row(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { eventViewModel.attendEvent(selectedEvent) },
            ) {
                Icon(
                    (Icons.Rounded.Check),
                    contentDescription = "Attend",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { eventViewModel.maybeEvent(selectedEvent) },
            ) {
                Icon(
                    (Lucide.MailQuestion),
                    contentDescription = "Maybe",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { eventViewModel.cannotEvent(selectedEvent) },
            ) {
                Icon(
                    (Icons.Rounded.Close),
                    contentDescription = "Cannot",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}