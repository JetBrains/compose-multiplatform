package example.imageviewer.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.model.PictureData
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun Thumbnail(
    picture: PictureData,
    onClickSelect: () -> Unit,
    onClickFullScreen: () -> Unit,
    onClickInfo: () -> Unit
) {
    Card(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp).height(70.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(200.dp),
        border = BorderStroke(1.dp, Color.White),
        backgroundColor = MaterialTheme.colors.background,
        contentColor = MaterialTheme.colors.onBackground
    ) {
        Box(Modifier.clickable { onClickSelect() }) {
            Row(modifier = Modifier.padding(end = 30.dp)) {
                Tooltip(picture.name) {
                    ThumbnailImage(
                        modifier = Modifier.size(70.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(1.dp, Color.White), CircleShape)
                            .clickable { onClickFullScreen() },
                        picture = picture,
                    )
                }
                Text(
                    text = picture.name,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                        .padding(start = 16.dp),
                    style = MaterialTheme.typography.subtitle1
                )

                Image(
                    painterResource("dots.png"),
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
}
