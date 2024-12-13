package com.agilxp.fessedebouc.ui.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agilxp.fessedebouc.ui.viewmodel.UserViewModel

@Composable
fun InvitationView(smallScreen: Boolean, userViewModel: UserViewModel) {
    val userUiState by userViewModel.uiState.collectAsState()

    if (userUiState.invitations.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("No pending invitations")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (smallScreen) 1 else 2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(userUiState.invitations) { item ->
                val color = when (item.status.lowercase()) {
                    "accepted" -> Color.Green
                    "pending" -> Color.Blue
                    "declined" -> Color.Red
                    else -> Color.Black
                }
                OutlinedCard(modifier = Modifier.fillMaxWidth().border(1.dp, color)) {
                    Row(verticalAlignment = CenterVertically) {
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Invitation to join ${item.group.name}",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "# members: ${item.group.users.size}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { userViewModel.acceptInvitation(item) },
                            enabled = item.status.lowercase() == "pending"
                        ) {
                            Icon(
                                (Icons.Rounded.Check),
                                contentDescription = "Accept invitation",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { userViewModel.rejectInvitation(item) },
                            enabled = item.status.lowercase() == "pending"
                        ) {
                            Icon(
                                (Icons.Rounded.Close),
                                contentDescription = "Deny invitation",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}