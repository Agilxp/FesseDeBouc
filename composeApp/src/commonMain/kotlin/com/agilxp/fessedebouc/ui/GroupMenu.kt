package com.agilxp.fessedebouc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agilxp.fessedebouc.colors
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel

@Composable
fun GroupMenu(
    groupViewModel: GroupViewModel = viewModel { GroupViewModel() }
) {
    val scroll = rememberScrollState()
    val groupUiState by groupViewModel.uiState.collectAsState()
    var newGroupName by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    Scaffold(
        content = { innerPadding ->
            Column {
                Text(
                    text = "Groups",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 32.dp, start = 16.dp).fillMaxWidth(),
                )

                Box(Modifier.padding(innerPadding)) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scroll),
                    ) {
                        groupUiState.myGroups.forEach { group ->
                            Box(
                                modifier = Modifier.fillMaxWidth().clickable { groupViewModel.selectGroup(group) }) {
                                Card(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(text = group.name, fontSize = 18.sp)
                                        Text(text = "(${group.users.size} members)", fontSize = 8.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.height(180.dp).fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (groupUiState.errorMessage?.isNotEmpty() == true) {
                    Text(groupUiState.errorMessage!!, color = colors.error)
                }
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("First name") },
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    singleLine = true,
                    trailingIcon = {
                        AnimatedVisibility(visible = newGroupName.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                            IconButton(onClick = { newGroupName = "" }) {
                                Icon(Icons.Outlined.Clear, "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = KeyboardActions {
                        focusManager.moveFocus(FocusDirection.Next)
                    })
                Box(Modifier.padding(top = 4.dp).clip(MaterialTheme.shapes.medium).clickable(role = Role.Button) {
                    groupViewModel.addGroup(newGroupName)
                    newGroupName = ""
                }
                    .background(colors.primaryContainer).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    BasicText(
                        text = "Create Group",
                        style = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight(600))
                    )
                }
            }
        }
    )

}