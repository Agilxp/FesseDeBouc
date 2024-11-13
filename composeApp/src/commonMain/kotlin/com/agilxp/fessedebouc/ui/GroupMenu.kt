package com.agilxp.fessedebouc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    Scaffold(
        content = { innerPadding ->
            Column {
                Text(
                    text = "Groups",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 32.dp, start = 6.dp).fillMaxWidth(),
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
//            VerticalScrollbar(
//                Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
//                scroll
//            )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.height(180.dp).fillMaxWidth().padding(6.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (groupUiState.errorMessage?.isNotEmpty() == true) {
                    Text(groupUiState.errorMessage!!, color = colors.error)
                }
                Text(
                    "New group name",
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                TextField(
                    value = newGroupName,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { newGroupName = it }
                )
                Button(onClick = {
                    groupViewModel.addGroup(newGroupName)
                    newGroupName = ""
                }) {
                    Text("Create Group")
                }
            }
        }
    )

}