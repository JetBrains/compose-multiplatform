package example.imageviewer.view

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.ResString
import example.imageviewer.model.AppState
import example.imageviewer.model.ContentState
import example.imageviewer.model.Picture
import example.imageviewer.model.ScreenType
import example.imageviewer.style.DarkGray
import example.imageviewer.style.DarkGreen
import example.imageviewer.style.Foreground
import example.imageviewer.style.LightGray
import example.imageviewer.style.MiniatureColor
import example.imageviewer.style.MiniatureHoverColor
import example.imageviewer.style.TranslucentBlack
import example.imageviewer.style.TranslucentWhite
import example.imageviewer.style.Transparent
import example.imageviewer.style.icDots
import example.imageviewer.style.icEmpty
import example.imageviewer.style.icRefresh
import example.imageviewer.utils.toByteArray

@Composable
fun MainScreen(content: ContentState) {
    Column {
        TopContent(content)
        ScrollableArea(content)
    }
    if (!content.isContentReady()) {
        LoadingScreen(ResString.loading)
    }
}

@Composable
fun TopContent(content: ContentState) {
    TitleBar(text = ResString.appName, content = content)
    PreviewImage(content)
    Spacer(modifier = Modifier.height(10.dp))
    Divider()
    Spacer(modifier = Modifier.height(5.dp))
}

@Composable
fun TitleBar(text: String, content: ContentState) {
    val interactionSource = remember { MutableInteractionSource() }
    val refreshButtonHover by interactionSource.collectIsHoveredAsState()
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
                Tooltip(ResString.refresh) {
                    Clickable(
                        modifier = Modifier
                            .hoverable(interactionSource)
                            .background(color = if (refreshButtonHover) TranslucentBlack else Transparent),
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
        }
    })
}

@Composable
fun PreviewImage(content: ContentState) {
    Clickable(
        modifier = Modifier.background(color = DarkGray),
        onClick = {
            AppState.screenState(ScreenType.FullscreenImage)
        }
    ) {
        Card(
            backgroundColor = Transparent,
            modifier = Modifier.height(250.dp),
            shape = RectangleShape,
            elevation = 1.dp
        ) {
            Image(
                if (content.isMainImageEmpty())
                    icEmpty()
                else
                    BitmapPainter(content.getSelectedImage()),
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
    val cardHoverInteractionSource = remember { MutableInteractionSource() }
    val cardHover by cardHoverInteractionSource.collectIsHoveredAsState()
    val infoButtonInteractionSource = remember { MutableInteractionSource() }
    val infoButtonHover by infoButtonInteractionSource.collectIsHoveredAsState()
    Card(
        backgroundColor = if (cardHover) MiniatureHoverColor else MiniatureColor,
        modifier = Modifier.padding(start = 10.dp, end = 18.dp).height(70.dp)
            .fillMaxWidth()
            .hoverable(cardHoverInteractionSource)
            .clickable {
                content.setMainImage(picture)
            },
        shape = RectangleShape
    ) {
        Row(modifier = Modifier.padding(end = 30.dp)) {
            Clickable(
                onClick = {
                    content.fullscreen(picture)
                }
            ) {
                Image(
                    org.jetbrains.skia.Image.makeFromEncoded(
                        toByteArray(picture.image)
                    ).toComposeImageBitmap(),
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
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.body1
            )

            Clickable(
                modifier = Modifier.height(70.dp)
                    .width(30.dp)
                    .hoverable(infoButtonInteractionSource)
                    .background(color = if (infoButtonHover) TranslucentWhite else Transparent),
                onClick = {
                    showPopUpMessage(
                        "${ResString.picture} " +
                        "${picture.name} \n" +
                        "${ResString.size} " +
                        "${picture.width}x${picture.height} " +
                        "${ResString.pixels}"
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
    Box(
        modifier = Modifier.fillMaxSize()
        .padding(end = 8.dp)
    ) {
        val stateVertical = rememberScrollState(0)
        Column(modifier = Modifier.verticalScroll(stateVertical)) {
            var index = 1
            Column {
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
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(stateVertical),
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}

@Composable
fun Divider() {
    Divider(
        color = LightGray,
        modifier = Modifier.padding(start = 10.dp, end = 10.dp)
    )
}
