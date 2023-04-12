package example.imageviewer.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.model.Picture
import example.imageviewer.model.name
import example.imageviewer.painterResourceCached
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun Miniature(
    picture: Picture,
    image: ImageBitmap?,
    onClickSelect: () -> Unit,
    onClickFullScreen: () -> Unit,
    onClickInfo: () -> Unit,
) {
    Card(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp).height(70.dp)
            .fillMaxWidth(),
        onClick = { onClickSelect() },
        shape = RoundedCornerShape(200.dp),
        border = BorderStroke(1.dp, Color.White),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        )

    ) {
        Row(modifier = Modifier.padding(end = 30.dp)) {
            val modifier = Modifier.height(70.dp)
                .width(70.dp)
            if (image != null) {
                Image(
                    image,
                    contentDescription = null,
                    modifier = modifier
                        .clip(CircleShape)
                        .border(BorderStroke(1.dp, Color.White), CircleShape)
                        .clickable { onClickFullScreen() },
                    contentScale = ContentScale.Crop
                )
            } else {
                CircularProgressIndicator(modifier)
            }
            Text(
                text = picture.name,
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.titleLarge
            )

            Image(
                painterResourceCached("dots.png"),
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
