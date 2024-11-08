package com.agilxp.fessedebouc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
        content = {
            Column {
                Text(
                    text = "Groups",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 32.dp, start = 6.dp).fillMaxWidth(),
                )

                Box(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.verticalScroll(scroll),
                    ) {
                        groupUiState.myGroups.forEach { group ->
                            Box(
                                modifier = Modifier.fillMaxWidth().clickable { groupViewModel.selectGroup(group) }) {
                                Text(text = group.name, fontSize = 18.sp)
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
                modifier = Modifier.height(140.dp).fillMaxWidth(0.9f),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (groupUiState.errorMessage?.isNotEmpty() == true) {
                    Text(groupUiState.errorMessage!!, color = colors.error)
                }
                Text("New group name", fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                TextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it })
                Button(modifier = Modifier, onClick = { groupViewModel.addGroup(newGroupName) }) {
                    Text("Add Group")
                }
            }
        }
    )

}