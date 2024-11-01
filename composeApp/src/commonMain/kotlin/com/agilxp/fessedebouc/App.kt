package com.agilxp.fessedebouc

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import com.agilxp.fessedebouc.ui.FesseDeBoucApp

@Composable
fun App() {
    val colors = if (isSystemInDarkTheme()) darkColors() else lightColors()
    MaterialTheme(colors = colors) {
        FesseDeBoucApp()
    }
}