package example.imageviewer.style

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

object ImageviewerColors {
    val ToastBackground = Color(23, 23, 23)
    val background = Color(0xFFFFFFFF)
    val onBackground = Color(0xFF19191C)

    val fullScreenImageBackground = Color(0xFF19191C)
    val filterButtonsBackground = fullScreenImageBackground.copy(alpha = 0.7f)
    val uiLightBlack = Color(25, 25, 28).copy(alpha = 0.7f)
    val noteBlockBackground = Color(0xFFF3F3F4)
}

@Composable
fun ImageViewerTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme() // todo check and change colors
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            background = ImageviewerColors.background,
            onBackground = ImageviewerColors.onBackground
        )
    ) {
        ProvideTextStyle(LocalTextStyle.current.copy(letterSpacing = 0.sp)) {
            content()
        }
    }
}
