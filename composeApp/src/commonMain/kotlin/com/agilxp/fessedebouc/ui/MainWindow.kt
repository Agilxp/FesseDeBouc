package com.agilxp.fessedebouc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel

private enum class BottomNavigationItems {
    CHAT,
    EVENT,
}

@Composable
fun MainWindow(
    navController: NavHostController = rememberNavController(),
    groupViewModel: GroupViewModel = viewModel { GroupViewModel() }
) {
    val groupUiState by groupViewModel.uiState.collectAsState()
    var selectedScreen by remember { mutableStateOf(BottomNavigationItems.CHAT) }
    Scaffold(
        bottomBar = {
            BottomNavigation {
                BottomNavigationItem(
                    selected = BottomNavigationItems.CHAT == selectedScreen,
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = BottomNavigationItems.CHAT.name
                        )
                    },
                    onClick = {
                        selectedScreen = BottomNavigationItems.CHAT
                        navController.navigate(
                            route = BottomNavigationItems.CHAT.name
                        ) {
                            navController.graph.startDestinationRoute?.let {
                                popUpTo(it) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
                BottomNavigationItem(
                    selected = BottomNavigationItems.EVENT == selectedScreen,
                    icon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = BottomNavigationItems.EVENT.name
                        )
                    },
                    onClick = {
                        selectedScreen = BottomNavigationItems.EVENT
                        navController.navigate(
                            route = BottomNavigationItems.EVENT.name
                        ) {
                            navController.graph.startDestinationRoute?.let {
                                popUpTo(it) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState()),
            startDestination = BottomNavigationItems.CHAT.name
        ) {
            composable(route = BottomNavigationItems.CHAT.name) {
                Column(Modifier.padding(16.dp).fillMaxSize()) {
                    Text(groupUiState.selectedGroup?.name ?: "No group selected")
                    Text("Chats")
                }
            }
            composable(route = BottomNavigationItems.EVENT.name) {
                Column(Modifier.padding(16.dp).fillMaxSize()) {
                    Text(groupUiState.selectedGroup?.name ?: "No group selected")
                    Text("Events")
                }
            }
        }
    }
}