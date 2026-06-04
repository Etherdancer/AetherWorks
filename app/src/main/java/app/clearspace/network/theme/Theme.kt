package app.clearspace.network.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class AppTheme {
    DEFAULT,
    LIGHT,
    DARK
}

private val ClearSpaceDefaultScheme = darkColorScheme(
    primary = CsPrimary,
    onPrimary = CsOnPrimary,
    primaryContainer = CsPrimaryContainer,
    onPrimaryContainer = CsOnPrimaryContainer,
    secondary = CsSecondary,
    onSecondary = CsOnSecondary,
    secondaryContainer = CsSecondaryContainer,
    onSecondaryContainer = CsOnSecondaryContainer,
    background = CsBackground,
    onBackground = CsSecondary, // Silver text on black
    surface = CsSurface,
    onSurface = CsSecondary,
    error = ErrorColor,
    onError = OnErrorColor,
    errorContainer = ErrorContainerColor,
    onErrorContainer = OnErrorContainerColor
)

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightSecondary,
    surface = LightSurface,
    onSurface = LightSecondary,
    error = ErrorColor,
    onError = OnErrorColor,
    errorContainer = ErrorContainerColor,
    onErrorContainer = OnErrorContainerColor
)

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkSecondary,
    surface = DarkSurface,
    onSurface = DarkSecondary,
    error = ErrorColor,
    onError = OnErrorColor,
    errorContainer = ErrorContainerColor,
    onErrorContainer = OnErrorContainerColor
)

@Composable
fun ClearSpaceTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    content: @Composable () -> Unit,
) {
    val systemIsDark = isSystemInDarkTheme()
    
    val colorScheme = when (appTheme) {
        AppTheme.DEFAULT -> ClearSpaceDefaultScheme
        AppTheme.LIGHT -> LightScheme
        AppTheme.DARK -> DarkScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assume this exists in Typography.kt
        content = content
    )
}

