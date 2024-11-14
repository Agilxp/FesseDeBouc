package com.agilxp.fessedebouc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agilxp.fessedebouc.colors
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FesseDeBoucApp(
    groupViewModel: GroupViewModel
) {
    val groupUiState by groupViewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fesse De Bouc") },
                modifier = Modifier,
                colors = TopAppBarDefaults.topAppBarColors(colors.primaryContainer)
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier.padding(innerPadding).fillMaxSize().height(1.dp), // Don't know why but height is needed
        ) {
            Box(modifier = Modifier.fillMaxWidth(0.2f).fillMaxHeight()) {
                GroupMenu(groupViewModel)
            }
            VerticalDivider(color = colors.primaryContainer, modifier = Modifier.fillMaxHeight().width(1.dp))
            if (groupUiState.selectedGroup != null) {
                Box(modifier = Modifier.fillMaxWidth(0.75f)) {
                    MainWindow(groupViewModel)
                }
                VerticalDivider(color = colors.primaryContainer, modifier = Modifier.fillMaxHeight().width(1.dp))
                Box(Modifier.fillMaxWidth().fillMaxHeight()) {
                    GroupAdmin(groupViewModel)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No group selected")
                }
            }
        }
    }
}