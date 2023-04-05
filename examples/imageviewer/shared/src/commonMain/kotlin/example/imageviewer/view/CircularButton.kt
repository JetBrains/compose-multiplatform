package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import example.imageviewer.LocalLocalization
import example.imageviewer.style.ImageviewerColors
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun CircularButton(
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
internal fun CircularButton(
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
internal fun CircularButton(
    image: Painter,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    CircularButton(
        content = {
            Image(
                image,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    )
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun BackButton(onClick: () -> Unit) {
    Tooltip(LocalLocalization.current.back) {
        CircularButton(
            painterResource("arrowleft.png"),
            onClick = onClick
        )
    }
}
