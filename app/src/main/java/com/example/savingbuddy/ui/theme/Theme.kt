package com.example.savingbuddy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BlueNeutral,
    onPrimary = Color.White,
    primaryContainer = BlueNeutral.copy(alpha = 0.1f),
    secondary = GreenIncome,
    onSecondary = Color.White,
    tertiary = Orange,
    background = Background,
    surface = Surface,
    onBackground = OnSurface,
    onSurface = OnSurface,
    error = RedExpense,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueNeutral,
    onPrimary = Color.White,
    primaryContainer = BlueNeutral.copy(alpha = 0.2f),
    secondary = GreenIncome,
    onSecondary = Color.White,
    tertiary = Orange,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White,
    error = RedExpense,
    onError = Color.White
)

@Composable
fun SavingBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}