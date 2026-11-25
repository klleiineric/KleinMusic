package app.kleinmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KleinScheme = darkColorScheme(
    primary = KleinColors.Orange,
    background = KleinColors.Background,
    surface = KleinColors.Surface,
    surfaceVariant = KleinColors.Elevated,
    onBackground = KleinColors.Text,
    onSurface = KleinColors.Text,
    onSurfaceVariant = KleinColors.Muted
)

@Composable
fun KleinTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KleinScheme, content = content)
}
