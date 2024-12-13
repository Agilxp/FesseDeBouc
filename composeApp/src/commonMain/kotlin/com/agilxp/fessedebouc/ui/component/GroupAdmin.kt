package com.agilxp.fessedebouc.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agilxp.fessedebouc.colors
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel
import com.agilxp.fessedebouc.util.isValidEmail

@Composable
fun GroupAdmin(
    groupViewModel: GroupViewModel
) {
    val groupUiState by groupViewModel.uiState.collectAsState()
    var emailAddressInvite by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var borderDp by remember { mutableStateOf(1.dp) }
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Members (admin/kick)")
            //groupUiState.selectedGroup?.users
            Text("Invitate")
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
            Text("Requests")
            Text("Leave")
        }
    }
}