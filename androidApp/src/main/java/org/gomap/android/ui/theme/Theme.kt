package org.gomap.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = MossGreen,
    secondary = RiverTeal,
    tertiary = Ember,
    background = WarmStone,
    surface = WarmStone
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF59C984),
    secondary = Color(0xFF68D4E0),
    tertiary = Color(0xFFF3A36E),
    background = DeepForest,
    surface = Color(0xFF19352D)
)

@Composable
fun GoMapAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
