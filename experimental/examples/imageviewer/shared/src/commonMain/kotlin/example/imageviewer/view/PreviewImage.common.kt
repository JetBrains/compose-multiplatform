package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.model.*
import example.imageviewer.model.State
import example.imageviewer.style.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.orEmpty
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun PreviewImage(state: MutableState<State>, getImage: suspend (Picture) -> ImageBitmap) {
    val pictures = state.value.pictures
    val index = state.value.currentImageIndex
    val imageState = remember(pictures, index) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(pictures, index) {
        val picture = pictures.getOrNull(index)
        if (picture != null) {
            imageState.value = getImage(picture)
        }
    }

    val image = imageState.value
    Spacer(
        modifier = Modifier.height(5.dp).fillMaxWidth()
            .background(brush = kotlinHorizontalGradientBrush)
    )
    Card(
//        colors = CardDefaults.cardColors()
//        backgroundColor = MaterialTheme.colors.background,
        modifier = Modifier.height(200.dp)
            .background(brush = kotlinHorizontalGradientBrush)
            .padding(10.dp)
            .clickable { state.toFullscreen() },
        shape = RoundedCornerShape(10.dp, 10.dp, 10.dp, 10.dp),
//        elevation = 1.dp
    ) {
        Image(
            bitmap = image ?: resource("empty.png").rememberImageBitmap().orEmpty(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()//.padding(start = 1.dp, top = 1.dp, end = 1.dp, bottom = 5.dp),
            ,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
internal expect fun needShowPreview(): Boolean
