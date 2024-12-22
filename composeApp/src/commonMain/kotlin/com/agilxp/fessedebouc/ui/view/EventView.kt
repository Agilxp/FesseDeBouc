package com.agilxp.fessedebouc.ui.view

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agilxp.fessedebouc.colors
import com.agilxp.fessedebouc.ui.component.ChatWindow
import com.agilxp.fessedebouc.ui.component.EventDetails
import com.agilxp.fessedebouc.ui.viewmodel.EventViewModel
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel
import com.agilxp.fessedebouc.ui.viewmodel.UserViewModel
import com.composables.core.Dialog
import com.composables.core.DialogPanel
import com.composables.core.Scrim
import com.composables.core.rememberDialogState
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessagesSquare
import com.composables.icons.lucide.Plus

private enum class BottomNavigationItems {
    CHAT,
    EVENT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventView(
    smallScreen: Boolean,
    userViewModel: UserViewModel,
    eventViewModel: EventViewModel,
    groupViewModel: GroupViewModel,
    navController: NavHostController = rememberNavController()
) {
    val userUiState by userViewModel.uiState.collectAsState()
    val eventUiState by eventViewModel.uiState.collectAsState()
    val state = rememberDialogState(initiallyVisible = false)
    var selectedScreen by remember { mutableStateOf(BottomNavigationItems.EVENT) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(eventUiState.selectedEvent?.name ?: "No events") },
                actions = {
                    FloatingActionButton(
                        onClick = { state.visible = true },
                        containerColor = colors.secondaryContainer,
                    ) {
                        Icon(Lucide.Plus, "Create event")
                    }
                },
                navigationIcon = {
                    if (eventUiState.selectedEvent != null) {
                        IconButton(onClick = { eventViewModel.selectEvent(null) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Go back")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (eventUiState.selectedEvent != null) {
                NavigationBar {
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
                }
            }
        }
    ) { paddingValues ->
        if (userUiState.events.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No pending events")
            }
        } else {
            if (eventUiState.selectedEvent == null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (smallScreen) 1 else 2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // List events
                    items(userUiState.events) { event ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = event.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
                                Text(text = "${event.start} - ${event.end}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                // View event details
                NavHost(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    startDestination = BottomNavigationItems.EVENT.name
                ) {
                    composable(route = BottomNavigationItems.EVENT.name) {
                        EventDetails(eventViewModel)
                    }
                    composable(route = BottomNavigationItems.CHAT.name) {
                        ChatWindow(GroupViewModel())
                    }
                }
            }
        }
    }
    Dialog(state) {
        Scrim(enter = fadeIn(), exit = fadeOut(), scrimColor = Color.Black.copy(0.3f))
        DialogPanel(
            modifier = Modifier.systemBarsPadding()
                .fillMaxSize(0.9f)
                .padding(16.dp)
                .shadow(8.dp)
                .background(Color.White)
                .padding(24.dp),
            enter = scaleIn(initialScale = 0.8f) + fadeIn(tween(durationMillis = 250)),
            exit = scaleOut(targetScale = 0.6f) + fadeOut(tween(durationMillis = 150))
        ) {
            Text("Add new event")
        }
    }
}