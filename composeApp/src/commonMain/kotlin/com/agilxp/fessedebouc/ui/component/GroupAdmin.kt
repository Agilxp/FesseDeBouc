package com.agilxp.fessedebouc.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agilxp.fessedebouc.colors
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel
import com.agilxp.fessedebouc.util.isValidEmail
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.UserRoundMinus

@Composable
fun GroupAdmin(
    groupViewModel: GroupViewModel,
    smallScreen: Boolean
) {
    val groupUiState by groupViewModel.uiState.collectAsState()
    var emailAddressInvite by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var borderDp by remember { mutableStateOf(1.dp) }
    val selectedGroup = groupUiState.selectedGroup!!
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(Modifier.fillMaxWidth().height(500.dp)) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Members", style = MaterialTheme.typography.titleLarge)
                }
                LazyColumn(Modifier.padding(vertical = 10.dp).fillMaxSize()) {
                    items(selectedGroup.users) { user ->
                        OutlinedCard(Modifier.padding(horizontal = 10.dp)) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=256&q=80",
                                        modifier = Modifier.size(100.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                        contentDescription = "User image"
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(user.name, style = MaterialTheme.typography.titleLarge)
                                        Text(user.email, style = MaterialTheme.typography.titleSmall)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    if (!smallScreen) {
                                        GroupAdminActionButtons(groupViewModel, user)
                                    }
                                }
                                if (smallScreen) {
                                    GroupAdminActionButtons(groupViewModel, user)
                                }
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Invite", style = MaterialTheme.typography.titleLarge)
            }
            BasicTextField(
                value = emailAddressInvite,
                onValueChange = { emailAddressInvite = it },
                modifier = Modifier.onFocusChanged {
                    borderDp = if (it.isFocused) 2.dp else 1.dp
                }.fillMaxWidth().shadow(2.dp, RoundedCornerShape(4.dp))
                    .border(
                        borderDp,
                        if (emailError) colors.error else colors.primaryContainer,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight(400), color = colors.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            Icons.Outlined.Email,
                            null,
                            colorFilter = ColorFilter.tint(
                                (if (emailError) colors.error else colors.primaryContainer).copy(
                                    alpha = 0.8f
                                )
                            )
                        )

                        Box(contentAlignment = Alignment.CenterStart) {
                            if (emailAddressInvite.isBlank()) {
                                BasicText(
                                    "Email address to send invitation to",
                                    style = TextStyle(color = colors.primary)
                                )
                            }
                            innerTextField()
                        }
                    }
                })
            Spacer(Modifier.height(8.dp))
            if (emailError) {
                BasicText(
                    "Email is invalid",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = if (emailError) colors.error else colors.primaryContainer,
                        fontWeight = FontWeight(500)
                    )
                )
            }
            if (!groupUiState.invitationMessages.isNullOrEmpty()) {
                BasicText(
                    groupUiState.invitationMessages!!,
                    style = TextStyle(fontSize = 12.sp, color = colors.primaryContainer, fontWeight = FontWeight(500))
                )
            }
            Box(Modifier.padding(top = 4.dp).clip(MaterialTheme.shapes.medium).clickable(role = Role.Button) {
                if (isValidEmail(emailAddressInvite)) {
                    groupViewModel.sendInvitation(emailAddressInvite)
                    emailAddressInvite = ""
                    emailError = false
                } else {
                    emailError = true
                }
            }
                .background(colors.primaryContainer).padding(horizontal = 14.dp, vertical = 10.dp)) {
                BasicText(
                    text = "Send invitation",
                    style = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight(600))
                )
            }
            ExtendedFloatingActionButton(
                onClick = {
                    groupViewModel.leaveGroup()
                },
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                ) {
                    Icon(Lucide.UserRoundMinus, null)
                    Text("Leave group")
                }
            }
        }
    }
}