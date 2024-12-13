package com.agilxp.fessedebouc.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agilxp.fessedebouc.colors
import com.agilxp.fessedebouc.ui.GroupAdmin
import com.agilxp.fessedebouc.ui.component.ChatWindow
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel

@Composable
fun GroupView(smallScreen: Boolean, groupViewModel: GroupViewModel) {
    val groupUiState by groupViewModel.uiState.collectAsState()
    if (smallScreen) {
        Scaffold(
            bottomBar = {
                NavigationBar(Modifier.fillMaxSize()) {
                    groupUiState.myGroups.forEachIndexed { index, map ->
                        val group = groupUiState.myGroups[index]
                        val label = group.name
                        NavigationBarItem(
                            selected = group == groupUiState.selectedGroup,
                            icon = { },
                            label = { Text(label) },
                            onClick = { groupViewModel.selectGroup(group) },
                            colors = NavigationBarItemDefaults.colors()
                        )
                    }
                }
            }
        ) { contentPadding ->
            Text("small GroupView", Modifier.padding(contentPadding).fillMaxSize())
        }
    } else {
        Scaffold { contentPadding ->
            PermanentNavigationDrawer(
                modifier = Modifier.fillMaxSize().background(colors.primaryContainer),
                drawerContent = {
                    PermanentDrawerSheet(Modifier.width(200.dp).fillMaxSize()) {
                        LazyColumn(Modifier.padding(vertical = 30.dp).fillMaxSize()) {
                            items(groupUiState.myGroups) { group ->
                                val label = group.name
                                NavigationDrawerItem(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    selected = group == groupUiState.selectedGroup,
                                    label = { Text(label) },
                                    onClick = { groupViewModel.selectGroup(group) },
                                    shape = ShapeDefaults.Medium
                                )
                            }
                        }
                    }
                },
                content = {
                    Row(Modifier.fillMaxSize()) {
                        VerticalDivider(color = colors.onPrimaryContainer)
                        if (groupUiState.selectedGroup != null) {
                            Row(Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.fillMaxWidth(0.75f)) {
                                    ChatWindow(groupViewModel)
                                }
                                VerticalDivider(color = colors.onPrimaryContainer)
                                Box(Modifier.fillMaxWidth().fillMaxHeight()) {
                                    GroupAdmin(groupViewModel)
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize()) {
                                Text("Select a group")
                            }
                        }
                    }
                }
            )
        }
    }

}