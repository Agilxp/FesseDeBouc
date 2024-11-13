package com.agilxp.fessedebouc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agilxp.fessedebouc.colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FesseDeBoucApp(
    navController: NavHostController = rememberNavController(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fesse De Bouc") },
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
                Row(
                    modifier = Modifier.fillMaxSize().height(1.dp), // Don't know why but height is needed
                ) {
                    Box(modifier = Modifier.fillMaxWidth(0.2f)) {
                        GroupMenu()
                    }
                    VerticalDivider(color = colors.primary, modifier = Modifier.fillMaxHeight().width(1.dp))
                    MainWindow()
                }
            }
        }
    }
}