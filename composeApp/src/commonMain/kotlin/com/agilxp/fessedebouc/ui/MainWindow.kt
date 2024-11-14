package com.agilxp.fessedebouc.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agilxp.fessedebouc.ui.component.ChatWindow
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessagesSquare

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
    if (groupUiState.selectedGroup != null) {
        var selectedScreen by remember { mutableStateOf(BottomNavigationItems.CHAT) }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = BottomNavigationItems.CHAT == selectedScreen,
                        colors = NavigationBarItemDefaults.colors(),
                        icon = {
                            Icon(
                                Lucide.MessagesSquare,
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
                    NavigationBarItem(
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
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                startDestination = BottomNavigationItems.CHAT.name
            ) {
                composable(route = BottomNavigationItems.CHAT.name) {
                    ChatWindow(groupViewModel)
                }
                composable(route = BottomNavigationItems.EVENT.name) {
                    Column(Modifier.padding(16.dp).fillMaxSize()) {
                        Text(groupUiState.selectedGroup?.name ?: "No group selected")
                        Text("Events")
                    }
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No group selected")
        }
    }
}