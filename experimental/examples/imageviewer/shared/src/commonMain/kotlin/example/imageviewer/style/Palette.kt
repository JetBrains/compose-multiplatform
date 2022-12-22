package example.imageviewer.style

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
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

    fun buttonBackground(isHover: Boolean) = if (isHover) TranslucentBlack else Transparent
}

@Composable
internal fun ImageViewerTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme() // todo check and change colors
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = ImageviewerColors.Foreground,
            secondary = ImageviewerColors.LightGray,
            background = ImageviewerColors.DarkGray,
            surface = ImageviewerColors.Gray,
            onPrimary = ImageviewerColors.Foreground,
            onSecondary = Color.Black,
            onBackground = ImageviewerColors.Foreground,
            onSurface = ImageviewerColors.Foreground
        )
    ) {
        content()
    }
}
