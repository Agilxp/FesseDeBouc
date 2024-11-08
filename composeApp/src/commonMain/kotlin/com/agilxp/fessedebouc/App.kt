package com.agilxp.fessedebouc

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import com.agilxp.fessedebouc.ui.FesseDeBoucApp

@Composable
fun App() {
    colors = if (isSystemInDarkTheme()) darkColors() else lightColors()
    MaterialTheme(colors = colors) {
        BoxWithConstraints {
            if (maxWidth.value < 1000) {
                Text("Coming Soon")
            } else {
                FesseDeBoucApp()
            }
        }
    }
}

var colors = lightColors()