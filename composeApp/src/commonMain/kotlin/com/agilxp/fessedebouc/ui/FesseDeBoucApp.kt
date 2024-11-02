package com.agilxp.fessedebouc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agilxp.fessedebouc.AuthViewModel

@Composable
fun FesseDeBoucApp(
    navController: NavHostController = rememberNavController(),
    groupViewModel: GroupViewModel = viewModel { GroupViewModel() },
    appViewModel: AuthViewModel = viewModel { AuthViewModel() },
) {
    val groupUiState by groupViewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fess De Bouc") },
                modifier = Modifier,
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState()),
            startDestination = "Home"
        ) {
            composable(route = "Home") {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { navController.navigate("Profile") }) {
                        Text("Profile")
                    }
                    Text(appViewModel.uiState.value.accessToken ?: "Not logged in")
                    Text("Groups")
                    groupUiState.myGroups.map { group ->
                        Text(group.name)
                    }

                }
            }
            composable(route = "Profile") {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { navController.navigate("Home") }) {
                        Text("Home")
                    }
                }
            }
        }
    }
}