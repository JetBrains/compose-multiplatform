package example.imageviewer.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        // todo: something weird going on with the left-side corners here.
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
