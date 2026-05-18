package com.zephyruso.zashboard.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

@Composable
fun ZashboardTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = (context as? Activity)?.resources?.configuration?.uiMode?.and(
        android.content.res.Configuration.UI_MODE_NIGHT_MASK,
    ) == android.content.res.Configuration.UI_MODE_NIGHT_YES

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
