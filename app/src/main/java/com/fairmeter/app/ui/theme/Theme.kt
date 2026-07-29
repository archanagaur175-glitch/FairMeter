package com.fairmeter.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = NearBlack,
    primaryContainer = AmberDark,
    secondary = TealSecondary,
    onSecondary = WarmWhite,
    secondaryContainer = TealDark,
    background = SurfaceDark,
    onBackground = WarmWhite,
    surface = Charcoal,
    onSurface = WarmWhite,
    surfaceVariant = Charcoal,
    error = ErrorRed,
    onError = WarmWhite
)

private val LightColorScheme = lightColorScheme(
    primary = AmberDark,
    onPrimary = WarmWhite,
    primaryContainer = AmberLight,
    secondary = TealSecondary,
    onSecondary = WarmWhite,
    secondaryContainer = TealLight,
    background = WarmWhite,
    onBackground = NearBlack,
    surface = WarmWhite,
    onSurface = NearBlack,
    surfaceVariant = AmberLight,
    error = ErrorRed,
    onError = WarmWhite
)

@Composable
fun FairMeterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
