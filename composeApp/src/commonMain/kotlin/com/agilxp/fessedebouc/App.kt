package com.agilxp.fessedebouc

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel
import com.composables.icons.lucide.*

val navigationItems = listOf(
    mapOf("icon" to Lucide.Group, "label" to "Groups"),
    mapOf("icon" to Lucide.MailCheck, "label" to "Invitations"),
    mapOf("icon" to Lucide.CalendarCheck2, "label" to "Events"),
    mapOf("icon" to Lucide.UserCog, "label" to "Settings"),
)

@Composable
fun App(
    groupViewModel: GroupViewModel = viewModel { GroupViewModel() }
) {
    colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    var navigationIndex by remember { mutableStateOf(0) }
    MaterialTheme(colorScheme = colors) {
        BoxWithConstraints {
            val smallScreen = maxWidth.value < 1000
            if (smallScreen) {
                Scaffold(
                    bottomBar = {
                        NavigationBar(Modifier.fillMaxSize()) {
                            navigationItems.forEachIndexed { index, map ->
                                val item = navigationItems.get(index)
                                val icon = item.get("icon") as ImageVector
                                val label = item.get("label") as String
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
                    Text("small : $navigationIndex", Modifier.padding(contentPadding).fillMaxSize())
                }
            } else {
                Scaffold { contentPadding ->
                    PermanentNavigationDrawer(modifier = Modifier.fillMaxSize().background(colors.primaryContainer),drawerContent = {
                        PermanentDrawerSheet(Modifier.width(200.dp).fillMaxSize()) {
                            LazyColumn(Modifier.padding(vertical = 100.dp).fillMaxSize()) {
                                itemsIndexed(navigationItems) { index, map ->
                                    val item = navigationItems.get(index)
                                    val icon = item.get("icon") as ImageVector
                                    val label = item.get("label") as String
                                    NavigationDrawerItem(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        selected = index == navigationIndex,
                                        icon = { Icon(icon, label) },
                                        label = { Text(label) },
                                        onClick = { navigationIndex = index })
                                }
                            }
                        }
                    }, content = {
                        when (navigationIndex) {
                            0 -> Text("Groups")
                            1 -> Text("Invitations")
                            2 -> Text("Events")
                            3 -> Text("Settings")
                        }
                    })
                }
            }
        }
    }
}

var colors = lightColorScheme()