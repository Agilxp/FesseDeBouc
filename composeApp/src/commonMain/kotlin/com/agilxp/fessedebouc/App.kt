package com.agilxp.fessedebouc

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.agilxp.fessedebouc.ui.FesseDeBoucApp

@Composable
fun App() {
    colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors) {
        BoxWithConstraints {
            if (maxWidth.value < 1000) {
                Text("Coming Soon")
            } else {
                FesseDeBoucApp()
            }
        }
    }
}

var colors = lightColorScheme()