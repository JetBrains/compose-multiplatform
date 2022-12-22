package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.model.*
import example.imageviewer.style.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.orEmpty
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun Miniature(
    picture: Picture,
    image: ImageBitmap?,
    onClickSelect: () -> Unit,
    onClickFullScreen: () -> Unit,
    onClickInfo: () -> Unit,
) {
    Card(
        backgroundColor = ImageviewerColors.MiniatureColor,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp).height(70.dp)
            .fillMaxWidth()
            .clickable {
                onClickSelect()
            },
        shape = RectangleShape,
        elevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(end = 30.dp)) {
            val modifier = Modifier.height(70.dp)
                .width(90.dp)
                .padding(start = 1.dp, top = 1.dp, end = 1.dp, bottom = 1.dp)
            if (image != null) {
                Image(
                    image,
                    contentDescription = null,
                    modifier = modifier.clickable { onClickFullScreen() },
                    contentScale = ContentScale.Crop
                )
            } else {
                CircularProgressIndicator(modifier)
            }
            Text(
                text = picture.name,
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically).padding(start = 16.dp),
                style = MaterialTheme.typography.body1
            )

            Image(
                resource("dots.png").rememberImageBitmap().orEmpty(),
                contentDescription = null,
                modifier = Modifier.height(70.dp)
                    .width(30.dp)
                    .padding(start = 1.dp, top = 25.dp, end = 1.dp, bottom = 25.dp)
                    .clickable { onClickInfo() },
                contentScale = ContentScale.FillHeight
            )
        }
    }
}
