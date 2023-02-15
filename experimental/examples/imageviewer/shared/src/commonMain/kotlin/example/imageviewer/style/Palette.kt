package example.imageviewer.style

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ImageviewerColors {
    val Gray = Color.DarkGray
    val LightGray = Color(100, 100, 100)
    val DarkGray = Color(32, 32, 32)
    val PreviewImageAreaHoverColor = Color(45, 45, 45)
    val ToastBackground = Color(23, 23, 23)
    val MiniatureColor = Color(50, 50, 50)
    val MiniatureHoverColor = Color(55, 55, 55)
    val Foreground = Color(210, 210, 210)
    val TranslucentBlack = Color(0, 0, 0, 60)
    val TranslucentWhite = Color(255, 255, 255, 20)
    val Transparent = Color.Transparent

    val KotlinGradient0 = Color(0xFF7F52FF)
    val KotlinGradient50 = Color(0xFFC811E2)
    val KotlinGradient100 = Color(0xFFE54857)

    val kotlinHorizontalGradientBrush = Brush.horizontalGradient(
        colors = listOf(
            KotlinGradient0,
            KotlinGradient50,
            KotlinGradient100
        )
    )

    fun buttonBackground(isHover: Boolean) = if (isHover) TranslucentBlack else Transparent
}

@Composable
internal fun ImageViewerTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme() // todo check and change colors
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color(0xFF1B1B1B),
            onBackground = Color(0xFFFFFFFF)
        )
    ) {
        content()
    }
}
