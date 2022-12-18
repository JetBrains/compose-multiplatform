package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
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

@Composable
fun MainScreen(content: ContentState) {
    Column {
        TopContent(content)
        Divider(color = LightGray, modifier = Modifier.padding(start = 10.dp, end = 10.dp))
        val scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
            for (picture in content.state.value.pictures) {
                Miniature(
                    picture = picture,
                    image = content.state.value.miniatures[picture],
                    onClickSelect = {
                        content.setMainImage(picture)
                    },
                    onClickFullScreen = {
                        content.fullscreen(picture)
                    },
                    onClickInfo = {
                        content.dependencies.notification.notifyImageData(picture)
                    },
                )
                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
    if (!content.isContentReady()) {
        LoadingScreen(content.dependencies.localization.loading)
    }
}

@Composable
fun TopContent(content: ContentState) {
    TitleBar(text = content.dependencies.localization.appName, content = content)
//    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {//todo
    PreviewImage(content)
    Spacer(modifier = Modifier.height(10.dp))
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
        content.state.value = content.state.value.copy(
            screen = ScreenState.FullScreen
        )
    }) {
        Card(
            backgroundColor = DarkGray,
            modifier = Modifier.height(250.dp),
            shape = RectangleShape,
            elevation = 1.dp
        ) {
            Image(
                bitmap = content.state.value.mainImage ?: resource("empty.png").rememberImageBitmap().orEmpty(),
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
    image: ImageBitmap?,
    onClickSelect: () -> Unit,
    onClickFullScreen: () -> Unit,
    onClickInfo: () -> Unit,
) {
    Card(
        backgroundColor = MiniatureColor,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp).height(70.dp)
            .fillMaxWidth()
            .clickable {
                onClickSelect()
            },
        shape = RectangleShape,
        elevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(end = 30.dp)) {
            Clickable(
                onClick = {
                    onClickFullScreen()
                }
            ) {
                val modifier = Modifier.height(70.dp)
                    .width(90.dp)
                    .padding(start = 1.dp, top = 1.dp, end = 1.dp, bottom = 1.dp)
                if (image != null) {
                    Image(
                        image,
                        contentDescription = null,
                        modifier = modifier,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = modifier,
                        color = DarkGreen
                    )
                }
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
                    onClickInfo()
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

