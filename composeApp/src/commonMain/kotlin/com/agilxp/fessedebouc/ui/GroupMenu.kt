package com.agilxp.fessedebouc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GroupMenu(
    groupViewModel: GroupViewModel = viewModel { GroupViewModel() }
) {
    val groupUiState by groupViewModel.uiState.collectAsState()
    Column(
        modifier = Modifier.width(240.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Groups",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp, start = 6.dp).fillMaxWidth(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 6.dp),
        ) {
            groupUiState.myGroups.forEach { group ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(text = group.name, fontSize = 18.sp)
                }
            }
        }

        if (groupUiState.errorMessage?.isNotEmpty() == true) {
            Text(groupUiState.errorMessage!!)
        }

        Box (
            modifier = Modifier.height(50.dp).fillMaxWidth().padding(start = 6.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Button(shape = RectangleShape, onClick = { groupViewModel.addGroup("Test group 6") }) {
                Text("Add Group")
            }
        }
    }
}