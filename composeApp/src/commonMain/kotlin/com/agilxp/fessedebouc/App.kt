package com.agilxp.fessedebouc

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agilxp.fessedebouc.ui.view.GroupView
import com.agilxp.fessedebouc.ui.view.InvitationView
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel
import com.agilxp.fessedebouc.ui.viewmodel.UserViewModel
import com.composables.icons.lucide.*

val navigationItems = listOf(
    mapOf("icon" to Lucide.MessagesSquare, "label" to "Chats"),
    mapOf("icon" to Lucide.CalendarCheck2, "label" to "Events"),
    mapOf("icon" to Lucide.MailCheck, "label" to "Invitations"),
    mapOf("icon" to Lucide.UserCog, "label" to "Profile"),
)

@Composable
fun App(
    groupViewModel: GroupViewModel = viewModel { GroupViewModel() },
    userViewModel: UserViewModel = viewModel { UserViewModel() }
) {
    colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    var navigationIndex by remember { mutableStateOf(0) }
    val userUiState by userViewModel.uiState.collectAsState()
    MaterialTheme(colorScheme = colors) {
        BoxWithConstraints {
            val smallScreen = maxWidth.value < 1000
            if (smallScreen) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            navigationItems.forEachIndexed { index, map ->
                                val item = navigationItems.get(index)
                                val icon = item["icon"] as ImageVector
                                var label = item["label"] as String
                                when (index) {
                                    1 -> label += " (${userUiState.events.size})"
                                    2 -> label += " (${userUiState.invitations.size})"
                                }
                                NavigationBarItem(
                                    selected = index == navigationIndex,
                                    icon = { Icon(icon, label) },
                                    label = { Text(label) },
                                    onClick = { navigationIndex = index },
                                    colors = NavigationBarItemDefaults.colors()
                                )
                            }
                        }
                    }
                ) { contentPadding ->
                    Box(Modifier.padding(contentPadding).fillMaxSize()) {
                        when (navigationIndex) {
                            0 -> GroupView(smallScreen, groupViewModel)
                            1 -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text("Events")
                            }

                            2 -> InvitationView(smallScreen, userViewModel)
                            3 -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text("Profile")
                            }
                        }
                    }
                }
            } else {
                Scaffold { contentPadding ->
                    PermanentNavigationDrawer(
                        modifier = Modifier.fillMaxSize().background(colors.primaryContainer),
                        drawerContent = {
                            PermanentDrawerSheet(Modifier.width(210.dp).fillMaxSize()) {
                                LazyColumn(Modifier.padding(vertical = 30.dp).fillMaxSize()) {
                                    itemsIndexed(navigationItems) { index, item ->
                                        val icon = item["icon"] as ImageVector
                                        var label = item["label"] as String
                                        when (index) {
                                            1 -> label += " (${userUiState.events.size})"
                                            2 -> label += " (${userUiState.invitations.size})"
                                        }
                                        NavigationDrawerItem(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            selected = index == navigationIndex,
                                            icon = { Icon(icon, label) },
                                            label = { Text(label) },
                                            onClick = { navigationIndex = index },
                                            shape = ShapeDefaults.Medium
                                        )
                                    }
                                }
                            }
                        },
                        content = {
                            Row(Modifier.fillMaxSize().padding(contentPadding)) {
                                VerticalDivider(color = colors.onPrimaryContainer)
                                when (navigationIndex) {
                                    0 -> GroupView(smallScreen, groupViewModel)
                                    1 -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                        Text("Events")
                                    }

                                    2 -> InvitationView(smallScreen, userViewModel)
                                    3 -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                        Text("Profile")
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

var colors = lightColorScheme()