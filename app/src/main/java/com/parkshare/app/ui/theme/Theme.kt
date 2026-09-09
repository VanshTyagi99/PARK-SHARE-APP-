package com.parkshare.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ParkShareColorScheme = lightColorScheme(

    primary = PrimaryBlue,
    secondary = SecondaryGreen,

    background = Background,
    surface = Surface,

    error = Error,

    onPrimary = Surface,
    onSecondary = Surface,

    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ParkShareTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ParkShareColorScheme,
        typography = AppTypography,
        content = content
    )
}