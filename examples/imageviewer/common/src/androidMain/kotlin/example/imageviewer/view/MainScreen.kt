package example.imageviewer.view

import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.common.R
import example.imageviewer.model.AppState
import example.imageviewer.model.ContentState
import example.imageviewer.model.Picture
import example.imageviewer.model.ScreenType
import example.imageviewer.style.DarkGray
import example.imageviewer.style.DarkGreen
import example.imageviewer.style.Foreground
import example.imageviewer.style.LightGray
import example.imageviewer.style.MiniatureColor
import example.imageviewer.style.Transparent
import example.imageviewer.style.icDots
import example.imageviewer.style.icEmpty
import example.imageviewer.style.icRefresh

@Composable
fun MainScreen(content: ContentState) {
    Column {
        TopContent(content)
        ScrollableArea(content)
    }
    if (!content.isContentReady()) {
        LoadingScreen(content.getString(R.string.loading))
    }
}

@Composable
fun TopContent(content: ContentState) {
    TitleBar(text = content.getString(R.string.app_name), content = content)
    if (content.getOrientation() == Configuration.ORIENTATION_PORTRAIT) {
        PreviewImage(content)
        Spacer(modifier = Modifier.height(10.dp))
        Divider()
    }
    Spacer(modifier = Modifier.height(5.dp))
}

@Composable
fun TitleBar(text: String, content: ContentState) {
    TopAppBar(
        backgroundColor = DarkGreen,
        title = {
            Row(Modifier.height(50.dp)) {
                Text(
                    text,
                    color = Foreground,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                )
                Surface(
                    color = Transparent,
                    modifier = Modifier.padding(end = 20.dp).align(Alignment.CenterVertically),
                    shape = CircleShape
                ) {
                    Clickable(
                        onClick = {
                            if (content.isContentReady()) {
                                content.refresh()
                            }
                        }
                    ) {
                        Image(
                            icRefresh(),
                            contentDescription = null,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                }
            }
        })
}

@Composable
fun PreviewImage(content: ContentState) {
    Clickable(onClick = {
        AppState.screenState(ScreenType.FullscreenImage)
    }) {
        Card(
            backgroundColor = DarkGray,
            modifier = Modifier.height(250.dp),
            shape = RectangleShape,
            elevation = 1.dp
        ) {
            Image(
                if (content.isMainImageEmpty()) {
                    icEmpty()
                } else {
                    BitmapPainter(content.getSelectedImage().asImageBitmap())
                },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth().padding(start = 1.dp, top = 1.dp, end = 1.dp, bottom = 5.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun Miniature(
    picture: Picture,
    content: ContentState
) {
    Card(
        backgroundColor = MiniatureColor,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp).height(70.dp)
            .fillMaxWidth()
            .clickable {
                content.setMainImage(picture)
            },
        shape = RectangleShape,
        elevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(end = 30.dp)) {
            Clickable(
                onClick = {
                    content.fullscreen(picture)
                }
            ) {
                Image(
                    picture.image.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.height(70.dp)
                        .width(90.dp)
                        .padding(start = 1.dp, top = 1.dp, end = 1.dp, bottom = 1.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = picture.name,
                color = Foreground,
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically).padding(start = 16.dp),
                style = MaterialTheme.typography.body1
            )

            Clickable(
                modifier = Modifier.height(70.dp)
                    .width(30.dp),
                onClick = {
                    showPopUpMessage(
                        "${content.getString(R.string.picture)} " +
                                "${picture.name} \n" +
                                "${content.getString(R.string.size)} " +
                                "${picture.width}x${picture.height} " +
                                "${content.getString(R.string.pixels)}",
                        content.getContext()
                    )
                }
            ) {
                Image(
                    icDots(),
                    contentDescription = null,
                    modifier = Modifier.height(70.dp)
                        .width(30.dp)
                        .padding(start = 1.dp, top = 25.dp, end = 1.dp, bottom = 25.dp),
                    contentScale = ContentScale.FillHeight
                )
            }
        }
    }
}

@Composable
fun ScrollableArea(content: ContentState) {
    var index = 1
    val scrollState = rememberScrollState()
    Column(Modifier.verticalScroll(scrollState)) {
        for (picture in content.getMiniatures()) {
            Miniature(
                picture = picture,
                content = content
            )
            Spacer(modifier = Modifier.height(5.dp))
            index++
        }
    }
}

@Composable
fun Divider() {
    Divider(
        color = LightGray,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp)
    )
}
