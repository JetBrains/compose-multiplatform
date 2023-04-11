package example.imageviewer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import example.imageviewer.LocalLocalization
import example.imageviewer.icon.IconCustomArrowBack
import example.imageviewer.style.ImageviewerColors

@Composable
fun CircularButton(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(ImageviewerColors.uiLightBlack)
            .run {
                if (enabled) {
                    clickable { onClick() }
                } else this
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun CircularButton(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    CircularButton(
        modifier = modifier,
        content = {
            Icon(imageVector, null, Modifier.size(34.dp), Color.White)
        },
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun BackButton(onClick: () -> Unit) {
    Tooltip(LocalLocalization.current.back) {
        CircularButton(
            imageVector = IconCustomArrowBack,
            onClick = onClick
        )
    }
}
