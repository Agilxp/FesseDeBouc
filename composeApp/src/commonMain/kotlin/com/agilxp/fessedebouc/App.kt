package com.agilxp.fessedebouc

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agilxp.fessedebouc.ui.FesseDeBoucApp
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel

@Composable
fun App(
    groupViewModel: GroupViewModel = viewModel { GroupViewModel() }
) {
    colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors) {
        BoxWithConstraints {
            if (maxWidth.value < 1000) {
                Text("Coming Soon")
            } else {
                FesseDeBoucApp(groupViewModel)
            }
        }
    }
}

var colors = lightColorScheme()