package com.agilxp.fessedebouc.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.agilxp.fessedebouc.colors
import com.agilxp.fessedebouc.ui.viewmodel.UserViewModel
import com.composables.icons.lucide.CalendarCheck2
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MailCheck

val navigationItems = listOf(
    mapOf("icon" to Lucide.MailCheck, "label" to "Invitations"),
    mapOf("icon" to Lucide.CalendarCheck2, "label" to "Events"),
)

@Composable
fun ActivityView(smallScreen: Boolean, userViewModel: UserViewModel) {
    val userUiState by userViewModel.uiState.collectAsState()
    var navigationIndex by remember { mutableStateOf(0) }
    if (smallScreen) {
        Scaffold(
            bottomBar = {
                NavigationBar(Modifier.fillMaxSize()) {
                    NavigationBar(Modifier.fillMaxSize()) {
                        navigationItems.forEachIndexed { index, map ->
                            val item = navigationItems.get(index)
                            val icon = item["icon"] as ImageVector
                            var label = item["label"] as String
                            when(index) {
                                0 -> label += " (${userUiState.invitations.size})"
                                1 -> label += " (${userUiState.events.size})"
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
                            itemsIndexed(navigationItems) { index, item ->
                                val icon = item["icon"] as ImageVector
                                var label = item["label"] as String
                                when(index) {
                                    0 -> label += " (${userUiState.invitations.size})"
                                    1 -> label += " (${userUiState.events.size})"
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
                            0 -> Text("Invitations")
                            1 -> Text("Events")
                        }
                    }
                }
            )
        }
    }
}