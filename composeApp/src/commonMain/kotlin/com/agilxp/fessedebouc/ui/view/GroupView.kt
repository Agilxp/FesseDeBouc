package com.agilxp.fessedebouc.ui.view

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agilxp.fessedebouc.colors
import com.agilxp.fessedebouc.ui.component.ChatWindow
import com.agilxp.fessedebouc.ui.component.GroupAdmin
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel
import com.composables.core.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopGroupBar(groupViewModel: GroupViewModel, state: DialogState) {
    val groupUiState by groupViewModel.uiState.collectAsState()
    CenterAlignedTopAppBar(
        title = { Text(groupUiState.selectedGroup!!.name) },
        colors = TopAppBarDefaults.topAppBarColors(colors.primaryContainer),
        actions = {
            FloatingActionButton(
                onClick = { state.visible = true },
                containerColor = colors.secondaryContainer,
            ) {
                Icon(Lucide.Settings, "Group settings")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupView(smallScreen: Boolean, groupViewModel: GroupViewModel) {
    val groupUiState by groupViewModel.uiState.collectAsState()
    val state = rememberDialogState(initiallyVisible = false)
    if (smallScreen) {
        Scaffold(topBar = {
            TopGroupBar(groupViewModel, state)
        }, bottomBar = {
            NavigationBar {
                groupUiState.myGroups.forEachIndexed { index, map ->
                    val group = groupUiState.myGroups[index]
                    val label = group.name
                    NavigationBarItem(
                        selected = group == groupUiState.selectedGroup,
                        icon = { },
                        label = { Text(text = label, overflow = TextOverflow.Ellipsis, maxLines = 2) },
                        onClick = { groupViewModel.selectGroup(group) },
                        colors = NavigationBarItemDefaults.colors(),
                    )
                }
            }
        }
        ) { contentPadding ->
            if (groupUiState.selectedGroup != null) {
                Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                    ChatWindow(groupViewModel)
                }
            } else {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Select a group")
                }
            }
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
                                    label = { Text(label, overflow = TextOverflow.Ellipsis, maxLines = 1) },
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
                            Scaffold(
                                topBar = {
                                    TopGroupBar(groupViewModel, state)
                                },
                            ) {
                                ChatWindow(groupViewModel)
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text("Select a group")
                            }
                        }
                    }
                }
            )
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
            GroupAdmin(groupViewModel)
        }
    }
}