package example.imageviewer.style

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

data class ImageviewerColors(
    val toastBackground: Color,
    val background: Color,
    val onBackground: Color,
    val fullScreenImageBackground: Color,
    val filterButtonsBackground: Color,
    val uiLightBlack: Color,
    val noteBlockBackground: Color,
)

private val LightImageviewerColors = ImageviewerColors(
    toastBackground = Color(23, 23, 23),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF19191C),
    fullScreenImageBackground = Color(0xFF19191C),
    filterButtonsBackground = Color(0xFF19191C).copy(alpha = 0.7f),
    uiLightBlack = Color(25, 25, 28).copy(alpha = 0.7f),
    noteBlockBackground = Color(0xFFF3F3F4),
)

private val DarkImageviewerColors = ImageviewerColors(
    toastBackground = Color(23, 23, 23),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECECEE),
    fullScreenImageBackground = Color(0xFF000000),
    filterButtonsBackground = Color(0xFF000000).copy(alpha = 0.7f),
    uiLightBlack = Color(45, 45, 48).copy(alpha = 0.7f),
    noteBlockBackground = Color(0xFF2B2B2E),
)

val LocalImageviewerColors = compositionLocalOf { LightImageviewerColors }

@Composable
fun ImageViewerTheme(content: @Composable () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val colors = if (isDarkTheme) DarkImageviewerColors else LightImageviewerColors
    val materialColors = (if (isDarkTheme) darkColors() else lightColors()).copy(
        background = colors.background,
        onBackground = colors.onBackground,
    )
    CompositionLocalProvider(LocalImageviewerColors provides colors) {
        MaterialTheme(colors = materialColors) {
            ProvideTextStyle(LocalTextStyle.current.copy(letterSpacing = 0.sp)) {
                content()
            }
        }
    }
}
