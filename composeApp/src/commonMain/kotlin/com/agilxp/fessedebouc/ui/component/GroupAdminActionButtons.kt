package com.agilxp.fessedebouc.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agilxp.fessedebouc.model.UserDTO
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldMinus
import com.composables.icons.lucide.ShieldPlus
import com.composables.icons.lucide.UserMinus

@Composable
fun GroupAdminActionButtons(groupViewModel: GroupViewModel, user: UserDTO) {
    val groupUiState by groupViewModel.uiState.collectAsState()
    val selectedGroup = groupUiState.selectedGroup!!
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        ExtendedFloatingActionButton(
            onClick = {
                groupViewModel.kickUser(user)
            },
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
            ) {
                Icon(Lucide.UserMinus, null)
                Text("Kick user")
            }
        }
        if (selectedGroup.admins.contains(user)) {
            ExtendedFloatingActionButton(
                onClick = {
                    groupViewModel.removeGroupAdmin(user)
                },
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                ) {
                    Icon(Lucide.ShieldMinus, null)
                    Text("Remove admin")
                }
            }
        } else {
            ExtendedFloatingActionButton(
                onClick = {
                    groupViewModel.addGroupAdmin(user)
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                ) {
                    Icon(Lucide.ShieldPlus, null)
                    Text("Make admin")
                }
            }
        }
    }
}