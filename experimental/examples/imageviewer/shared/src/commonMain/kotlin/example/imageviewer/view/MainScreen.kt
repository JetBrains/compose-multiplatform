package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.model.*
import example.imageviewer.style.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.orEmpty
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource

@Composable
fun MainScreen(content: ContentState) {
    Column {
        TopContent(content)
        ScrollableArea(content)
    }
    if (!content.isContentReady()) {
        LoadingScreen(content.localization.loading)
    }
}

@Composable
fun TopContent(content: ContentState) {
    TitleBar(text = content.localization.appName, content = content)
//    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {//todo
        PreviewImage(content)
        Spacer(modifier = Modifier.height(10.dp))
        Divider()
//    }
    Spacer(modifier = Modifier.height(5.dp))
}

@OptIn(ExperimentalResourceApi::class)
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
                            bitmap = resource("refresh.png").rememberImageBitmap().orEmpty(),
                            contentDescription = null,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                }
            }
        })
}

@OptIn(ExperimentalResourceApi::class)
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
                bitmap = if (content.isMainImageEmpty()) {
                    resource("empty.png").rememberImageBitmap().orEmpty()
                } else {
                    content.getSelectedImage()
                },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth().padding(start = 1.dp, top = 1.dp, end = 1.dp, bottom = 5.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
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
                    picture.image,
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
                    content.notification.notifyImageData(picture)
                }
            ) {
                Image(
                    resource("dots.png").rememberImageBitmap().orEmpty(),
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
