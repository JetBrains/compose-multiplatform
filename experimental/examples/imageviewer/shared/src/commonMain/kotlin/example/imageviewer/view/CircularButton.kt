package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import example.imageviewer.style.ImageviewerColors

@Composable
internal fun CircularButton(image: Painter, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(ImageviewerColors.uiLightBlack)
            .clickable { onClick() }, contentAlignment = Alignment.Center
    ) {
        Image(
            image,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}
